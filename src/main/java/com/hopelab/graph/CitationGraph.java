package com.hopelab.graph;

import java.util.*;

public class CitationGraph {

    // idden article bul
    private final Map<String, Article> articles = new HashMap<>();

    //makale ekleme fonk.
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




    //verilen idde article varsa döndür yoksa oluştur döndür
    public Article ensureArticle(String id) {
        Article a = articles.get(id);
        if (a == null) {
            a = new Article(id);   //artcile construct
            addArticle(a);
        }
        return a;
    }


    //ok ekleme
    public void addEdge(Article from, Article to) {
        if (from == null || to == null) return;
        from.addOutNeighbor(to);
        to.addInNeighbor(from);
    }


}
