package com.prolab3.models;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class Node {
    // PDF isterleri: id, authors, title, year, citationCount
    public String id;
    public String title;
    public List<String> authors;
    public int year;
    public int citationCount;

    // Graf Yapısı
    public List<Node> outgoingEdges = new ArrayList<>(); // Referans verdikleri
    public List<Node> incomingEdges = new ArrayList<>(); // Atıf yapanlar
    public Node nextById; // Yeşil kenar için (ID sırasına göre bir sonraki)

    // Analiz Sonuçları
    public int hIndex = 0;
    public double betweenness = 0.0;
    
    // Görselleştirme
    public double x, y;
    public Color color = Color.LIGHTBLUE; 
    public double radius = 15.0;

    public Node(String id, String title, List<String> authors, int year) {
        this.id = id;
        this.title = title;
        this.authors = authors != null ? authors : new ArrayList<>();
        this.year = year;
        this.citationCount = 0;
        
        // Rastgele başlangıç konumu
        this.x = Math.random() * 800;
        this.y = Math.random() * 600;
    }
    
    public void addOutgoing(Node target) {
        if (!outgoingEdges.contains(target)) {
            outgoingEdges.add(target);
        }
    }
    
    public void addIncoming(Node source) {
        if (!incomingEdges.contains(source)) {
            incomingEdges.add(source);
            citationCount++;
        }
    }

    @Override
    public String toString() {
        return title;
    }
}
