package com.hopelab.graph.service;

import com.hopelab.graph.model.Article;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HIndexService {

    public record Result(int hIndex, List<Article> hCore, int hMedian) {
    }

    public static Result computeFor(Article target) {
        // parametredeki makaleye atıf yapanların arraylist
        List<Article> citing = new ArrayList<>(target.getInNeighbors());

        // yoksa tüm sonuclar 0
        if (citing.isEmpty()) {
            return new Result(0, List.of(), 0);
        }

        //herbirini n aldıgı atıf sayısı
        citing.sort((a, b) -> Integer.compare(
                b.getInNeighbors().size(),
                a.getInNeighbors().size()
        ));

        List<Integer> counts = new ArrayList<>();
        for (Article a : citing) {
            counts.add(a.getInNeighbors().size());
        }

        // h-index hesapla
        int h = 0;
        for (int i = 0; i < counts.size(); i++) {
            int c = counts.get(i);
            int rank = i + 1;
            if (c >= rank) {
                h = rank;
            } else {
                break;
            }
        }

        // h-core hesapla
        List<Article> hCore = new ArrayList<>();
        for (int i = 0; i < h && i < citing.size(); i++) {
            hCore.add(citing.get(i));
        }

        // h-median yani h-core içindeki atıf sayılarının ortancasını hesapla
        int hMedian = 0;
        if (!hCore.isEmpty()) {
            List<Integer> hCoreCounts = new ArrayList<>();
            for (Article a : hCore) {
                hCoreCounts.add(a.getInNeighbors().size());
            }
            hCoreCounts.sort(Comparator.naturalOrder());
            int n = hCoreCounts.size();
            if (n % 2 == 1) {
                hMedian = hCoreCounts.get(n / 2);
            } else {
                // çift sayıysa buyuk olanı al
                hMedian = hCoreCounts.get(n / 2);
            }
        }
        //sonucları dondr
        return new Result(h, hCore, hMedian);
    }
}