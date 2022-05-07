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
    private final GoogleSearchScheduler googleSearchScheduler;
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
        GoogleSearchScheduler googleSearchScheduler,
        @Value("${fetch-websites-scheduler.enabled}") boolean fetchWebsitesJobEnabled,
        @Value("${process-websites-scheduler.enabled}") boolean processWebsitesJobEnabled,
        @Value("${fix-duplicate-websites-scheduler.enabled}") boolean fixDuplicateWebsitesJobEnabled,
        @Value("${establish-subdomain-relationships.enabled}") boolean establishSubdomainRelationshipsJobEnabled
    ) {
        this.websiteRepository = websiteRepository;
        this.pageRepository = pageRepository;
        this.scraperService = scraperService;
        this.indexService = indexService;
        this.googleSearchScheduler = googleSearchScheduler;
        this.fetchWebsitesJobEnabled = fetchWebsitesJobEnabled;
        this.processWebsitesJobEnabled = processWebsitesJobEnabled;
        this.fixDuplicateWebsitesJobEnabled = fixDuplicateWebsitesJobEnabled;
        this.establishSubdomainRelationshipsJobEnabled = establishSubdomainRelationshipsJobEnabled;
    }

    @Scheduled(fixedRate = 4 * 60 * 60 * 1000)
    @SchedulerLock(name = "fetchWebsitesContent")
    public void fetchWebsitesContent() throws IOException, ExecutionException, InterruptedException {
        if (fetchWebsitesJobEnabled) {
            googleSearchScheduler.findNewWebsites();
            fetchWebsites();
            fetchPages();
            processWebsites();
            indexService.indexAndCompareWebsites();
        }
    }

    private void fetchWebsites() {
        LocalTime startTime = LoggingHelper.logStartOfMethod("Fetch websites");

        List<Website> nextUncheckedWebsites = websiteRepository.getNextWebsitesThatNeedFetching();

        LoggingHelper.logMessage(String.format("Found %s websites that need fetching", nextUncheckedWebsites.size()));

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

        LoggingHelper.logEndOfMethod("Fetch websites", startTime);
        LoggingHelper.logMessage(String.format("" +
            "Websites contacted: %s, Websites successful: %s [%s]",
            nextUncheckedWebsites.size(),
            successfulWebsites.size(),
            formatSuccessfulPercentageOfTotal(successfulWebsites.size(), nextUncheckedWebsites.size())
        ));
    }

    private void fetchPages() {
        LocalTime startTimePageProcessing = LoggingHelper.logStartOfMethod("Fetch pages");

        List<Page> nextUncheckedPages = pageRepository.getNextPagesThatNeedFetching();

        LoggingHelper.logMessage(String.format("Found %s pages that need fetching", nextUncheckedPages.size()));

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

        LoggingHelper.logEndOfMethod("Fetch pages (done processing)", startTimePageProcessing);

        LocalTime startTimePages = LoggingHelper.logStartOfMethod("Fetch pages (parallel)");
        Random random = new Random();

        AtomicInteger totalPages = new AtomicInteger(0);
        AtomicInteger totalSuccessfulPages = new AtomicInteger(0);
        listsOfListsOfPages.parallelStream().forEach(listOfPages -> {

            LocalTime startTimePageIteration = LoggingHelper.logStartOfMethod("Fetch pages (iteration)");

            try {
                long sleepTime = random.nextInt(100) * 100;
                LoggingHelper.logMessage(String.format("Thread %s sleeping for %s ms", Thread.currentThread().getId(), sleepTime));
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
                "Fetch pages (iteration)",
                startTimePageIteration
            );

            LoggingHelper.logMessage(String.format(
                "Pages contacted: %s, Pages successful: %s [%s]",
                listOfPages.size(),
                successfulPages.size(),
                formatSuccessfulPercentageOfTotal(successfulPages.size(), listOfPages.size())
            ));
        });

        LoggingHelper.logEndOfMethod("Fetch pages (total)", startTimePages);
        LoggingHelper.logMessage(String.format(
            "Pages contacted: %s, Pages successful: %s [%s]",
            totalPages.get(),
            totalSuccessfulPages.get(),
            formatSuccessfulPercentageOfTotal(totalSuccessfulPages.get(), totalPages.get())
        ));
    }

    private static String formatSuccessfulPercentageOfTotal(int successful, int total) {
        double percentageSuccessful = total > 0 ? (double) successful / (double) total : 0;
        return String.format("%.2f%%", percentageSuccessful * 100);
    }

    private void processWebsites() {
        if (processWebsitesJobEnabled) {
            LocalTime startTime = LoggingHelper.logStartOfMethod("Process websites");

            Queue<Website> nextUnprocessedWebsites = new LinkedList<>(
                websiteRepository.getNextWebsitesThatNeedProcessing()
            );

            TimeLimitedRepeater
                .repeat(() -> scraperService.processWebsite(nextUnprocessedWebsites))
                .repeatWithDefaultTimeLimit();

            LoggingHelper.logEndOfMethod("Process websites", startTime);
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
