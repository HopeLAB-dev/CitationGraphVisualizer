package com.prolab3.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    // ID ile makaleye hızlı ulaşmak için Map kullanıyoruz
    public Map<String, Node> nodes = new HashMap<>();
    public List<Edge> edges = new ArrayList<>();

    public void addNode(Node node) {
        nodes.put(node.article.id, node);
    }

    public void addEdge(String sourceId, String targetId) {
        Node source = nodes.get(sourceId);
        Node target = nodes.get(targetId);
        
        if (source != null && target != null) {
            Edge edge = new Edge(source, target);
            edges.add(edge);
            source.outgoingEdges.add(edge);
            target.incomingEdges.add(edge);
            
            // Atıf sayısını da burada arttıralım
            target.article.citationCount++;
        }
    }
}
