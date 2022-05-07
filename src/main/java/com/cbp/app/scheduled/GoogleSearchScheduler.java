package com.cbp.app.scheduled;

import com.cbp.app.client.GoogleSearchClient;
import com.cbp.app.helper.LoggingHelper;
import com.cbp.app.model.db.GoogleSearch;
import com.cbp.app.model.db.GoogleSearchTerm;
import com.cbp.app.model.db.Website;
import com.cbp.app.model.response.GoogleSearch.GoogleSearchResponse;
import com.cbp.app.repository.GoogleSearchRepository;
import com.cbp.app.repository.GoogleSearchTermRepository;
import com.cbp.app.repository.WebsiteRepository;
import com.cbp.app.service.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Component
public class GoogleSearchScheduler {

    private final GoogleSearchClient googleSearchClient;
    private final GoogleSearchRepository googleSearchRepository;
    private final GoogleSearchTermRepository googleSearchTermRepository;
    private final WebsiteRepository websiteRepository;
    private final boolean jobEnabled;

    @Autowired
    public GoogleSearchScheduler(
        GoogleSearchClient googleSearchClient,
        GoogleSearchRepository googleSearchRepository,
        GoogleSearchTermRepository googleSearchTermRepository,
        WebsiteRepository websiteRepository,
        @Value("${google-custom-search-scheduler.enabled}") boolean jobEnabled
    ) {
        this.googleSearchClient = googleSearchClient;
        this.googleSearchRepository = googleSearchRepository;
        this.googleSearchTermRepository = googleSearchTermRepository;
        this.websiteRepository = websiteRepository;
        this.jobEnabled = jobEnabled;
    }

    private static final int ONE_HOUR_IN_MILLISECONDS = 4 * 60 * 60 * 1000;
    private static final int MAX_SEARCH_INDEX = 101;

    @Scheduled(fixedRate = ONE_HOUR_IN_MILLISECONDS)
    public void findNewWebsites() {
        LocalTime startTime = LoggingHelper.logStartOfMethod("Find new websites");
        if (!jobEnabled) {
            return;
        }

        Optional<Integer> nextSearchTermId = googleSearchRepository.getNextSearchTermId();
        Optional<GoogleSearchTerm> nextSearchTerm = nextSearchTermId
            .map(googleSearchTermRepository::findById)
            .orElse(googleSearchTermRepository.getNextUnusedSearchTerm());

        if (!nextSearchTerm.isPresent()) {
            LoggingHelper.logMessage("No search term found");
            return;
        }

        GoogleSearchTerm searchTerm = nextSearchTerm.get();
        LoggingHelper.logMessage(String.format("Using search term: [%s]", searchTerm.getTerm()));
        Optional<Integer> storedNextStartIndex = googleSearchRepository.getNextSearchStartIndexByTermId(searchTerm.getTermId());

        if (!storedNextStartIndex.isPresent()) {
            LoggingHelper.logMessage("Search term results exhausted");
            return;
        }

        Integer startIndex = storedNextStartIndex.get();
        GoogleSearchResponse response = googleSearchClient.fetchNextSearchResults(searchTerm.getTerm(), startIndex);
        int currentStartIndex = response.getQueries().getRequest().get(0).getStartIndex();
        int nextStartIndex = response.getItems() != null ? currentStartIndex + 10 : MAX_SEARCH_INDEX;

        if (response.getItems() != null) {
            response.getItems().forEach(item -> {
                String websiteUrl = item.getDisplayLink();
                String sanitizedWebsiteUrl = LinkService.sanitizeLinkUrl(websiteUrl);
                Optional<Website> existingWebsite = websiteRepository.findByUrl(sanitizedWebsiteUrl);
                if (!existingWebsite.isPresent()) {
                    Website newWebsite = new Website(item.getTitle(), sanitizedWebsiteUrl, LocalDateTime.now());
                    websiteRepository.save(newWebsite);
                }
            });
        }

        GoogleSearch googleSearch = new GoogleSearch(
            currentStartIndex,
            searchTerm.getTermId(),
            nextStartIndex,
            LocalDateTime.now()
        );
        googleSearchRepository.save(googleSearch);
        LoggingHelper.logEndOfMethod("Find new websites", startTime);
    }
}
