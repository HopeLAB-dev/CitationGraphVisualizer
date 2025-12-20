package com.prolab3.algorithms;

import com.prolab3.models.Graph;
import com.prolab3.models.Node;
import com.prolab3.models.Edge;
import java.util.*;

public class BasitAlgoritmalar {

    // --- FAZ 3: H-INDEX HESAPLAMA ---
    public static int hIndexHesapla(Node node, Graph graph) {
        // H-Index tanımı: Bu makaleye atıf yapanlar içinden (incoming edges)
        // en az h atıfa sahip h tane makale olmalı.
        
        List<Integer> citationCounts = new ArrayList<>();
        
        // Bize atıf yapanları bul (incoming edges)
        for (Edge e : node.incomingEdges) {
            Node referansVeren = e.source;
            citationCounts.add(referansVeren.article.citationCount);
        }
        
        // Büyükten küçüğe sırala
        Collections.sort(citationCounts, Collections.reverseOrder());
        
        int h = 0;
        for (int i = 0; i < citationCounts.size(); i++) {
            // Liste indexi 0'dan başlar, sıra sayısı i+1'dir.
            // Atıf sayısı >= sıra sayısı olmalı
            if (citationCounts.get(i) >= (i + 1)) {
                h = i + 1;
            } else {
                break;
            }
        }
        return h;
    }

    // --- FAZ 4.2: BETWEENNESS CENTRALITY ---
    // Yönsüz graf varsayımıyla çalışır (İsterlerde öyle diyor)
    public static void betweennessHesapla(Graph graph) {
        // Önce temizle
        for (Node n : graph.nodes.values()) n.betweenness = 0.0;

        List<Node> nodes = new ArrayList<>(graph.nodes.values());
        
        // Her düğüm çifti için en kısa yolu bul (BFS)
        // Bu çok kaba kuvvet bir yöntem (Brute Force) ama basittir.
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node start = nodes.get(i);
                Node end = nodes.get(j);
                
                // Start'tan End'e giden en kısa yolları bul
                List<Node> path = findShortestPath(start, end, graph);
                
                // Yoldaki ara düğümlerin puanını arttır
                if (path != null) {
                    for (Node n : path) {
                        if (n != start && n != end) {
                            n.betweenness++;
                        }
                    }
                }
            }
        }
    }

    // Basit BFS ile en kısa yol bulma (sadece BİR tane yol bulur, amatörce)
    private static List<Node> findShortestPath(Node start, Node end, Graph graph) {
        Queue<List<Node>> queue = new LinkedList<>();
        List<Node> initialPath = new ArrayList<>();
        initialPath.add(start);
        queue.add(initialPath);
        
        Set<Node> visited = new HashSet<>();
        visited.add(start);

        while (!queue.isEmpty()) {
            List<Node> path = queue.poll();
            Node lastNode = path.get(path.size() - 1);

            if (lastNode == end) {
                return path;
            }

            // Komşuları bul (Yönsüz dediği için hem incoming hem outgoing bakıyoruz)
            Set<Node> neighbors = new HashSet<>();
            for (Edge e : lastNode.outgoingEdges) neighbors.add(e.target);
            for (Edge e : lastNode.incomingEdges) neighbors.add(e.source);

            for (Node neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<Node> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        return null;
    }

    // --- FAZ 4.3: K-CORE DECOMPOSITION ---
    // Kopyalama yapmadan direkt graf üzerinde işaretleme yapalım
    // Silinenleri listeden çıkarmak yerine "invisible" (görünmez) yapabiliriz veya silebiliriz.
    // Burada kopya graf dönmek daha güvenli.
    public static List<String> kCoreBul(Graph originalGraph, int k) {
        // Silinmemesi gereken (K-Core içinde kalan) ID'leri döndürelim
        
        // Geçici bir "derece" map'i tutalım
        Map<String, Integer> degrees = new HashMap<>();
        Map<String, Boolean> removed = new HashMap<>();
        
        // Başlangıç dereceleri (Yönsüz kabul ederek: Giren + Çıkan)
        // Veya sadece toplam bağlantı sayısı
        for (Node n : originalGraph.nodes.values()) {
            // Yönsüz graf mantığı: Unique komşu sayısı
            Set<String> neighborIds = new HashSet<>();
            for (Edge e : n.outgoingEdges) neighborIds.add(e.target.article.id);
            for (Edge e : n.incomingEdges) neighborIds.add(e.source.article.id);
            
            degrees.put(n.article.id, neighborIds.size());
            removed.put(n.article.id, false);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            // Derecesi K'dan küçük olan ve henüz silinmemişleri bul
            List<String> toRemove = new ArrayList<>();
            
            for (String id : degrees.keySet()) {
                if (!removed.get(id) && degrees.get(id) < k) {
                    toRemove.add(id);
                }
            }
            
            // Bulunanları sil ve komşuların derecesini düşür
            for (String id : toRemove) {
                removed.put(id, true);
                changed = true;
                
                Node n = originalGraph.nodes.get(id);
                // Komşuları bul
                Set<Node> neighbors = new HashSet<>();
                for (Edge e : n.outgoingEdges) neighbors.add(e.target);
                for (Edge e : n.incomingEdges) neighbors.add(e.source);
                
                for (Node neighbor : neighbors) {
                    if (!removed.get(neighbor.article.id)) {
                        int currentDeg = degrees.get(neighbor.article.id);
                        degrees.put(neighbor.article.id, currentDeg - 1);
                    }
                }
            }
        }

        List<String> survivorIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : removed.entrySet()) {
            if (!entry.getValue()) {
                survivorIds.add(entry.getKey());
            }
        }
        return survivorIds;
    }
}
