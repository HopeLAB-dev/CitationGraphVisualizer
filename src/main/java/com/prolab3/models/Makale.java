package com.prolab3.models;

import java.util.ArrayList;
import java.util.List;

public class Makale {
    public String id;
    public String title;
    public int year;
    public List<String> authors = new ArrayList<>();
    public List<String> referencedWorks = new ArrayList<>();
    public int citationCount = 0;

    public Makale(String id, String title, int year, List<String> authors, List<String> referencedWorks) {
        this.id = id;
        this.title = title;
        this.year = year;
        if (authors != null) this.authors = authors;
        if (referencedWorks != null) this.referencedWorks = referencedWorks;
    }
    
    // Basit olması için getter/setter yerine public field kullandım ama toString lazım olabilir
    @Override
    public String toString() {
        return title + " (" + year + ")";
    }
}