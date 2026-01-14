import com.hopelab.graph.model.Article;
import com.hopelab.graph.model.CitationGraph;
import com.hopelab.graph.service.GraphLoader;
import com.hopelab.graph.service.GraphMetrics;
import com.hopelab.graph.service.HIndexService;

import java.util.Scanner;

public class MainTest {
    public static void main(String[] args) {
        CitationGraph graph = GraphLoader.loadFromResource("articles.json");

        System.out.println("Node count   : " + graph.getArticles().size());
        System.out.println("Edge count   : " + calculateEdgeCount(graph));

        Scanner scanner = new Scanner(System.in);
        System.out.print("H-index hesaplanacak makale id: ");
        String id = scanner.nextLine();

        Article target = graph.getArticle(id);
        if (target == null) {
            System.out.println("Bu id'ye sahip makale bulunamadı.");
            return;
        }

        HIndexService.Result res = HIndexService.computeFor(target);
        System.out.println("h-index  : " + res.hIndex());
        System.out.println("h-median : " + res.hMedian());
        System.out.println("h-core makale sayısı: " + res.hCore().size());
        System.out.println("h-core:");
        for (Article a : res.hCore()) {
            System.out.println("  " + a.getId() + " (in=" + a.getInNeighbors().size() + ")");
        }
        // --- Tüm graf içinde en büyük h-index'i bulma (bunu zaten eklemiştik, kalsın) ---
        int maxH = -1;
        Article best = null;
        for (Article a : graph.getArticles()) {
            HIndexService.Result r = HIndexService.computeFor(a);
            if (r.hIndex() > maxH) {
                maxH = r.hIndex();
                best = a;
            }
        }

        System.out.println();
        System.out.println("Tüm graf içinde en büyük h-index: " + maxH);
        if (best != null) {
            System.out.println("Bu makale id: " + best.getId());
            System.out.println("Bu makalenin aldığı atıf sayısı: " + best.getInNeighbors().size());
        }

        // --- Betweenness centrality hesapla ve en yüksek 10 düğümü yazdır ---
        System.out.println();
        System.out.println("Betweenness centrality hesaplanıyor (biraz sürebilir)...");

        var bc = GraphMetrics.betweenness(graph);

        System.out.println("En yüksek betweenness skoruna sahip ilk 10 düğüm:");
        bc.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .forEach(e -> {
                    Article a = e.getKey();
                    double score = e.getValue();
                    System.out.println("  " + a.getId() + "  score=" + score);
                });

        // --- k-core: kullanıcıdan k al, k-core düğüm sayısını yazdır ---
        System.out.println();
        System.out.print("k-core için k değeri: ");
        String kStr = scanner.nextLine().trim();
        int k = 0;
        try {
            k = Integer.parseInt(kStr);
        } catch (NumberFormatException e) {
            System.out.println("Geçersiz k, 0 kabul edildi.");
        }
        var core = GraphMetrics.kCore(graph, k);
        System.out.println("k-core düğüm sayısı (degree >= " + k + "): " + core.size());

    }

    private static int calculateEdgeCount(CitationGraph graph) {
        int count = 0;
        for (Article a : graph.getArticles()) {
            count += a.getOutNeighbors().size();
        }
        return count;
    }
}