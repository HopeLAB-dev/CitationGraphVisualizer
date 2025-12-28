package com.hopelab.graph;

import java.util.ArrayList;
import java.util.List;

public class Article {

    private final String id;

    // Artık final DEĞİL, değiştirilebilir
    private String title;
    private int year;
    private List<String> authors;
    private String venue;

    // Graf ilişkileri
    private final List<Article> outNeighbors = new ArrayList<>(); // bu makalenin referans verdiği
    private final List<Article> inNeighbors  = new ArrayList<>(); // bu makaleye referans veren

    // En genel ctor: JSON’dan okurken kullanabiliriz
    public Article(String id, String title, int year, List<String> authors, String venue) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.authors = (authors != null) ? authors : new ArrayList<>();
        this.venue = venue;
    }

    // GraphLoader’ın "ensureArticle" için kullanacağı basit ctor
    // (ilk başta sadece id ile oluşturup sonradan setTitle/setYear ile dolduracağız)
    public Article(String id) {
        this(id, "", 0, new ArrayList<>(), "");
    }

    // ---- GETTER’LAR ----
    public String getId()      { return id; }
    public String getTitle()   { return title; }
    public int getYear()       { return year; }
    public List<String> getAuthors() { return authors; }
    public String getVenue()   { return venue; }

    public List<Article> getOutNeighbors() { return outNeighbors; }
    public List<Article> getInNeighbors()  { return inNeighbors; }

    // ---- SETTER’LAR (GraphLoader için) ----
    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setAuthors(List<String> authors) {
        this.authors = (authors != null) ? authors : new ArrayList<>();
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    // ---- KOMŞU EKLEME (CitationGraph.addEdge için) ----
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
