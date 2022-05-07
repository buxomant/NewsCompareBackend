package com.cbp.app.service;

import com.cbp.app.helper.LoggingHelper;
import com.cbp.app.helper.Ticker;
import com.cbp.app.model.SimpleLink;
import com.cbp.app.model.db.*;
import com.cbp.app.model.enumType.WebsiteContentType;
import com.cbp.app.model.enumType.WebsiteType;
import com.cbp.app.repository.*;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cbp.app.helper.Ticker.TickResult.BREAK;
import static com.cbp.app.helper.Ticker.TickResult.CONTINUE;
import static com.cbp.app.service.HelperService.distinctByKey;
import static com.cbp.app.service.IndexService.DATE_AND_HOUR_PATTERN;
import static com.cbp.app.service.IndexService.WEBSITE_STORAGE_PATH;

@Service
public class ScraperService {
    private final WebsiteRepository websiteRepository;
    private final WebsiteContentRepository websiteContentRepository;
    private final PageRepository pageRepository;
    private final WebsiteToWebsiteRepository websiteToWebsiteRepository;
    private final PageToPageRepository pageToPageRepository;
    private final SubdomainOfRepository subdomainOfRepository;

    private static final int TIMEOUT_IN_MILLIS = 1;

    @Autowired
    public ScraperService(
        WebsiteRepository websiteRepository,
        WebsiteContentRepository websiteContentRepository,
        PageRepository pageRepository,
        WebsiteToWebsiteRepository websiteToWebsiteRepository,
        PageToPageRepository pageToPageRepository,
        SubdomainOfRepository subdomainOfRepository
    ) {
        this.websiteRepository = websiteRepository;
        this.websiteContentRepository = websiteContentRepository;
        this.pageRepository = pageRepository;
        this.websiteToWebsiteRepository = websiteToWebsiteRepository;
        this.pageToPageRepository = pageToPageRepository;
        this.subdomainOfRepository = subdomainOfRepository;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void storeWebsiteContent(Website currentWebsite, Document webPage) {
        String url = currentWebsite.getUrl();

        try {
            saveWebsiteTextToFile(webPage, url, currentWebsite);
        } catch (IOException e) {
            return;
        }

        String cleanContent = webPage.toString().replaceAll("\u0000", "");
        if (cleanContent != null && !cleanContent.equals("")) {
            WebsiteContent websiteContent = new WebsiteContent(currentWebsite.getWebsiteId(), cleanContent);
            websiteContent.setTimeFetched(LocalDateTime.now());
            websiteContentRepository.save(websiteContent);
        }

        WebsiteType websiteType = WebsiteService.getWebsiteType(url, webPage.baseUri());

        if (currentWebsite.getContentType() == WebsiteContentType.UNCATEGORIZED) {
            WebsiteContentType websiteContentType = WebsiteService.getWebsiteContentType(webPage.baseUri());
            currentWebsite.setContentType(websiteContentType);
        }

        currentWebsite.setType(websiteType);
        currentWebsite.setLastCheckedOn(LocalDateTime.now());
        currentWebsite.setLastResponseCode(200);
        websiteRepository.save(currentWebsite); // qq do this in batch
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void storePageContent(Page currentPage, Document webPage) {
        String url = currentPage.getUrl();

        try {
            savePageTextToFile(webPage, url, currentPage);
        } catch (IOException e) {
            System.out.println("Failed to save page with URL: " + url + ". Exception: " + e.getMessage());
            return;
        }

        currentPage.setLastCheckedOn(LocalDateTime.now());
        currentPage.setLastResponseCode(200);
        pageRepository.save(currentPage); // qq do this in batch
    }

    public Optional<Document> getWebPageIfUrlReachable(Website currentWebsite) {
        Connection connection = null;
        Document webPage = null;
        Connection.Response response = null;
        String urlIncludingWwwAndProtocol;
        String url = currentWebsite.getUrl();
        String urlIncludingWww = RegexPatternService.urlMissingWwwPattern.matcher(url).matches() ? "www." + url : url;
        urlIncludingWwwAndProtocol = "https://" + urlIncludingWww;

        try {
            connection = Jsoup.connect(urlIncludingWwwAndProtocol).timeout(TIMEOUT_IN_MILLIS);
            response = connection.execute();
            webPage = connection.get();
        } catch (HttpStatusException | SSLException | SocketException | SocketTimeoutException e) {
            urlIncludingWwwAndProtocol = "http://" + urlIncludingWww;
        } catch (IOException e) {
            saveWebsiteError(currentWebsite, response, e.getMessage());
            return Optional.empty();
        }

        if (webPage == null) {
            try {
                connection = Jsoup.connect(urlIncludingWwwAndProtocol);
                response = connection.execute();
                webPage = connection.get();
            } catch (IOException e) {
                saveWebsiteError(currentWebsite, response, e.getMessage());
                return Optional.empty();
            }
        }

        return Optional.of(webPage);
    }

    public Optional<Document> getWebPageIfUrlReachable(Page currentPage) {
        Connection connection = null;
        Connection.Response response = null;
        Document webPage = null;
        String urlIncludingWwwAndProtocol;
        String url = currentPage.getUrl();
        String urlIncludingWww = RegexPatternService.urlMissingWwwPattern.matcher(url).matches() ? "www." + url : url;
        urlIncludingWwwAndProtocol = "https://" + urlIncludingWww;

        try {
            connection = Jsoup.connect(urlIncludingWwwAndProtocol).timeout(TIMEOUT_IN_MILLIS);
            response = connection.execute();
            webPage = connection.get();
        } catch (HttpStatusException | SSLException | SocketException | SocketTimeoutException e) {
            urlIncludingWwwAndProtocol = "http://" + urlIncludingWww;
        } catch (IOException e) {
            savePageError(currentPage, response, e.getMessage());
            return Optional.empty();
        }

        if (webPage == null) {
            try {
                connection = Jsoup.connect(urlIncludingWwwAndProtocol).timeout(TIMEOUT_IN_MILLIS);
                response = connection.execute();
                webPage = connection.get();
            } catch (IOException e) {
                savePageError(currentPage, response, e.getMessage());
                return Optional.empty();
            }
        }

        return Optional.of(webPage);
    }

    private void saveWebsiteTextToFile(Document webPage, String url, Website currentWebsite) throws IOException {
        String dateAndHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_AND_HOUR_PATTERN));
        String workingDirectory = WEBSITE_STORAGE_PATH + "/" + dateAndHour;
        File directory = new File(workingDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = workingDirectory + "/" + url + "_website_" + currentWebsite.getWebsiteId() + ".txt";
        File websiteFile = new File(fileName);
        if (!websiteFile.exists()) {
            websiteFile.createNewFile();
        }
        try (PrintStream out = new PrintStream(new FileOutputStream(websiteFile))) {
            out.print(webPage.text());
        }
    }

    private void savePageTextToFile(Document webPage, String url, Page currentPage) throws IOException {
        String dateAndHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_AND_HOUR_PATTERN));
        String workingDirectory = WEBSITE_STORAGE_PATH + "/" + dateAndHour;
        File directory = new File(workingDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String urlAsValidFileName = url
            .replaceAll("/", "[]")
            .replaceAll("\\\\", "][")
            .replaceAll("\\?", "")
            .replaceAll(":", "")
            .replaceAll("<", "")
            .replaceAll(">", "")
            .replaceAll("\\|", "")
            .replaceAll("_", "")
            .replaceAll("\\*", "")
            .replaceAll("\"", "");

        int urlLimit = Math.min(urlAsValidFileName.length(), 180);
        String lengthLimitedUrl = urlAsValidFileName.substring(0, urlLimit);

        String fileName = workingDirectory + "/" + lengthLimitedUrl + "_page_" + currentPage.getPageId() + ".txt";
        File websiteFile = new File(fileName);
        if (!websiteFile.exists()) {
            websiteFile.createNewFile();
        }
        try (PrintStream out = new PrintStream(new FileOutputStream(websiteFile))) {
            out.print(webPage.text());
        }
    }

