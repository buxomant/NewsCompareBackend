package com.cbp.app.service;

import com.cbp.app.model.SimpleLink;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LinkService {
    public static String sanitizeLinkUrl(String linkUrl) {
        List<String> link = Collections.singletonList(linkUrl);
        return link.stream()
            .map(String::trim)
            .map(String::toLowerCase)
            .map(LinkService::trimNonAlphanumericContent)
            .map(LinkService::stripProtocolPrefix)
            .map(LinkService::stripWwwPrefix)
            .map(LinkService::stripAnchorString)
            .map(LinkService::stripQueryString)
            .map(LinkService::stripAsteriskString)
            .map(LinkService::trimNonAlphanumericContent)
            .collect(Collectors.toList())
            .get(0);
    }

    public static boolean shouldSaveUrl(String linkUrl) {
        return LinkService.isNotIPOrPhoneNumber(linkUrl)
            && LinkService.isNotIgnorable(linkUrl)
            && LinkService.isValidWebUrl(linkUrl)
            && LinkService.isNotEmptyOrUseless(linkUrl)
            && LinkService.isNotJavascriptFunction(linkUrl);
    }

    public static String stripProtocolPrefix(String linkUrl) {
        return linkUrl
            .replace("https//", "")
            .replace("https://", "")
            .replace("https:/", "")
            .replace("https:\\\\", "")
            .replace("https:\\", "")
            .replace("http//", "")
            .replace("http://", "")
            .replace("http:/", "")
            .replace("http:\\\\", "")
            .replace("http:\\", "");
    }

    public static String stripWwwPrefix(String linkUrl) {
        if (linkUrl.startsWith("www.")) {
            return linkUrl.replaceAll("www.", "");
        } else {
            return linkUrl;
        }
    }

    public static SimpleLink convertLocalLinks(SimpleLink link, String baseUrl) {
        String linkUrl = link.getLinkUrl();
        if (RegexPatternService.localPageLinkPattern.matcher(linkUrl).matches()
            || RegexPatternService.localLinkPattern.matcher(linkUrl).matches()
            || RegexPatternService.dateStringPattern.matcher(linkUrl).matches()
            || !linkUrl.contains(".")
            || (linkUrl.contains(".-") && !linkUrl.contains(".ro"))
        ) {
            link.setLinkUrl(baseUrl + "/" + linkUrl);
        }

        return link;
    }

    public static String stripAnchorString(String linkUrl) {
        Matcher matcher = RegexPatternService.anchorStringPattern.matcher(linkUrl);
        return matcher.matches() ? matcher.group(1) : linkUrl;
    }

    public static String stripQueryString(String linkUrl) {
        Matcher matcher = RegexPatternService.queryStringPattern.matcher(linkUrl);
        return matcher.matches() ? matcher.group(1) : linkUrl;
    }

    public static String stripAsteriskString(String linkUrl) {
        Matcher matcher = RegexPatternService.asteriskStringPattern.matcher(linkUrl);
        return matcher.matches() ? matcher.group(1) : linkUrl;
    }

    public static SimpleLink stripSubPage(SimpleLink link) {
        Matcher matcher = RegexPatternService.subPagePattern.matcher(link.getLinkUrl());
        return matcher.matches()
            ? new SimpleLink(link.getLinkTitle(), matcher.group(1))
            : link;
    }

    public static String trimNonAlphanumericContent(String linkUrl) {
        String linkWithoutNewlinesOrSpaces = linkUrl
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", ""); // qq rewrite more concisely
        Matcher matcher = RegexPatternService.alphanumericContentPattern.matcher(linkWithoutNewlinesOrSpaces);
        return matcher.replaceAll("");
    }

    public static boolean isNotEmptyOrUseless(String linkUrl) {
        return !linkUrl.equals("") && !linkUrl.equals("/") && !linkUrl.equals("#");
    }

    public static boolean isNotJavascriptFunction(String linkUrl) {
        return !linkUrl.contains("()");
    }

    public static boolean isNotIPOrPhoneNumber(String linkUrl) {
        return !RegexPatternService.ipStringPattern.matcher(linkUrl).matches()
            && !RegexPatternService.phoneStringPattern.matcher(linkUrl).matches();
    }

    public static boolean isNotIgnorable(String linkUrl) {
        return !RegexPatternService.miscIgnorablePattern.matcher(linkUrl).matches();
    }

    public static boolean isValidWebUrl(String linkUrl) {
        return !linkUrl.contains("@")
            && !linkUrl.startsWith("#")
            && !RegexPatternService.nonWebProtocolPattern.matcher(linkUrl).matches()
            && !RegexPatternService.nonWebResourcePattern.matcher(linkUrl).matches();
    }

    public static List<SimpleLink> domLinksToSimpleLinks(Elements linkElements, String currentWebsiteUrl) {
        return linkElements.parallelStream()
            .map(link -> new SimpleLink(link.text(), link.attr("href")))
            .map(link -> LinkService.convertLocalLinks(link, currentWebsiteUrl))
            .filter(link -> LinkService.shouldSaveUrl(link.getLinkUrl()))
            .distinct()
            .collect(Collectors.toList());
    }

    public static String urlToTopDomain(String url) {
        String baseUrl = url.split("\\[]")[0];
        if (Pattern.compile(".*\\..*.*\\..*.*").matcher(baseUrl).matches()) {
            String[] baseUrlSplit = baseUrl.split("\\.");
            return baseUrlSplit[baseUrlSplit.length - 2] + "." + baseUrlSplit[baseUrlSplit.length - 1];
        } else {
            return baseUrl;
        }
    }
}
