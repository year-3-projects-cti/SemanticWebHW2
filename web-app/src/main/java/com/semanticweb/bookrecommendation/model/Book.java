package com.semanticweb.bookrecommendation.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String uri;
    private String title;
    private String author;
    private List<String> genres = new ArrayList<>();
    private String readingLevel;

    public Book() {}

    public Book(String uri, String title, String author, List<String> genres, String readingLevel) {
        this.uri = uri;
        this.title = title;
        this.author = author;
        this.genres = genres;
        this.readingLevel = readingLevel;
    }

    public String getLocalName() {
        if (uri == null || uri.isEmpty()) return "";
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public String getReadingLevel() { return readingLevel; }
    public void setReadingLevel(String readingLevel) { this.readingLevel = readingLevel; }
}
