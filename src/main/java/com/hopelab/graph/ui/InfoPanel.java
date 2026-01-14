package com.hopelab.graph.ui;

import com.hopelab.graph.model.Article;
import com.hopelab.graph.model.CitationGraph;
import com.hopelab.graph.service.HIndexService;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.*;

public class InfoPanel {

    private final CitationGraph graph;

    private GraphView graphView;

    private final BorderPane root;
    private final Label globalLabel;
    private final TextArea selectedArea;
    private final Button removeButton;

    //analiz kutu
    private final VBox analysisBox;
    private final TextField kField;
    private final Label kInfoLabel;

    public InfoPanel(CitationGraph graph) {
        this.graph = graph;

        root = new BorderPane();
        root.setPrefWidth(320);
        root.setStyle("-fx-background-color: #1e1e1e;");

        //üsstteki genel istatikler
        globalLabel = new Label();
        globalLabel.setStyle("-fx-text-fill: white;");
        globalLabel.setWrapText(true);

        VBox topBox = new VBox(10, globalLabel);
        topBox.setPadding(new Insets(10));
        root.setTop(topBox);

        //secili makale detayları ve kaldırma butonu
        selectedArea = new TextArea();
        selectedArea.setEditable(false);
        selectedArea.setWrapText(true);
        selectedArea.setStyle(
                "-fx-control-inner-background: #282828;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12;"
        );

        removeButton = new Button("Seçili düğümü kaldır");
        removeButton.setDisable(true);
        removeButton.setOnAction(e -> {
            if (graphView != null) {
                graphView.removeSelected();
            }
        });

        VBox centerBox = new VBox(5, selectedArea, removeButton);
        centerBox.setPadding(new Insets(10));
        root.setCenter(centerBox);

        // id ile bulma ve onun butonu
        TextField idField = new TextField();
        idField.setPromptText("Makale ID veya OpenAlex linki");

        Button idButton = new Button("✓ ID ile göster");
        idButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        Label idInfoLabel = new Label();
        idInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        idButton.setOnAction(e -> {
            if (graphView == null) return;

            String raw = idField.getText().trim();
            if (raw.isEmpty()) {
                idInfoLabel.setText("ID alanı boş olamaz.");
                return;
            }

            boolean ok = graphView.focusArticleById(raw);
            if (ok) {
                idInfoLabel.setText("Makale yüklendi ve seçildi.");
                graphView.setLabelsEnabled(true);
            } else {
                idInfoLabel.setText("Bu ID'ye sahip makale bulunamadı.");
            }
        });

        // kcore icin analiz butonu
        kField = new TextField();
        kField.setPromptText("k-core için k değeri");

        Button kButton = new Button("k-core göster");
        kInfoLabel = new Label();
        kInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        kButton.setOnAction(e -> {
            if (graphView == null) return;

            String text = kField.getText().trim();
            int k;
            try {
                k = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                kInfoLabel.setText("Geçersiz k değeri.");
                return;
            }

            graphView.computeKCoreOnVisible(k);
            kInfoLabel.setText("k-core düğüm sayısı görünür graf üzerinde hesaplandı.");
        });

        analysisBox = new VBox(5,
                new Separator(),
                new Label("Analiz Modu"),
                kField, kButton, kInfoLabel
        );
        analysisBox.setPadding(new Insets(10, 0, 0, 0));
        analysisBox.setVisible(false);
        analysisBox.setManaged(false);

        // analiz moduna gir
        Button analysisButton = new Button("Analiz Uygulamaları");
        analysisButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        analysisButton.setOnAction(e -> {
            if (graphView == null) return;
            graphView.enterAnalysisMode();
            analysisBox.setVisible(true);
            analysisBox.setManaged(true);
        });

        //GÖRÜNÜMÜ SIFIRLAMA TUŞU
        Button resetButton = new Button("Görünümü sıfırla");
        resetButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        resetButton.setOnAction(e -> {
            if (graphView != null) {
                graphView.resetView();
            }
            analysisBox.setVisible(false);
            analysisBox.setManaged(false);
        });

        VBox bottomBox = new VBox(8,
                idField, idButton, idInfoLabel,
                analysisButton,
                resetButton,
                analysisBox
        );
        bottomBox.setPadding(new Insets(10));
        root.setBottom(bottomBox);

        updateGlobalStats(new HashSet<>(graph.getArticles()));
    }

