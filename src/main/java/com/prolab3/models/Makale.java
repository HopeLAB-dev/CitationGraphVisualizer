package com.prolab3.models;

import java.util.ArrayList;
import java.util.List;

public class Makale {
    private String id;
    private String title;
    private int year;
    private List<String> authors;
    private List<String> referencedWorks;

    public Makale(String id, String title, int year, List<String> authors, List<String> referencedWorks) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.authors = authors != null ? authors : new ArrayList<>();
        this.referencedWorks = referencedWorks != null ? referencedWorks : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public List<String> getAuthors() { return authors; }
    public List<String> getReferencedWorks() { return referencedWorks; }
}
