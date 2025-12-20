package com.prolab3.algorithms;

import com.prolab3.models.Graph;
import com.prolab3.models.Node;
import java.util.*;

public class GraphAlgorithms {

    // --- H-INDEX & H-CORE ---
    
    public static int calculateHIndex(Node node) {
        // Atıf yapanların (incoming) atıf sayılarını al
        List<Integer> citations = new ArrayList<>();
        for (Node citing : node.incomingEdges) {
            citations.add(citing.citationCount);
        }
        
        // Büyükten küçüğe sırala
        citations.sort(Collections.reverseOrder());
        
        int h = 0;
        for (int i = 0; i < citations.size(); i++) {
            if (citations.get(i) >= (i + 1)) {
                h = i + 1;
            } else {
                break;
            }
        }
        return h;
    }
    
    // H-Core kümesini döndürür (Node listesi)
    public static List<Node> getHCore(Node node) {
        int h = calculateHIndex(node);
        List<Node> hCore = new ArrayList<>();
        
        // Atıf yapanları sıralı alalım
        List<Node> citingNodes = new ArrayList<>(node.incomingEdges);
        citingNodes.sort((a, b) -> Integer.compare(b.citationCount, a.citationCount)); // Descending
        
        // İlk h tanesi H-Core'dur
        for (int i = 0; i < h && i < citingNodes.size(); i++) {
            hCore.add(citingNodes.get(i));
        }
        return hCore;
    }
    
    public static double calculateHMedian(List<Node> hCore) {
        if (hCore.isEmpty()) return 0;
        List<Integer> counts = new ArrayList<>();
        for(Node n : hCore) counts.add(n.citationCount);
        Collections.sort(counts);
        
        int size = counts.size();
        if (size % 2 == 1) {
            return counts.get(size / 2);
        } else {
            return (counts.get(size / 2 - 1) + counts.get(size / 2)) / 2.0;
        }
    }

    // --- BETWEENNESS CENTRALITY (YÖNSÜZ) ---
    
    public static void calculateBetweenness(Graph graph) {
        // 1. Grafı yönsüz mantığa göre hazırla (Adjacency List for Undirected)
        Map<String, List<String>> adj = new HashMap<>();
        List<String> allIds = new ArrayList<>(graph.nodes.keySet());
        
        for (String id : allIds) adj.put(id, new ArrayList<>());
        
        // Varolan kenarları yönsüz olarak ekle (A->B ise A-B bağlantısı)
        for (Node u : graph.nodes.values()) {
            for (Node v : u.outgoingEdges) {
                if (!adj.get(u.id).contains(v.id)) adj.get(u.id).add(v.id);
                if (!adj.get(v.id).contains(u.id)) adj.get(v.id).add(u.id);
            }
        }
        
        // Skorları sıfırla
        for(Node n : graph.nodes.values()) n.betweenness = 0.0;
        
        // Her düğüm için Brandes algoritması veya Basit BFS (kısa yollar)
        // PDF basit anlatım içeriyor: "Tüm ikililer için en kısa yolları bul, üzerinden geçeni say"
        // Bu O(N^3) olabilir. Binlerce makale için optimize edilmeli ama Java kaldırır.
        
        // Daha hızlı yöntem (Unweighted BFS for all pairs):
        for (String sId : allIds) {
            // BFS başlat
            Map<String, List<String>> prev = new HashMap<>(); // Predecessors
            Map<String, Integer> dist = new HashMap<>();
            Map<String, Double> sigma = new HashMap<>(); // Number of shortest paths
            
            for(String id : allIds) {
                dist.put(id, -1);
                sigma.put(id, 0.0);
                prev.put(id, new ArrayList<>());
            }
            
            dist.put(sId, 0);
            sigma.put(sId, 1.0);
            Queue<String> q = new LinkedList<>();
            q.add(sId);
            Stack<String> stack = new Stack<>();
            
            while (!q.isEmpty()) {
                String u = q.poll();
                stack.push(u);
                
                for (String v : adj.get(u)) {
                    if (dist.get(v) == -1) {
                        dist.put(v, dist.get(u) + 1);
                        q.add(v);
                    }
                    if (dist.get(v) == dist.get(u) + 1) {
                        sigma.put(v, sigma.get(v) + sigma.get(u));
                        prev.get(v).add(u);
                    }
                }
            }
            
            // Dependency accumulation (Brandes)
            Map<String, Double> delta = new HashMap<>();
            for(String id : allIds) delta.put(id, 0.0);
            
            while (!stack.isEmpty()) {
                String w = stack.pop();
                for (String v : prev.get(w)) {
                    double c = (sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(sId)) {
                    // Yönsüz olduğu için 2'ye bölmek gerekir (s->t ve t->s aynı yol)
                    // Ama göreli büyüklük yeterliyse bölmeye gerek yok. 
                    // Standart formülde finalde 2'ye bölünür.
                    graph.nodes.get(w).betweenness += delta.get(w);
                }
            }
        }
        
        // Sonuçları 2'ye böl (Undirected graph adjustment)
        for(Node n : graph.nodes.values()) {
            n.betweenness /= 2.0;
        }
    }

    // --- K-CORE DECOMPOSITION ---
    
    // Geriye K-Core içinde kalan düğüm ID'lerinin listesini döner
    public static List<String> calculateKCore(Graph graph, int k) {
        // Grafın kopyası üzerinde çalış (Nodes ve dereceleri)
        Map<String, Integer> degrees = new HashMap<>();
        Map<String, Boolean> removed = new HashMap<>();
        
        // Derece hesapla (Yönsüz mantık: unique komşu sayısı)
        Map<String, List<String>> neighbors = new HashMap<>();
        
        for(Node n : graph.nodes.values()) {
            Set<String> uniqueNeighbors = new HashSet<>();
            for(Node out : n.outgoingEdges) uniqueNeighbors.add(out.id);
            for(Node in : n.incomingEdges) uniqueNeighbors.add(in.id);
            
            neighbors.put(n.id, new ArrayList<>(uniqueNeighbors));
            degrees.put(n.id, uniqueNeighbors.size());
            removed.put(n.id, false);
        }
        
        boolean changed = true;
        while(changed) {
            changed = false;
            List<String> toRemove = new ArrayList<>();
            
            // Silinecekleri belirle
            for(String id : degrees.keySet()) {
                if(!removed.get(id) && degrees.get(id) < k) {
                    toRemove.add(id);
                }
            }
            
            // Sil ve komşuları güncelle
            if(!toRemove.isEmpty()) {
                changed = true;
                for(String id : toRemove) {
                    removed.put(id, true);
                    // Komşuların derecesini düşür
                    for(String neighborId : neighbors.get(id)) {
                        if(!removed.get(neighborId)) {
                            degrees.put(neighborId, degrees.get(neighborId) - 1);
                        }
                    }
                }
            }
        }
        
        List<String> survivorIds = new ArrayList<>();
        for(Map.Entry<String, Boolean> entry : removed.entrySet()) {
            if(!entry.getValue()) survivorIds.add(entry.getKey());
        }
        return survivorIds;
    }
}
