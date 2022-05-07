package com.cbp.app.controller;

import com.cbp.app.model.db.Website;
import com.cbp.app.model.db.WebsiteToWebsite;
import com.cbp.app.model.enumType.WebsiteType;
import com.cbp.app.model.request.WebsiteRequest;
import com.cbp.app.model.response.WebsiteResponse;
import com.cbp.app.model.response.WebsiteToWebsiteResponse;
import com.cbp.app.model.response.WebsiteToWebsitesResponse;
import com.cbp.app.model.response.WebsitesResponse;
import com.cbp.app.repository.WebsiteRepository;
import com.cbp.app.repository.WebsiteToWebsiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
public class WebsiteController {

    private final WebsiteRepository websiteRepository;
    private final WebsiteToWebsiteRepository websiteToWebsiteRepository;

    public WebsiteController(
        WebsiteRepository websiteRepository,
        WebsiteToWebsiteRepository websiteToWebsiteRepository
    ) {
        this.websiteRepository = websiteRepository;
        this.websiteToWebsiteRepository = websiteToWebsiteRepository;
    }

    @RequestMapping(value = "/websites", method = RequestMethod.GET)
    public WebsitesResponse getWebsites() {
        List<Website> websites = websiteRepository.findAll();
        List<WebsiteResponse> websiteResponses = websites.stream()
            .filter(website -> website.getType() != WebsiteType.INDEXING_SERVICE)
            .sorted(Comparator.comparing(Website::getUrl))
            .map(WebsiteResponse::new)
            .collect(Collectors.toList());
        return new WebsitesResponse(websiteResponses);
    }

    @RequestMapping(value = "/websites/website-type/{websiteType}/content-type/{contentType}", method = RequestMethod.GET)
    public WebsitesResponse getWebsitesWithTypeAndContentType(
        @PathVariable String websiteType,
        @PathVariable String contentType
    ) {
        List<Website> websites = websiteRepository.findWebsitesByWebsiteTypeAnAndContentType(websiteType, contentType);
        List<WebsiteResponse> websiteResponses = websites.stream()
            .map(WebsiteResponse::new)
            .collect(Collectors.toList());
        return new WebsitesResponse(websiteResponses);
    }

    @RequestMapping(value = "/website/{websiteId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateWebsite(
        @Valid @RequestBody WebsiteRequest request,
        @PathVariable int websiteId
    ) {
        Optional<Website> websiteOptional = websiteRepository.findById(websiteId);
        if (websiteOptional.isPresent()) {
            Website website = websiteOptional.get();
            website.setUrl(request.getUrl());
            website.setType(request.getType());
            website.setContentType(request.getContentType());
            website.setFetchEveryNumberOfHours(request.getFetchEveryNumberOfHours());
            websiteRepository.save(website);
        }
    }

    @RequestMapping(value = "/website/{websiteId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteWebsite(
        @PathVariable int websiteId
    ) {
        Optional<Website> websiteOptional = websiteRepository.findById(websiteId);
        if (websiteOptional.isPresent()) {
            Website website = websiteOptional.get();
            websiteRepository.delete(website);
        }
    }

    @RequestMapping(value = "/website-to-website", method = RequestMethod.GET)
    public WebsiteToWebsitesResponse getWebsiteToWebsiteLinks() {
        List<WebsiteToWebsite> websiteToWebsites = websiteToWebsiteRepository
            .findAllForLatestContentIdCoalesced();

        List<WebsiteToWebsiteResponse> websiteToWebsiteResponses = websiteToWebsites.stream()
            .map(wtw -> new WebsiteToWebsiteResponse(wtw.getWebsiteIdFrom(), wtw.getWebsiteIdTo()))
            .collect(Collectors.toList());

        return new WebsiteToWebsitesResponse(websiteToWebsiteResponses);
    }

    @RequestMapping(value = "/website-to-website/website-type/{websiteType}/content-type/{contentType}", method = RequestMethod.GET)
    public WebsiteToWebsitesResponse getWebsiteToWebsiteLinksWithTypeAndContentType(
        @PathVariable String websiteType,
        @PathVariable String contentType
    ) {
        List<WebsiteToWebsite> websiteToWebsites = websiteToWebsiteRepository
            .findAllByWebsiteTypeAndContentTypeForLatestContentIdCoalesced(websiteType, contentType);

        List<WebsiteToWebsiteResponse> websiteToWebsiteResponses = websiteToWebsites.stream()
            .map(wtw -> new WebsiteToWebsiteResponse(wtw.getWebsiteIdFrom(), wtw.getWebsiteIdTo()))
            .collect(Collectors.toList());

        return new WebsiteToWebsitesResponse(websiteToWebsiteResponses);
    }
}
