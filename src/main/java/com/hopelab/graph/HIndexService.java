package com.hopelab.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HIndexService {

    public static class Result {
        public final int hIndex;
        public final List<Article> hCore;
        public final int hMedian;

        public Result(int hIndex, List<Article> hCore, int hMedian) {
            this.hIndex = hIndex;
            this.hCore = hCore;
            this.hMedian = hMedian;
        }
    }

    public static Result computeFor(Article target) {
        // 1) Bu makaleye atıf yapanlar:
        List<Article> citing = new ArrayList<>(target.getInNeighbors());

        // Atıf yapan makale yoksa her şey 0
        if (citing.isEmpty()) {
            return new Result(0, List.of(), 0);
        }

        // 2) Bu makalelerin HER BİRİNİN aldığı atıf sayısı
        citing.sort((a, b) -> Integer.compare(
                b.getInNeighbors().size(),
                a.getInNeighbors().size()
        ));

        List<Integer> counts = new ArrayList<>();
        for (Article a : citing) {
            counts.add(a.getInNeighbors().size());
        }

        // 3) h-index hesapla
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

        // 4) h-core = ilk h makale
        List<Article> hCore = new ArrayList<>();
        for (int i = 0; i < h && i < citing.size(); i++) {
            hCore.add(citing.get(i));
        }

        // 5) h-median = h-core içindeki atıf sayılarının ortancası
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
                // çift sayıda ise, ortadaki iki değerden büyük olanı al
                hMedian = hCoreCounts.get(n / 2);
            }
        }

        return new Result(h, hCore, hMedian);
    }
}
