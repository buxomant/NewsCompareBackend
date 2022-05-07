package com.cbp.app.model;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;

public class ScraperResult {
    Document document;
    Connection.Response response;
    Exception exception;

    public ScraperResult(Document document, Connection.Response response, Exception exception) {
        this.document = document;
        this.response = response;
        this.exception = exception;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Connection.Response getResponse() {
        return response;
    }

    public void setResponse(Connection.Response response) {
        this.response = response;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
