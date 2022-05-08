package com.cbp.app.model;

public class ProcessingResult {
    int noOfExternalLinks;
    int noOfInternalLinks;

    public ProcessingResult(int noOfExternalLinks, int noOfInternalLinks) {
        this.noOfExternalLinks = noOfExternalLinks;
        this.noOfInternalLinks = noOfInternalLinks;
    }

    public int getNoOfExternalLinks() {
        return noOfExternalLinks;
    }

    public void setNoOfExternalLinks(int noOfExternalLinks) {
        this.noOfExternalLinks = noOfExternalLinks;
    }

    public int getNoOfInternalLinks() {
        return noOfInternalLinks;
    }

    public void setNoOfInternalLinks(int noOfInternalLinks) {
        this.noOfInternalLinks = noOfInternalLinks;
    }
}