    //normal modda graf istatistikleri
    public void updateGlobalStats(Set<Article> visibleNodes) {
        if (visibleNodes == null || visibleNodes.isEmpty()) {
            globalLabel.setText("Genel İstatistikler\n-------------------\nGraf boş.");
            return;
        }

        int nodeCount = visibleNodes.size();
        int edgeCount = 0;
        int totalGiven = 0;
        int totalReceived = 0;

        Article mostCited = null;
        int maxIn = -1;
        Article mostCiting = null;
        int maxOut = -1;

        for (Article a : visibleNodes) {
            int outVisible = 0;
            for (Article b : a.getOutNeighbors()) {
                if (visibleNodes.contains(b)) {
                    outVisible++;
                    edgeCount++;
                }
            }

            int inVisible = 0;
            for (Article b : a.getInNeighbors()) {
                if (visibleNodes.contains(b)) {
                    inVisible++;
                }
            }

            totalGiven += outVisible;
            totalReceived += inVisible;

            if (inVisible > maxIn) {
                maxIn = inVisible;
                mostCited = a;
            }
            if (outVisible > maxOut) {
                maxOut = outVisible;
                mostCiting = a;
            }
        }

        if (maxIn < 0) maxIn = 0;
        if (maxOut < 0) maxOut = 0;

        String text = """
                Genel İstatistikler (şu anki görünüm)
                --------------------------------------
                Toplam makale (düğüm): %d
                Toplam referans (beyaz kenar): %d
                Toplam verilen referans: %d
                Toplam alınan referans: %d

                En çok atıf alan (görünürde):
                %s (%d)

                En çok atıf veren (görünürde):
                %s (%d)
                """.formatted(
                nodeCount,
                edgeCount,
                totalGiven,
                totalReceived,
                mostCited != null ? mostCited.getId() : "-",
                maxIn,
                mostCiting != null ? mostCiting.getId() : "-",
                maxOut
        );

        globalLabel.setText(text);
    }

    // analiz modunda yukarıdaki degerlerin yerini betweennes sonucları alıyor
    public void showBetweennessResultsInteger(Set<Article> visibleNodes,
                                              Map<Article, Integer> betMap) {
        if (visibleNodes == null || visibleNodes.isEmpty()) {
            globalLabel.setText("Betweenness Centrality\n-------------------\nGraf boş.");
            return;
        }

        List<Map.Entry<Article, Integer>> list =
                new ArrayList<>(betMap.entrySet());
        //ilk 10 azalan sırala
        list.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("Betweenness Centrality\n");
        sb.append("--------------------------------------\n");
        sb.append("Düğüm sayısı: ").append(visibleNodes.size()).append("\n\n");
        sb.append("İlk 10 düğüm:\n");

        int limit = Math.min(10, list.size());
        for (int i = 0; i < limit; i++) {
            var e = list.get(i);
            Article a = e.getKey();
            int val = e.getValue();
            sb.append(String.format("%2d) %s  ->  %d%n",
                    i + 1, a.getId(), val));
        }

        globalLabel.setText(sb.toString());
    }



    public void updateSelected(Article selected, HIndexService.Result res) {
        if (selected == null) {
            clearSelected();
            return;
        }

        removeButton.setDisable(false);

        StringBuilder sb = new StringBuilder();
        sb.append("Seçili Makale\n");
        sb.append("-------------\n");
        sb.append("ID: ").append(selected.getId()).append("\n");
        sb.append("Başlık: ").append(selected.getTitle()).append("\n");
        sb.append("Yıl: ").append(selected.getYear()).append("\n");
        sb.append("Yazar(lar): ").append(selected.getAuthors()).append("\n");
        sb.append("Alınan atıf (global): ").append(selected.getInNeighbors().size()).append("\n\n");

        sb.append("h-index: ").append(res.hIndex()).append("\n");
        sb.append("h-median: ").append(res.hMedian()).append("\n");
        sb.append("h-core makale sayısı: ").append(res.hCore().size()).append("\n\n");
        sb.append("h-core ID'leri:\n");
        for (Article a : res.hCore()) {
            sb.append(" - ").append(a.getId()).append("\n");

        }

        selectedArea.setText(sb.toString());
        selectedArea.positionCaret(0);
    }

    public void clearSelected() {
        selectedArea.clear();
        removeButton.setDisable(true);
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setGraphView(GraphView graphView) {
        this.graphView = graphView;
    }
}