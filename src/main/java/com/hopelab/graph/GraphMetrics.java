package com.hopelab.graph;

import java.util.*;

/**
 * Graf analiz metrikleri: betweenness centrality ve k-core decomposition.
 */
public class GraphMetrics {

    /**
     * Grafı yönsüz kabul edip adjacency list çıkarıyoruz.
     * Hem outNeighbors hem de inNeighbors üzerinden bağlantı kuruyoruz.
     */
    private static Map<Article, List<Article>> buildUndirectedAdj(CitationGraph graph) {
        Map<Article, List<Article>> adj = new HashMap<>();
        for (Article a : graph.getArticles()) {
            adj.put(a, new ArrayList<>());
        }

        for (Article a : graph.getArticles()) {
            for (Article b : a.getOutNeighbors()) {
                // a -> b yönlü kenarı, yönsüz olarak a-b yap
                if (!adj.get(a).contains(b)) {
                    adj.get(a).add(b);
                }
                if (!adj.get(b).contains(a)) {
                    adj.get(b).add(a);
                }
            }
            for (Article b : a.getInNeighbors()) {
                // inNeighbors tarafı da kontrol edelim (ekstra güvenlik)
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

    public static Map<Article, Double> betweennessUndirectedSubgraph(
            CitationGraph graph,
            Set<Article> nodes
    ) {
        // Brandes algoritması (undirected)
        Map<Article, Double> CB = new HashMap<>();
        for (Article v : nodes) {
            CB.put(v, 0.0);
        }

        // Komşuluk listesi (yönsüz)
        Map<Article, List<Article>> neighbors = new HashMap<>();
        for (Article v : nodes) {
            List<Article> list = new ArrayList<>();
            for (Article w : v.getOutNeighbors()) {
                if (nodes.contains(w)) {
                    list.add(w);
                }
            }
            for (Article w : v.getInNeighbors()) {
                if (nodes.contains(w) && !list.contains(w)) {
                    list.add(w);
                }
            }
            neighbors.put(v, list);
        }

        for (Article s : nodes) {
            Stack<Article> S = new Stack<>();
            Map<Article, List<Article>> P = new HashMap<>();
            Map<Article, Integer> dist = new HashMap<>();
            Map<Article, Double> sigma = new HashMap<>();

            for (Article v : nodes) {
                P.put(v, new ArrayList<>());
                dist.put(v, -1);
                sigma.put(v, 0.0);
            }

            dist.put(s, 0);
            sigma.put(s, 1.0);

            Queue<Article> Q = new ArrayDeque<>();
            Q.add(s);

            while (!Q.isEmpty()) {
                Article v = Q.remove();
                S.push(v);
                for (Article w : neighbors.get(v)) {
                    if (dist.get(w) < 0) {
                        dist.put(w, dist.get(v) + 1);
                        Q.add(w);
                    }
                    if (dist.get(w).equals(dist.get(v) + 1)) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        P.get(w).add(v);
                    }
                }
            }

            Map<Article, Double> delta = new HashMap<>();
            for (Article v : nodes) {
                delta.put(v, 0.0);
            }

            while (!S.isEmpty()) {
                Article w = S.pop();
                for (Article v : P.get(w)) {
                    double c = (sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(s)) {
                    CB.put(w, CB.get(w) + delta.get(w));
                }
            }
        }

        // undirected graf için çift sayımı önlemek için 2'ye böl
        for (Article v : nodes) {
            CB.put(v, CB.get(v) / 2.0);
        }

        return CB;
    }


    /**
     * Brandes algoritmasının sade haliyle betweenness centrality.
     * Sonuç: her Article için betweenness skor map'i.
     */
    public static Map<Article, Double> betweenness(CitationGraph graph) {
        List<Article> nodes = new ArrayList<>(graph.getArticles());
        Map<Article, Double> bc = new HashMap<>();
        for (Article v : nodes) {
            bc.put(v, 0.0);
        }

        Map<Article, List<Article>> adj = buildUndirectedAdj(graph);

        for (Article s : nodes) {
            Deque<Article> stack = new ArrayDeque<>();
            Map<Article, List<Article>> pred = new HashMap<>();
            Map<Article, Integer> dist = new HashMap<>();
            Map<Article, Integer> sigma = new HashMap<>();

            for (Article v : nodes) {
                pred.put(v, new ArrayList<>());
                dist.put(v, -1);
                sigma.put(v, 0);
            }

            dist.put(s, 0);
            sigma.put(s, 1);

            Deque<Article> queue = new ArrayDeque<>();
            queue.add(s);

            // BFS ile en kısa yolları bul
            while (!queue.isEmpty()) {
                Article v = queue.removeFirst();
                stack.push(v);
                for (Article w : adj.get(v)) {
                    // keşfedilmemiş düğüm
                    if (dist.get(w) < 0) {
                        dist.put(w, dist.get(v) + 1);
                        queue.add(w);
                    }
                    // en kısa yollardan biri
                    if (dist.get(w) == dist.get(v) + 1) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        pred.get(w).add(v);
                    }
                }
            }

            Map<Article, Double> delta = new HashMap<>();
            for (Article v : nodes) {
                delta.put(v, 0.0);
            }

            // Geriye doğru geçip katkıları topla
            while (!stack.isEmpty()) {
                Article w = stack.pop();
                for (Article v : pred.get(w)) {
                    if (sigma.get(w) != 0) {
                        double c = ((double) sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                        delta.put(v, delta.get(v) + c);
                    }
                }
                if (w != s) {
                    bc.put(w, bc.get(w) + delta.get(w));
                }
            }
        }

        return bc;
    }

    /**
     * k-core decomposition.
     * Dönüş: k-core alt grafında kalan düğümler kümesi.
     */
    public static Set<Article> kCore(CitationGraph graph, int k) {
        List<Article> nodes = new ArrayList<>(graph.getArticles());
        Map<Article, List<Article>> adj = buildUndirectedAdj(graph);

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
            if (removed.contains(v)) continue;
            removed.add(v);

            for (Article w : adj.get(v)) {
                if (!removed.contains(w)) {
                    degree.put(w, degree.get(w) - 1);
                    if (degree.get(w) == k - 1) {
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


}