    private void saveWebsiteError(Website currentWebsite, Connection.Response response, String errorMessage) {
        currentWebsite.setError(errorMessage);
        currentWebsite.setLastCheckedOn(LocalDateTime.now());
        if (response != null) {
            currentWebsite.setLastResponseCode(response.statusCode());
        }
        websiteRepository.save(currentWebsite);
    }

    private void savePageError(Page currentPage, Connection.Response response, String errorMessage) {
        currentPage.setError(errorMessage);
        currentPage.setLastCheckedOn(LocalDateTime.now());
        if (response != null) {
            currentPage.setLastResponseCode(response.statusCode());
        }
        pageRepository.save(currentPage);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Ticker.TickResult processWebsite(Queue<Website> websites) {
        Website currentWebsite = websites.poll();
        if (currentWebsite == null) {
            return BREAK;
        }

        LocalTime startTime = LoggingHelper.logStartOfMethod("Process website");

        String url = currentWebsite.getUrl();
        WebsiteContent websiteContent = websiteContentRepository.getFirstByWebsiteIdAndTimeProcessedIsNull(currentWebsite.getWebsiteId());
        Document webPage = Jsoup.parse(websiteContent.getContent());
        Elements linkElements = webPage.select("[href]");

        List<SimpleLink> links = LinkService.domLinksToSimpleLinks(linkElements, url);
        processLinks(links, currentWebsite, websiteContent);

        currentWebsite.setLastProcessedOn(LocalDateTime.now());
        websiteRepository.save(currentWebsite);

        websiteContent.setTimeProcessed(LocalDateTime.now());
        websiteContentRepository.save(websiteContent);

        LoggingHelper.logMessage("Processed website: " + currentWebsite.getUrl() + " (" + websites.size() + " left)");
        LoggingHelper.logMessage("Websites left: " + websites.size());
        LoggingHelper.logEndOfMethod("Process website", startTime);

        return CONTINUE;
    }

    private void processLinks(List<SimpleLink> links, Website currentWebsite, WebsiteContent websiteContent) {
        Map<SimpleLink, Website> websitesByLinkTitle = linksToWebsitesByLinkTitle(links);

        List<String> websiteUrls = websitesByLinkTitle.values().stream().map(Website::getUrl).collect(Collectors.toList());
        List<Website> existingWebsites = websiteRepository.findAllByUrlIn(websiteUrls);
        List<String> existingWebsiteUrls = existingWebsites.stream().map(Website::getUrl).collect(Collectors.toList());
        List<Website> newWebsites = websitesByLinkTitle.values().stream()
            .filter(website -> !existingWebsiteUrls.contains(website.getUrl())).collect(Collectors.toList());
        List<Website> savedNewWebsites = websiteRepository.saveAll(newWebsites);

        websitesByLinkTitle.replaceAll((link, website) -> {
            Optional<Website> existingWebsite = existingWebsites.stream()
                .filter(w -> w.getUrl().equals(website.getUrl()))
                .findFirst();
            if (existingWebsite.isPresent()) {
                return existingWebsite.get();
            }
            Optional<Website> newWebsite = savedNewWebsites.stream()
                .filter(w -> w.getUrl().equals(website.getUrl()))
                .findFirst();
            return newWebsite.orElse(website);
        });

        List<WebsiteToWebsite> websiteToWebsites = createWebsiteToWebsiteLinks(websitesByLinkTitle, websiteContent);
        websiteToWebsiteRepository.saveAll(websiteToWebsites);

        List<Website> allWebsites = new ArrayList<>();
        allWebsites.addAll(existingWebsites);
        allWebsites.addAll(savedNewWebsites);
        Map<SimpleLink, Page> pagesByLinkTitle = linksToPagesByLinkTitle(links, allWebsites);

        List<String> pageUrls = pagesByLinkTitle.values().stream().map(Page::getUrl).collect(Collectors.toList());
        List<Page> existingPages = pageRepository.findAllByUrlIn(pageUrls);
        List<String> existingPageUrls = existingPages.stream().map(Page::getUrl).collect(Collectors.toList());
        List<Page> newPages = pagesByLinkTitle.values().stream()
            .filter(page -> !existingPageUrls.contains(page.getUrl()))
            .map(ScraperService::updateLastSeenToNow)
            .collect(Collectors.toList());
        List<Page> savedNewPages = pageRepository.saveAll(newPages);

        List<Page> updatedExistingPages = existingPages.stream()
            .map(ScraperService::updateLastSeenToNow)
            .collect(Collectors.toList());
        pageRepository.saveAll(updatedExistingPages);

        pagesByLinkTitle.replaceAll((link, page) -> {
            Optional<Page> existingPage = existingPages.stream()
                .filter(p -> p.getUrl().equals(page.getUrl()))
                .findFirst();
            if (existingPage.isPresent()) {
                return existingPage.get();
            }
            Optional<Page> newPage = savedNewPages.stream()
                .filter(p -> p.getUrl().equals(page.getUrl()))
                .findFirst();
            return newPage.orElse(page);
        });

        List<Page> pagesMatchingUrl = pageRepository.findAllByUrlOrderByPageId(currentWebsite.getUrl());
        Page currentPage = pagesMatchingUrl.size() > 0
            ? fixDuplicatePage(pagesMatchingUrl)
            : WebsiteService.createNewPageForWebsiteHomePage(websiteRepository.getOne(websiteContent.getWebsiteId()));
        Page savedCurrentPage = (currentPage.getPageId() == 0)
            ? pageRepository.save(currentPage)
            : currentPage;

        List<PageToPage> pageToPages = createPageToPageLinks(pagesByLinkTitle, savedCurrentPage, pagesMatchingUrl, websiteContent);
        pageToPageRepository.saveAll(pageToPages);
    }

    private static Page updateLastSeenToNow(Page page) {
        page.setLastSeen(LocalDateTime.now());
        return page;
    }

    private Map<SimpleLink, Website> linksToWebsitesByLinkTitle(List<SimpleLink> links) {
        return links.stream()
            .map(LinkService::stripSubPage)
            .filter(distinctByKey(SimpleLink::getLinkUrl))
            .collect(Collectors.toMap(
                Function.identity(),
                link -> WebsiteService.createNewWebsite(link.getLinkUrl())
            ));
    }

    private Map<SimpleLink, Page> linksToPagesByLinkTitle(List<SimpleLink> links, List<Website> websites) {
        return links.stream()
            .filter(distinctByKey(SimpleLink::getLinkUrl))
            .collect(Collectors.toMap(
                Function.identity(),
                link -> {
                    String linkUrl = link.getLinkUrl();
                    Website website = websites.stream().filter(w -> linkUrl.contains(w.getUrl())).findFirst().get();
                    return new Page(linkUrl, LocalDateTime.now(), website.getWebsiteId());
                }
            ));
    }

    private Map<SimpleLink, Page> websitesByLinkTitleToPagesByLinkTitle(Map<SimpleLink, Website> websitesByLinkTitle) {
        return websitesByLinkTitle.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> WebsiteService.createNewPageForWebsiteHomePage(entry.getValue())
            ));
    }

    private List<WebsiteToWebsite> createWebsiteToWebsiteLinks(
        Map<SimpleLink, Website> websitesByLinkTitle,
        WebsiteContent websiteContent
    ) {
        return websitesByLinkTitle.entrySet().stream()
            .map(entry -> new WebsiteToWebsite(
                websiteContent.getWebsiteId(),
                entry.getValue().getWebsiteId(),
                websiteContent.getContentId(),
                entry.getKey().getLinkTitle()
            ))
            .collect(Collectors.toList());
    }

    private List<PageToPage> createPageToPageLinks(
        Map<SimpleLink, Page> pagesByLinkTitle,
        Page savedCurrentPage,
        List<Page> pagesMatchingUrl,
        WebsiteContent websiteContent
    ) {
        return pagesByLinkTitle.entrySet().stream()
            .filter(entry -> !(pagesMatchingUrl.contains(entry.getValue()) && entry.getValue().getPageId() != savedCurrentPage.getPageId()))
            .map(entry -> new PageToPage(
                savedCurrentPage.getPageId(),
                entry.getValue().getPageId(),
                websiteContent.getContentId(),
                entry.getKey().getLinkTitle()
            ))
            .collect(Collectors.toList());
    }

    public Website fixDuplicateWebsite(List<Website> websitesMatchingUrl) {
        Website earliestWebsite = websitesMatchingUrl.get(0);

        if (websitesMatchingUrl.size() > 1) {
            websitesMatchingUrl.forEach(website -> {
                if (website.getWebsiteId() != earliestWebsite.getWebsiteId()) {
                    LocalTime startTime = LoggingHelper.logStartOfMethod("Fix duplicate website");

                    List<Page> pages = pageRepository.findAllByWebsiteId(website.getWebsiteId());
                    pages.forEach(page -> {
                        pageToPageRepository.deleteAllByPageIdFrom(page.getPageId());
                        pageToPageRepository.deleteAllByPageIdTo(page.getPageId());
                    });

                    pageRepository.deleteAllByWebsiteId(website.getWebsiteId());
                    websiteToWebsiteRepository.deleteAllByWebsiteIdFrom(website.getWebsiteId());
                    websiteToWebsiteRepository.findAllByWebsiteIdTo(website.getWebsiteId()).forEach(websiteToWebsite -> {
                        websiteToWebsite.setWebsiteIdTo(earliestWebsite.getWebsiteId());
                        websiteToWebsiteRepository.save(websiteToWebsite);
                    });
                    subdomainOfRepository.findAllByWebsiteIdParent(website.getWebsiteId()).forEach(subdomainOf -> {
                        subdomainOf.setWebsiteIdParent(earliestWebsite.getWebsiteId());
                        subdomainOfRepository.save(subdomainOf);
                    });
                    subdomainOfRepository.findAllByWebsiteIdChild(website.getWebsiteId()).forEach(subdomainOf -> {
                        subdomainOf.setWebsiteIdChild(earliestWebsite.getWebsiteId());
                        subdomainOfRepository.save(subdomainOf);
                    });

                    websiteRepository.delete(website);

                    LoggingHelper.logEndOfMethod("Fix duplicate website", startTime);
                }
            });
        }

        return earliestWebsite;
    }

    private Page fixDuplicatePage(List<Page> pagesMatchingUrl) {
        Page earliestPage = pagesMatchingUrl.get(0);
        pagesMatchingUrl.forEach(page -> {
            if (page.getPageId() != earliestPage.getPageId()) {
                LocalTime startTime = LoggingHelper.logStartOfMethod("Fix duplicate page");

                pageToPageRepository.deleteAllByPageIdFrom(page.getPageId());
                pageToPageRepository.findAllByPageIdTo(page.getPageId()).forEach(pageToPage -> {
                    pageToPage.setPageIdTo(earliestPage.getPageId());
                    pageToPageRepository.save(pageToPage);
                });

                pageRepository.delete(page);

                LoggingHelper.logEndOfMethod("Fix duplicate page", startTime);
            }
        });

        return earliestPage;
    }

    public void establishSubdomainRelationshipsForWebsite(Website website) {
        LocalTime startTime = LoggingHelper.logStartOfMethod("Establish subdomain relationships for website");

        List<Website> websiteSubdomains = websiteRepository.getSubdomainsForUrl(website.getUrl());
        websiteSubdomains.forEach(subdomain -> {
            SubdomainOf subdomainRelationship = new SubdomainOf(website.getWebsiteId(), subdomain.getWebsiteId());
            subdomainOfRepository.save(subdomainRelationship);
        });

        LoggingHelper.logEndOfMethod("Establish subdomain relationships for website", startTime);
    }
}
