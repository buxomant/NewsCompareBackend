package com.cbp.app.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class RegexPatternService {
    private final Pattern urlMissingWwwPattern;
    private final Pattern urlIncludingWwwPattern;
    private final Pattern globalLinkPattern;
    private final Pattern localLinkPattern;
    private final Pattern queryStringPattern;
    private final Pattern subPagePattern;
    private final Pattern nonWebResourcePattern;
    private final Pattern domesticWebsitePattern;
    private final Pattern domesticNewsWebsitePattern;
    private final Pattern socialMediaWebsitePattern;
    private final Pattern indexingServicePattern;

    public RegexPatternService() {
        this.urlMissingWwwPattern = Pattern.compile("^\\w+\\.ro");
        this.urlIncludingWwwPattern = Pattern.compile("^www\\..*");
        this.globalLinkPattern = Pattern.compile("^\\/{2}.+");
        this.localLinkPattern = Pattern.compile("^\\/.+");
        this.queryStringPattern = Pattern.compile("(.*)\\?.*");
        this.subPagePattern = Pattern.compile("(.*\\.ro)/.*");
        this.nonWebResourcePattern = Pattern.compile(".*\\.(?:bmp|jpg|jpeg|png|gif|svg|pdf|doc|docx|xls|xlsx|ppt|pptx|ashx|xml)$");
        this.domesticWebsitePattern = Pattern.compile(".+\\.ro.*");
        this.domesticNewsWebsitePattern = Pattern.compile(
            ".*adevarul\\.ro.*" +
            "|.*stirileprotv\\.ro.*" +
            "|.*libertatea\\.ro.*" +
            "|.*digi24\\.ro.*" +
            "|.*a1\\.ro.*" +
            "|.*antena3\\.ro.*" +
            "|.*cancan\\.ro.*" +
            "|.*realitatea\\.net.*" +
            "|.*romaniatv\\.net.*" +
            "|.*unica\\.net.*" +
            "|.*evz\\.ro.*" +
            "|.*gsp\\.ro.*" +
            "|.*click\\.ro.*" +
            "|.*csid\\.ro.*" +
            "|.*sfatulmedicului\\.ro.*" +
            "|.*digisport\\.ro.*" +
            "|.*ziare\\.com.*" +
            "|.*sport\\.ro.*" +
            "|.*stiripesurse\\.ro.*" +
            "|.*hotnews\\.ro.*" +
            "|.*mediafax\\.ro.*" +
            "|.*spynews\\.ro.*" +
            "|.*avocatnet\\.ro.*" +
            "|.*teotrandafir\\.com.*" +
            "|.*wowbiz\\.ro.*" +
            "|.*protv\\.ro.*" +
            "|.*zf\\.ro.*" +
            "|.*prosport\\.ro.*" +
            "|.*unica\\.ro.*" +
            "|.*kudika\\.ro.*" +
            "|.*ziaruldeiasi\\.ro.*" +
            "|.*gandul\\.ro.*" +
            "|.*gandul\\.info.*"
        );
        this.socialMediaWebsitePattern = Pattern.compile(
            ".*facebook\\.com.*" +
            "|.*fb\\.com.*" +
            "|.*twitter\\.com.*" +
            "|.*instagram\\.com.*" +
            "|.*last\\.fm.*" +
            "|.*pinterest\\.com.*" +
            "|.*linkedin\\.com.*" +
            "|.*youtube\\.com.*"
        );
        this.indexingServicePattern = Pattern.compile(
            ".*google\\.com.*" +
            "|.*alexa\\.com.*" +
            "|.*blogger\\.com.*" +
            "|.*trustpilot\\.com.*" +
            "|.*wordpress\\.com.*" +
            "|.*outlook\\.com.*" +
            "|.*blogspot\\.com.*" +
            "|.*archive\\.org.*" +
            "|.*creativecommons\\.org.*" +
            "|.*webstatsdomain\\.org.*" +
            "|.*webstatsdomain\\.com.*" +
            "|.*gov\\.uk.*"
        );
    }

    public Pattern getUrlMissingWwwPattern() {
        return urlMissingWwwPattern;
    }

    public Pattern getUrlIncludingWwwPattern() {
        return urlIncludingWwwPattern;
    }

    public Pattern getGlobalLinkPattern() {
        return globalLinkPattern;
    }

    public Pattern getLocalLinkPattern() {
        return localLinkPattern;
    }

    public Pattern getQueryStringPattern() {
        return queryStringPattern;
    }

    public Pattern getSubPagePattern() {
        return subPagePattern;
    }

    public Pattern getNonWebResourcePattern() {
        return nonWebResourcePattern;
    }

    public Pattern getDomesticWebsitePattern() {
        return domesticWebsitePattern;
    }

    public Pattern getDomesticNewsWebsitePattern() {
        return domesticNewsWebsitePattern;
    }

    public Pattern getSocialMediaWebsitePattern() {
        return socialMediaWebsitePattern;
    }

    public Pattern getIndexingServicePattern() {
        return indexingServicePattern;
    }
}
