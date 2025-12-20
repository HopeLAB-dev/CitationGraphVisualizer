package com.prolab3;

import com.prolab3.algorithms.BasitAlgoritmalar;
import com.prolab3.models.Graph;
import com.prolab3.models.Makale;
import com.prolab3.models.Node;
import com.prolab3.models.Edge;
import com.prolab3.parsers.ManualJsonParser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    // Global değişkenler (amatörce ama pratik)
    private Graph graph = new Graph();
    private Canvas canvas;
    private GraphicsContext gc;
    
    // UI Elemanları
    private Label lblTotalNodes;
    private Label lblTotalEdges;
    private Label lblCitationInfo;
    private Label lblSelectedNode;
    private TextField txtKCore;
    private TextArea txtLog;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // --- SOL PANEL: İstatistikler ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f0f0f0;");
        leftPanel.setPrefWidth(200);
        
        lblTotalNodes = new Label("Makale: 0");
        lblTotalEdges = new Label("Bağlantı: 0");
        lblCitationInfo = new Label("En çok atıf: -");
        lblSelectedNode = new Label("Seçili: Yok");
        lblSelectedNode.setWrapText(true);
        
        leftPanel.getChildren().addAll(
            new Label("--- İSTATİSTİKLER ---"),
            lblTotalNodes, lblTotalEdges, lblCitationInfo,
            new Separator(),
            new Label("--- SEÇİLİ MAKALE ---"),
            lblSelectedNode
        );
        root.setLeft(leftPanel);

        // --- ÜST PANEL: Kontroller ---
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #e0e0e0;");
        
        Button btnLoad = new Button("JSON Yükle");
        Button btnReset = new Button("Görünümü Sıfırla");
        
        Label lblK = new Label("K Değeri:");
        txtKCore = new TextField("2");
        txtKCore.setPrefWidth(50);
        Button btnKCore = new Button("K-Core Analizi");
        
        Button btnBetweenness = new Button("Betweenness Hesapla");
        
        topPanel.getChildren().addAll(btnLoad, btnReset, new Separator(), lblK, txtKCore, btnKCore, new Separator(), btnBetweenness);
        root.setTop(topPanel);

        // --- ALT PANEL: Log ---
        txtLog = new TextArea();
        txtLog.setPrefHeight(100);
        txtLog.setEditable(false);
        root.setBottom(txtLog);

        // --- ORTA PANEL: Çizim ---
        // ScrollPane içine koymuyorum, direkt canvas sığdığı kadar (basit olsun)
        canvas = new Canvas(1000, 700);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);
        
        // Mouse Tıklama Olayı (Canvas üzerinde)
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            handleMouseClick(event.getX(), event.getY());
        });

        // Buton Aksiyonları
        btnLoad.setOnAction(e -> loadFile(primaryStage));
        btnReset.setOnAction(e -> resetView());
        btnKCore.setOnAction(e -> runKCore());
        btnBetweenness.setOnAction(e -> runBetweenness());

        Scene scene = new Scene(root, 1200, 900);
        primaryStage.setTitle("Prolab 3 - Makale Analiz Sistemi");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void log(String msg) {
        txtLog.appendText(msg + "\n");
    }

    private void loadFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Veri Dosyası Seç");
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            log("Dosya okunuyor: " + file.getName());
            ManualJsonParser parser = new ManualJsonParser();
            List<Makale> makaleler = parser.parse(file.getAbsolutePath());
            
            // Grafı sıfırla ve yeniden kur
            graph = new Graph();
            for (Makale m : makaleler) {
                graph.addNode(new Node(m));
            }
            
            int linkCount = 0;
            for (Makale m : makaleler) {
                Node sourceNode = graph.nodes.get(m.id);
                for (String refId : m.referencedWorks) {
                    if (graph.nodes.containsKey(refId)) {
                        graph.addEdge(m.id, refId);
                        linkCount++;
                    }
                }
            }
            
            log("Yükleme tamamlandı. " + makaleler.size() + " makale, " + linkCount + " bağlantı.");
            updateStats();
            randomLayout(); // Düğümleri rastgele dağıt
            drawGraph();
        }
    }

    // Basit rastgele yerleşim (Layout algoritması yok, sadece scatter)
    private void randomLayout() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        for (Node node : graph.nodes.values()) {
            node.x = 50 + Math.random() * (width - 100);
            node.y = 50 + Math.random() * (height - 100);
            node.color = Color.BLUE; // Sıfırla
            node.radius = 5.0;
        }
    }

    private void updateStats() {
        lblTotalNodes.setText("Makale: " + graph.nodes.size());
        lblTotalEdges.setText("Bağlantı: " + graph.edges.size());
        
        // En çok atıf alanı bul (Basit döngü)
        Makale mostCited = null;
        int maxCit = -1;
        for (Node n : graph.nodes.values()) {
            if (n.article.citationCount > maxCit) {
                maxCit = n.article.citationCount;
                mostCited = n.article;
            }
        }
        if (mostCited != null) {
            lblCitationInfo.setText("En popüler:\n" + mostCited.title + "\n(" + maxCit + " atıf)");
        }
    }

    private void drawGraph() {
        // Ekranı temizle
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. Kenarları Çiz
        gc.setLineWidth(0.5);
        for (Edge edge : graph.edges) {
            // Eğer düğümlerden biri "görünmez" (gri/silik) ise kenarı da silik çiz
            if (edge.source.color == Color.LIGHTGRAY || edge.target.color == Color.LIGHTGRAY) {
                gc.setStroke(Color.LIGHTGRAY);
            } else {
                gc.setStroke(Color.GRAY);
            }
            gc.strokeLine(edge.source.x, edge.source.y, edge.target.x, edge.target.y);
            
            // Ok ucu çizmek biraz karmaşık matematik, amatör projede es geçiyoruz
            // veya basitçe ucuna küçük bir daire koyabiliriz yönü belli etmek için
            // gc.fillOval(edge.target.x - 2, edge.target.y - 2, 4, 4);
        }

        // 2. Düğümleri Çiz
        for (Node node : graph.nodes.values()) {
            gc.setFill(node.color);
            gc.fillOval(node.x - node.radius, node.y - node.radius, node.radius * 2, node.radius * 2);
            
            // Seçiliyse veya H-Core ise ismini yaz
            if (node.color == Color.RED || node.color == Color.ORANGE) {
                gc.setFill(Color.BLACK);
                gc.fillText(node.article.title, node.x + 10, node.y);
            }
        }
    }

    private void handleMouseClick(double x, double y) {
        // Tıklanan düğümü bul (basit mesafe kontrolü)
        Node clickedNode = null;
        for (Node node : graph.nodes.values()) {
            double dist = Math.sqrt(Math.pow(node.x - x, 2) + Math.pow(node.y - y, 2));
            if (dist < node.radius + 5) { // +5 tolerans
                clickedNode = node;
                break;
            }
        }

        if (clickedNode != null) {
            selectNode(clickedNode);
        }
    }

    private void selectNode(Node node) {
        // Herkesi normale döndür
        for (Node n : graph.nodes.values()) {
            n.color = Color.LIGHTGRAY; // Odaklanmak için diğerlerini silikleştir
        }
        
        // Seçileni Kırmızı yap
        node.color = Color.RED;
        node.radius = 8.0;

        // H-Index Hesapla
        int h = BasitAlgoritmalar.hIndexHesapla(node, graph);
        node.hIndex = h;

        // Bilgi panelini güncelle
        lblSelectedNode.setText(
            "Başlık: " + node.article.title + "\n" + 
            "Yıl: " + node.article.year + "\n" + 
            "Yazarlar: " + node.article.authors + "\n" + 
            "Atıf Sayısı: " + node.article.citationCount + "\n" + 
            "H-Index: " + h + "\n" + 
            "Betweenness: " + node.betweenness
        );
        log("Seçilen makale H-Index: " + h);

        // H-Core (Atıf yapanları) Yeşil yap
        // H tanımı: En az h atıfa sahip h makale.
        // Biz sadece atıf yapanları gösterelim (basitçe)
        for (Edge e : node.incomingEdges) {
            e.source.color = Color.GREEN;
        }

        drawGraph();
    }

    private void runKCore() {
        try {
            int k = Integer.parseInt(txtKCore.getText());
            log("K-Core (" + k + ") hesaplanıyor...");
            
            List<String> survivors = BasitAlgoritmalar.kCoreBul(graph, k);
            
            log("K-Core sonrası kalan düğüm sayısı: " + survivors.size());
            
            // Görsel güncelleme
            for (Node n : graph.nodes.values()) {
                if (survivors.contains(n.article.id)) {
                    n.color = Color.ORANGE; // Hayatta kalanlar
                    n.radius = 6.0;
                } else {
                    n.color = Color.LIGHTGRAY; // Elenenler
                    n.radius = 3.0;
                }
            }
            drawGraph();
            
        } catch (NumberFormatException e) {
            log("Hata: Geçerli bir K sayısı girin.");
        }
    }

    private void runBetweenness() {
        log("Betweenness hesaplanıyor (Bu işlem uzun sürebilir)...");
        // Uzun süreceği için ayrı thread'de yapıp UI thread'e dönmek lazım ama
        // amatör olduğu için direkt main thread'de yapıp kilitliyoruz :)
        
        BasitAlgoritmalar.betweennessHesapla(graph);
        
        // En yüksek betweenness'ı bul
        double maxBet = 0;
        Node maxNode = null;
        for (Node n : graph.nodes.values()) {
            if (n.betweenness > maxBet) {
                maxBet = n.betweenness;
                maxNode = n;
            }
        }
        
        log("Betweenness tamamlandı. En merkezi makale: " + (maxNode != null ? maxNode.article.title : "Yok") + " (" + maxBet + ")");
        
        // Belki merkezi düğümleri biraz büyütürüz
        for (Node n : graph.nodes.values()) {
            // Boyutlandırma (logaritmik veya basit oran)
            if (maxBet > 0) {
                n.radius = 5 + (n.betweenness / maxBet) * 10;
            }
        }
        drawGraph();
    }
    
    private void resetView() {
        for (Node n : graph.nodes.values()) {
            n.color = Color.BLUE;
            n.radius = 5.0;
        }
        drawGraph();
        log("Görünüm sıfırlandı.");
    }
}
