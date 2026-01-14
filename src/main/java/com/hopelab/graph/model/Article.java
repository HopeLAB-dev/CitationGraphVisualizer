package com.hopelab.graph.model;

import java.util.ArrayList;
import java.util.List;

public class Article {

    private final String id;


    private String title;
    private int year;
    private List<String> authors;


    private final List<Article> outNeighbors = new ArrayList<>(); // referans verdikleri
    private final List<Article> inNeighbors  = new ArrayList<>(); //referans verenler
    //const
    public Article(String id, String title, int year, List<String> authors) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.authors = (authors != null) ? authors : new ArrayList<>();
    }


    public Article(String id) {
        this(id, "", 0, new ArrayList<>());
    }

    //getterler
    public String getId()      { return id; }
    public String getTitle()   { return title; }
    public int getYear()       { return year; }
    public List<String> getAuthors() { return authors; }

    public List<Article> getOutNeighbors() { return outNeighbors; }
    public List<Article> getInNeighbors()  { return inNeighbors; }

    //setter
    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setAuthors(List<String> authors) {
        this.authors = (authors != null) ? authors : new ArrayList<>();
    }

    //in out neighbor ( atıf iliskileri ) ekleme
    public void addOutNeighbor(Article to) {
        if (!outNeighbors.contains(to)) {
            outNeighbors.add(to);
        }
    }

    public void addInNeighbor(Article from) {
        if (!inNeighbors.contains(from)) {
            inNeighbors.add(from);
        }
    }
}