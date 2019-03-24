package com.cbp.app.scheduled;

import com.cbp.app.helper.LoggingHelper;
import com.cbp.app.helper.TimeLimitedRepeater;
import com.cbp.app.model.db.Page;
import com.cbp.app.model.db.Website;
import com.cbp.app.repository.PageRepository;
import com.cbp.app.repository.WebsiteRepository;
import com.cbp.app.service.IndexService;
import com.cbp.app.service.ScraperService;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WebsiteScraperScheduler {

    private final WebsiteRepository websiteRepository;
    private final PageRepository pageRepository;
    private final ScraperService scraperService;
    private final IndexService indexService;
    private final boolean fetchWebsitesJobEnabled;
    private final boolean processWebsitesJobEnabled;
    private final boolean fixDuplicateWebsitesJobEnabled;
    private final boolean establishSubdomainRelationshipsJobEnabled;

    private static final int PAGE_LIST_NUMBER_THRESHOLD = 100;

    @Autowired
    public WebsiteScraperScheduler(
        WebsiteRepository websiteRepository,
        PageRepository pageRepository,
        ScraperService scraperService,
        IndexService indexService,
        @Value("${fetch-websites-scheduler.enabled}") boolean fetchWebsitesJobEnabled,
        @Value("${process-websites-scheduler.enabled}") boolean processWebsitesJobEnabled,
        @Value("${fix-duplicate-websites-scheduler.enabled}") boolean fixDuplicateWebsitesJobEnabled,
        @Value("${establish-subdomain-relationships.enabled}") boolean establishSubdomainRelationshipsJobEnabled
    ) {
        this.websiteRepository = websiteRepository;
        this.pageRepository = pageRepository;
        this.scraperService = scraperService;
        this.indexService = indexService;
        this.fetchWebsitesJobEnabled = fetchWebsitesJobEnabled;
        this.processWebsitesJobEnabled = processWebsitesJobEnabled;
        this.fixDuplicateWebsitesJobEnabled = fixDuplicateWebsitesJobEnabled;
        this.establishSubdomainRelationshipsJobEnabled = establishSubdomainRelationshipsJobEnabled;
    }

    @Scheduled(fixedRate = 4 * 60 * 60 * 1000)
    @SchedulerLock(name = "fetchWebsitesContent")
    public void fetchWebsitesContent() throws IOException, ExecutionException, InterruptedException {
        if (fetchWebsitesJobEnabled) {
            fetchWebsites();
            fetchPages();
            processWebsites();
            indexService.indexAndCompareWebsites();
        }
    }

    private void fetchWebsites() {
        LocalTime startTime = LoggingHelper.logStartOfMethod("fetch websites (parallel)");

        List<Website> nextUncheckedWebsites = websiteRepository.getNextDomesticWebsitesThatNeedFetching();

        Map<Website, Document> successfulWebsites = nextUncheckedWebsites.parallelStream()
            .collect(Collectors.toMap(
                Function.identity(),
                scraperService::getWebPageIfUrlReachable
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue().isPresent())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        successfulWebsites.entrySet().parallelStream()
            .forEach(entry -> scraperService.storeWebsiteContent(entry.getKey(), entry.getValue()));

        LoggingHelper.logEndOfMethod(
            "fetch websites (got " + successfulWebsites.size() + " out of " + nextUncheckedWebsites.size() + " websites in parallel)",
            startTime
        );
    }

    private void fetchPages() throws IOException {
        LocalTime startTimePageProcessing = LoggingHelper.logStartOfMethod("fetch pages (processing)");

        List<Page> nextUncheckedPages = pageRepository.getNextDomesticPagesThatNeedFetching();

        Map<Page, String> baseUrlToPages = nextUncheckedPages.stream()
            .collect(Collectors.toMap(Function.identity(), page -> page.getUrl().split("/")[0]));

        List<String> distinctBaseUrls = baseUrlToPages.values().stream().distinct().collect(Collectors.toList());

        List<List<Page>> listsOfListsOfPages = new ArrayList<>();

        while (baseUrlToPages.size() > 0) {
            List<Page> pages = distinctBaseUrls.stream().map(baseUrl -> {
                Optional<Map.Entry<Page, String>> entryOptional = baseUrlToPages.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(baseUrl)).findFirst();
                entryOptional.ifPresent(stringPageEntry -> baseUrlToPages.remove(stringPageEntry.getKey()));
                return entryOptional;
            })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            if (pages.size() >= PAGE_LIST_NUMBER_THRESHOLD) {
                listsOfListsOfPages.add(pages);
            }
        }

        LoggingHelper.logEndOfMethod("fetch pages (done processing)", startTimePageProcessing);

        LocalTime startTimePages = LoggingHelper.logStartOfMethod("fetch pages (parallel)");
        Random random = new Random();

        AtomicInteger totalPages = new AtomicInteger(0);
        AtomicInteger totalSuccessfulPages = new AtomicInteger(0);
        listsOfListsOfPages.parallelStream().forEach(listOfPages -> {

            LocalTime startTimePageIteration = LoggingHelper.logStartOfMethod("fetch pages (iteration)");

            try {
                long sleepTime = random.nextInt(100) * 100;
                LoggingHelper.logMessage("=== Thread sleeping for " + sleepTime + " ms");
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Map<Page, Document> successfulPages = listOfPages.parallelStream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    scraperService::getWebPageIfUrlReachable
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

            successfulPages.entrySet().parallelStream()
                .forEach(entry -> scraperService.storePageContent(entry.getKey(), entry.getValue()));

            totalPages.addAndGet(listOfPages.size());
            totalSuccessfulPages.addAndGet(successfulPages.size());

            LoggingHelper.logEndOfMethod(
                "fetch pages (got " + successfulPages.size() + " out of " + listOfPages.size() + " pages this iteration)",
                startTimePageIteration
            );
        });

        LoggingHelper.logEndOfMethod("fetch pages (got " + totalSuccessfulPages.get() + " out of " + totalPages.get() + " total pages)", startTimePages);
    }

    private void processWebsites() throws IOException {
        if (processWebsitesJobEnabled) {
            LocalTime startTime = LoggingHelper.logStartOfMethod("processWebsite");

            Queue<Website> nextUnprocessedWebsites = new LinkedList<>(
                websiteRepository.getNextDomesticWebsitesThatNeedProcessing()
            );

            TimeLimitedRepeater
                .repeat(() -> scraperService.processWebsite(nextUnprocessedWebsites))
                .repeatWithDefaultTimeLimit();

            LoggingHelper.logEndOfMethod("processWebsites", startTime);
        }
    }

    @Scheduled(fixedRate = 1000)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void fixDuplicateWebsites() {
        if (fixDuplicateWebsitesJobEnabled) {
            Optional<String> nextDuplicateWebsiteUrl = websiteRepository.getNextDuplicateWebsiteUrl();
            if (nextDuplicateWebsiteUrl.isPresent()) {
                List<Website> websitesMatchingUrl = websiteRepository.findAllByUrlOrderByWebsiteId(nextDuplicateWebsiteUrl.get());
                scraperService.fixDuplicateWebsite(websitesMatchingUrl);
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void establishSubdomainRelationships() {
        if (establishSubdomainRelationshipsJobEnabled) {
            Optional<Website> nextWebsite = websiteRepository.getNextWebsiteNotMarkedAsDomainOrSubdomain();
            nextWebsite.ifPresent(scraperService::establishSubdomainRelationshipsForWebsite);
        }
    }
}
