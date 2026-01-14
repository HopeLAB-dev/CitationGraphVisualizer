package com.hopelab.graph.service;

import com.hopelab.graph.model.Article;
import com.hopelab.graph.model.CitationGraph;

import java.util.*;

// GRAF ANALİZ YONTEMLERİ & ALGORITMALAR

public final class GraphMetrics {

    private GraphMetrics() {

    }

    //grafı yönsüzleştirip düğümlerarası bağ
    private static Map<Article, List<Article>> buildUndirectedAdjacency(CitationGraph graph) {
        Map<Article, List<Article>> adj = new HashMap<>();

        // düğümler için yeni liste
        for (Article a : graph.getArticles()) {
            adj.put(a, new ArrayList<>());
        }

        //okları yönsüz kenar yap
        for (Article a : graph.getArticles()) {


            for (Article b : a.getOutNeighbors()) {
                if (!adj.get(a).contains(b)) {
                    adj.get(a).add(b);
                }
                if (!adj.get(b).contains(a)) {
                    adj.get(b).add(a);
                }
            }
            //alınan atıf tekrar kontrol
            for (Article b : a.getInNeighbors()) {
                if (!adj.get(a).contains(b)) {
                    adj.get(a).add(b);
                }
                if (!adj.get(b).contains(a)) {
                    adj.get(b).add(a);
                }
            }
        }

        return adj;
    }

    //kcore hesapları
    public static Set<Article> kCore(CitationGraph graph, int k) {
        List<Article> nodes = new ArrayList<>(graph.getArticles());
        Map<Article, List<Article>> adj = buildUndirectedAdjacency(graph);

        Map<Article, Integer> degree = new HashMap<>();
        for (Article v : nodes) {
            degree.put(v, adj.get(v).size());
        }

        Deque<Article> queue = new ArrayDeque<>();
        for (Article v : nodes) {
            if (degree.get(v) < k) {
                queue.add(v);
            }
        }

        Set<Article> removed = new HashSet<>();

        while (!queue.isEmpty()) {
            Article v = queue.removeFirst();
            if (removed.contains(v)) {
                continue;
            }
            removed.add(v);

            for (Article w : adj.get(v)) {
                if (!removed.contains(w)) {
                    int newDeg = degree.get(w) - 1;
                    degree.put(w, newDeg);
                    if (newDeg == k - 1) {
                        queue.add(w);
                    }
                }
            }
        }

        Set<Article> core = new HashSet<>();
        for (Article v : nodes) {
            if (!removed.contains(v)) {
                core.add(v);
            }
        }

        return core;
    }


    //yönsüz adajcemcy list
    private static Map<Article, List<Article>> buildUndirectedAdjacencyForSubset(
            Set<Article> subset
    ) {
        Map<Article, List<Article>> adj = new HashMap<>();
        Set<Article> nodeSet = new HashSet<>(subset);

        for (Article a : nodeSet) {
            adj.put(a, new ArrayList<>());
        }

        for (Article a : nodeSet) {
            // out neighbors
            for (Article b : a.getOutNeighbors()) {
                if (!nodeSet.contains(b)) continue;
                List<Article> la = adj.get(a);
                List<Article> lb = adj.get(b);
                if (!la.contains(b)) la.add(b);
                if (!lb.contains(a)) lb.add(a);
            }
            // in neighbors (yine yönsüz kabul ediyoruz)
            for (Article b : a.getInNeighbors()) {
                if (!nodeSet.contains(b)) continue;
                List<Article> la = adj.get(a);
                List<Article> lb = adj.get(b);
                if (!la.contains(b)) la.add(b);
                if (!lb.contains(a)) lb.add(a);
            }
        }

        return adj;
    }

    //tamsayılı betweennes hesabı
    public static Map<Article, Integer> integerBetweenness(
            CitationGraph graph,
            Set<Article> subset
    ) {
        List<Article> nodes = new ArrayList<>(subset);
        int n = nodes.size();

        Map<Article, Integer> result = new HashMap<>();
        for (Article a : nodes) {
            result.put(a, 0);
        }

        if (n == 0) return result;

        Map<Article, List<Article>> adj = buildUndirectedAdjacencyForSubset(subset);

        //her node için BFS
        for (int i = 0; i < n; i++) {
            Article s = nodes.get(i);

            //BFS ICIN parent hashmapi
            Map<Article, Article> parent = new HashMap<>();
            ArrayDeque<Article> queue = new ArrayDeque<>();

            parent.put(s, null);
            queue.add(s);

            while (!queue.isEmpty()) {
                Article u = queue.removeFirst();
                for (Article v : adj.getOrDefault(u, Collections.emptyList())) {
                    if (!parent.containsKey(v)) {
                        parent.put(v, u);
                        queue.add(v);
                    }
                }
            }

            // s (kaynaktan) geriye doğru yürüt yolu
            for (int j = i + 1; j < n; j++) {
                Article t = nodes.get(j);
                if (!parent.containsKey(t)) {
                    //yol yoksa
                    continue;
                }

                Article cur = parent.get(t);
                while (cur != null && cur != s) {
                    //ilk ve son dugum arası kısayoldaki her dugumö +1 puan alsın
                    result.put(cur, result.get(cur) + 1);
                    cur = parent.get(cur);
                }
            }
        }

        return result;
    }
    
    // --- YENİ EKLENEN PUBLIC BETWEENNESS WRAPPER ---
    // Test sınıfı "betweenness(graph)" diye bir şey çağırmaya çalışıyor ama
    // mevcut kodda sadece "integerBetweenness(graph, subset)" var.
    // Bu uyumsuzluğu gidermek için tüm graf üzerinde çalışan bir wrapper ekliyoruz.
    public static Map<Article, Double> betweenness(CitationGraph graph) {
        // Tüm makaleleri al
        Set<Article> allArticles = new HashSet<>(graph.getArticles());
        
        // Integer hesap yapan mevcut fonksiyonu kullan
        Map<Article, Integer> intResult = integerBetweenness(graph, allArticles);
        
        // Sonucu Double'a çevir (Test sınıfı double bekliyorsa uyum sağlar)
        Map<Article, Double> doubleResult = new HashMap<>();
        for (Map.Entry<Article, Integer> entry : intResult.entrySet()) {
            doubleResult.put(entry.getKey(), entry.getValue().doubleValue());
        }
        return doubleResult;
    }
}