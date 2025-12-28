package com.hopelab.graph;

import java.util.*;

public class CitationGraph {

    // id -> Article
    private final Map<String, Article> articles = new HashMap<>();

    // ----------------------------------------------------------------
    // Makale ekleme / erişim
    // ----------------------------------------------------------------

    public void addArticle(Article article) {
        if (article == null) return;
        articles.put(article.getId(), article);
    }

    public Article getArticle(String id) {
        return articles.get(id);
    }

    public Collection<Article> getArticles() {
        return articles.values();
    }

    public int getNodeCount() {
        return articles.size();
    }

    // ----------------------------------------------------------------
    // GraphLoader için yardımcılar
    // ----------------------------------------------------------------

    /**
     * Verilen id'ye sahip Article'ı döndürür.
     * Yoksa yeni bir Article oluşturur, grafa ekler ve onu döndürür.
     */
    public Article ensureArticle(String id) {
        Article a = articles.get(id);
        if (a == null) {
            a = new Article(id);   // Article(String id) ctor'unu kullandık
            addArticle(a);
        }
        return a;
    }

    /**
     * İki Article arasına yönlü kenar ekler (from -> to).
     * Article içindeki komşu listelerini de günceller.
     */
    public void addEdge(Article from, Article to) {
        if (from == null || to == null) return;
        from.addOutNeighbor(to);
        to.addInNeighbor(from);
    }

    // Eski API'n de kalsın, içerden yeni metodu kullanalım:
    public void addDirectedEdge(String fromId, String toId) {
        Article from = articles.get(fromId);
        Article to   = articles.get(toId);
        if (from == null || to == null) return; // JSON'da olmayan referans olabilir
        addEdge(from, to);
    }

    // ----------------------------------------------------------------
    // Kenar / derece istatistikleri
    // ----------------------------------------------------------------

    public int getEdgeCount() {
        int sum = 0;
        for (Article a : articles.values()) {
            sum += a.getOutNeighbors().size();
        }
        return sum;
    }

    public int getInDegree(Article a) {
        return a.getInNeighbors().size();
    }

    public int getOutDegree(Article a) {
        return a.getOutNeighbors().size();
    }

    public int getTotalGivenReferences() {
        int sum = 0;
        for (Article a : articles.values()) {
            sum += a.getOutNeighbors().size();
        }
        return sum;
    }

    public int getTotalReceivedReferences() {
        int sum = 0;
        for (Article a : articles.values()) {
            sum += a.getInNeighbors().size();
        }
        return sum;
    }

    public Article getMostCitedArticle() {
        Article best = null;
        int bestIn = -1;
        for (Article a : articles.values()) {
            int in = a.getInNeighbors().size();
            if (in > bestIn) {
                bestIn = in;
                best = a;
            }
        }
        return best;
    }

    public Article getMostCitingArticle() {
        Article best = null;
        int bestOut = -1;
        for (Article a : articles.values()) {
            int out = a.getOutNeighbors().size();
            if (out > bestOut) {
                bestOut = out;
                best = a;
            }
        }
        return best;
    }
}
