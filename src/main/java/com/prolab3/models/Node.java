package com.prolab3.models;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class Node {
    public Makale article;
    public List<Edge> incomingEdges = new ArrayList<>();
    public List<Edge> outgoingEdges = new ArrayList<>();
    
    // Analiz Sonuçları
    public int hIndex = 0;
    public double betweenness = 0.0;
    
    // Görselleştirme
    public double x;
    public double y;
    public Color color = Color.BLUE; // Varsayılan renk
    public double radius = 5.0;      // Varsayılan boyut

    public Node(Makale article) {
        this.article = article;
        // Rastgele dağıt ama çok kenarlara gelmesin
        this.x = 50 + Math.random() * 700;
        this.y = 50 + Math.random() * 500;
    }
}
