package com.cbp.app.model;

import com.cbp.app.service.LinkService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleLink {
    private String linkTitle;
    private String linkUrl;

    public SimpleLink(String linkTitle, String linkUrl) {
        this.linkTitle = linkTitle;
        this.linkUrl = LinkService.sanitizeLinkUrl(linkUrl);
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }
}
