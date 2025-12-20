package com.prolab3;

import com.prolab3.algorithms.GraphAlgorithms;
import com.prolab3.models.Graph;
import com.prolab3.models.Node;
import com.prolab3.parsers.ManualJsonParser;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class Main extends Application {

    private Graph fullGraph = new Graph();
    private Graph visualGraph = new Graph();
    
    private Canvas canvas;
    private GraphicsContext gc;
    private TextArea infoArea;
    private Label statusLabel;
    
    private TextField searchField;
    private TextField kCoreField;
    
    // Zoom ve Pan Değişkenleri
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private double lastMouseX, lastMouseY;
    
    // Renk Yönetimi
    // 0: Kök (Kırmızı), 1: İlk H-Core (Mavi), 2: Genişleme 1 (Yeşil), 3: Genişleme 2 (Turuncu)...
    private int clickDepth = 0;
    private final Color[] PALETTE = {
        Color.RED,          // 0: Sorgulanan
        Color.BLUE,         // 1: İlk H-Core
        Color.GREEN,        // 2: Tıklama 1
        Color.ORANGE,       // 3: Tıklama 2
        Color.PURPLE,       // 4: Tıklama 3
        Color.CYAN,         // 5
        Color.BROWN,        // 6
        Color.MAGENTA       // 7
    };
    
    // Animasyon (Force Directed Layout için)
    private AnimationTimer timer;
    private boolean simulationRunning = false;
    
    // Hover Durumu
    private Node hoveredNode = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // --- SOL PANEL ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f4f4f4; border-color: #ddd;");
        leftPanel.setPrefWidth(280);
        
        Button loadBtn = new Button("📁 JSON Yükle");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        
        Separator sep1 = new Separator();
        Label searchLbl = new Label("Makale ID Ara:");
        searchField = new TextField();
        searchField.setPromptText("Örn: W2022...");
        Button searchBtn = new Button("🔍 Ara ve Başlat");
        searchBtn.setMaxWidth(Double.MAX_VALUE);
        
        Separator sep2 = new Separator();
        Button betBtn = new Button("📊 Betweenness Hesapla");
        betBtn.setMaxWidth(Double.MAX_VALUE);
        
        HBox kCoreBox = new HBox(5);
        kCoreField = new TextField("2");
        kCoreField.setPrefWidth(50);
        Button kCoreBtn = new Button("K-Core");
        kCoreBox.getChildren().addAll(new Label("K:"), kCoreField, kCoreBtn);
        
        Separator sep3 = new Separator();
        infoArea = new TextArea();
        infoArea.setEditable(false);
        infoArea.setWrapText(true);
        infoArea.setPrefHeight(400);
        infoArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        
        statusLabel = new Label("Hazır.");
        
        leftPanel.getChildren().addAll(
            loadBtn, statusLabel,
            sep1, searchLbl, searchField, searchBtn,
            sep2, betBtn, kCoreBox,
            sep3, new Label("Detaylar:"), infoArea,
            new Label("\nKullanım:\n- Sol Tık: Genişlet\n- Sağ Tık Sürükle: Gezin\n- Tekerlek: Yakınlaş")
        );
        root.setLeft(leftPanel);

        // --- ORTA PANEL (CANVAS) ---
        canvas = new Canvas(1000, 800);
        gc = canvas.getGraphicsContext2D();
        
        BorderPane centerPane = new BorderPane(canvas);
        centerPane.setStyle("-fx-background-color: #fafafa; -fx-border-color: lightgray;");
        root.setCenter(centerPane);
        
        // --- OLAYLAR ---
        loadBtn.setOnAction(e -> loadData(primaryStage));
        searchBtn.setOnAction(e -> searchAndDisplay(searchField.getText().trim()));
        betBtn.setOnAction(e -> runBetweenness());
        kCoreBtn.setOnAction(e -> runKCore());
        
        // Mouse Etkileşimi
        canvas.setOnScroll(this::handleScroll);
        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            if (e.getButton() == MouseButton.PRIMARY) {
                handleNodeClick(e.getX(), e.getY());
            }
        });
        canvas.setOnMouseDragged(this::handleDrag);
        canvas.setOnMouseMoved(this::handleHover);

        // Layout Animasyonu (Sürekli çizim döngüsü)
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if(simulationRunning) applyForces();
                draw();
            }
        };
        timer.start();

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("Prolab 3 - Gelişmiş Graf Analizi");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- DOSYA YÜKLEME ---
    private void loadData(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("JSON Dosyası Seç");
        // fileChooser.setInitialDirectory(new File(".")); // Kolaylık olsun
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            statusLabel.setText("Dosya okunuyor...");
            new Thread(() -> {
                ManualJsonParser parser = new ManualJsonParser();
                List<com.prolab3.models.Makale> rawArticles = parser.parse(file.getAbsolutePath());
                
                // ID Sıralaması (Yeşil Kenarlar İçin)
                rawArticles.sort(Comparator.comparing(com.prolab3.models.Makale::getId));
                
                fullGraph = new Graph();
                Node prevNode = null;
                
                // Düğümleri oluştur ve ID sırasına göre bağla (Yeşil Kenar Hazırlığı)
                for (com.prolab3.models.Makale m : rawArticles) {
                    Node newNode = new Node(m.getId(), m.getTitle(), m.getAuthors(), m.getYear());
                    fullGraph.addNode(newNode);
                    
                    if (prevNode != null) {
                        prevNode.nextById = newNode;
                    }
                    prevNode = newNode;
                }
                
                // Siyah Kenarları (Referansları) Ekle
                for (com.prolab3.models.Makale m : rawArticles) {
                    for (String refId : m.getReferencedWorks()) {
                        fullGraph.addEdge(m.getId(), refId);
                    }
                }
                
                // İstatistikleri Hesapla
                Node mostCited = null;
                Node mostRef = null;
                int maxCited = -1;
                int maxRef = -1;
                
                for(Node n : fullGraph.getAllNodes()) {
                    if(n.citationCount > maxCited) {
                        maxCited = n.citationCount;
                        mostCited = n;
                    }
                    if(n.outgoingEdges.size() > maxRef) {
                        maxRef = n.outgoingEdges.size();
                        mostRef = n;
                    }
                }
                
                final Node fMostCited = mostCited;
                final Node fMostRef = mostRef;

                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(fullGraph.nodes.size() + " makale yüklendi.");
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("VERİ YÜKLENDİ:\n");
                    sb.append("Makale Sayısı: ").append(fullGraph.nodes.size()).append("\n");
                    sb.append("Toplam Atıf (Kenar): ").append(fullGraph.totalEdges).append("\n");
                    sb.append("────────────────────────\n");
                    
                    if(fMostCited != null) {
                        sb.append("En Çok Atıf Alan:\n");
                        sb.append(" ID: ").append(fMostCited.id).append("\n");
                        sb.append(" Sayı: ").append(fMostCited.citationCount).append("\n");
                    }
                    sb.append("\n");
                    if(fMostRef != null) {
                        sb.append("En Çok Ref. Veren:\n");
                        sb.append(" ID: ").append(fMostRef.id).append("\n");
                        sb.append(" Sayı: ").append(fMostRef.outgoingEdges.size()).append("\n");
                    }
                    
                    sb.append("\nBaşlamak için bir ID aratın.");
                    infoArea.setText(sb.toString());
                            
                    // Dosya yüklendiğinde TÜM grafı göster (Performans limiti dahilinde)
                    showAllNodes();
                });
            }).start();
        }
    }
    
    // Yardımcı Metot: Tüm grafı göster
    private void showAllNodes() {
        visualGraph.clear();
        clickDepth = 0;
        
        List<Node> allNodes = fullGraph.getAllNodes();
        
        // Çok fazla düğüm varsa uyarı ver ve bir kısmını göster (örn: 500)
        // Eğer hepsi isteniyorsa limit artırılabilir ama JavaFX Canvas binlerce çizimde yavaşlayabilir.
        int limit = 300; 
        int count = 0;
        double radius = 400.0;
        
        for(Node n : allNodes) {
            if(count++ > limit) break;
            
            addNodeToVisual(n, Color.LIGHTGRAY);
            // Rastgele dağıt
            n.x = (Math.random() - 0.5) * radius * 2; 
            n.y = (Math.random() - 0.5) * radius * 2;
        }
        
        updateVisualEdges();
        
        // Kamerayı merkeze sıfırla
        translateX = canvas.getWidth() / 2;
        translateY = canvas.getHeight() / 2;
        scale = 0.8; // Biraz uzaklaşarak başla
        
        simulationRunning = true;
        
        if (allNodes.size() > limit) {
             infoArea.appendText("\n\nUYARI: Performans için sadece ilk " + limit + " makale gösteriliyor.\nTam analiz için ID aratın.");
        }
    }

    // --- ARAMA (RESET & BAŞLANGIÇ) ---
    private void searchAndDisplay(String id) {
        if (id.isEmpty()) return;
        Node rootNode = fullGraph.getNode(id);
        
        if (rootNode == null) {
            infoArea.setText("HATA: '" + id + "' ID'li makale bulunamadı.");
            return;
        }
        
        // Grafiği sıfırla
        visualGraph.clear();
        clickDepth = 0; // Renkleri sıfırla
        
        // Kök düğümü ekle
        addNodeToVisual(rootNode, getLevelColor(0));
        rootNode.x = 0; // Merkez
        rootNode.y = 0;
        
        // İlk H-Core'u ekle
        List<Node> hCore = GraphAlgorithms.getHCore(rootNode);
        
        // Radial Layout: Kök etrafına diz
        double angleStep = 360.0 / Math.max(1, hCore.size());
        double radius = 150.0;
        
        for(int i=0; i<hCore.size(); i++) {
            Node hNode = hCore.get(i);
            addNodeToVisual(hNode, getLevelColor(1)); // Seviye 1 Rengi
            hNode.x = radius * Math.cos(Math.toRadians(i * angleStep));
            hNode.y = radius * Math.sin(Math.toRadians(i * angleStep));
        }
        
        clickDepth = 1; // Bir sonraki tıklama Seviye 2 olacak
        updateVisualEdges();
        
        // Görünümü merkeze odakla
        translateX = canvas.getWidth() / 2;
        translateY = canvas.getHeight() / 2;
        scale = 1.0;
        
        updateInfo(rootNode, hCore);
        simulationRunning = true; // Fizik motorunu başlat
    }
    
    // --- TIKLAMA İLE GENİŞLEME ---
    private void handleNodeClick(double mouseX, double mouseY) {
        // Koordinat dönüşümü (Screen -> World)
        double worldX = (mouseX - translateX) / scale;
        double worldY = (mouseY - translateY) / scale;
        
        Node clicked = findNodeAt(worldX, worldY);
        
        if (clicked != null) {
            Node realNode = fullGraph.getNode(clicked.id);
            if (realNode == null) return;
            
            clickDepth++;
            Color nextColor = getLevelColor(clickDepth);
            
            // Yeni H-Core hesapla
            List<Node> newHCore = GraphAlgorithms.getHCore(realNode);
            int addedCount = 0;
            
            // Yeni düğümleri ekle
            double angleStep = 360.0 / Math.max(1, newHCore.size());
            double radius = 120.0; // Tıklananın etrafına
            
            for(int i=0; i<newHCore.size(); i++) {
                Node n = newHCore.get(i);
                
                // Sadece görsel grafta YOKSA ekle ve renklendir
                // Varsa rengini DEĞİŞTİRME (Proje isterlerine göre eski renk kalmalı)
                if(visualGraph.getNode(n.id) == null) {
                    addNodeToVisual(n, nextColor);
                    
                    // Pozisyon: Tıklanan düğümün etrafında
                    n.x = clicked.x + radius * Math.cos(Math.toRadians(i * angleStep));
                    n.y = clicked.y + radius * Math.sin(Math.toRadians(i * angleStep));
                    
                    addedCount++;
                }
            }
            
            updateVisualEdges();
            simulationRunning = true; // Düğümler birbirini itsin diye simülasyonu tetikle
            
            updateInfo(realNode, newHCore);
            infoArea.appendText("\\n\\n>>> GENİŞLETİLDİ <<<\\n" +
                                "Tıklanan: " + clicked.title + "\\n" +
                                "Yeni Eklenen: " + addedCount + "\\n" +
                                "Renk Seviyesi: " + clickDepth);
        }
    }
    
    // --- ZOOM & PAN MANTIĞI ---
    private void handleScroll(ScrollEvent e) {
        double zoomFactor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
        
        double oldScale = scale;
        scale *= zoomFactor;
        
        // Zoom sınırlarını uygula
        if (scale < 0.05) scale = 0.05;
        if (scale > 20.0) scale = 20.0;
        
        // Gerçek zoom oranını hesapla (sınırlara takılmış olabilir)
        double actualZoom = scale / oldScale;
        
        // Mouse'un olduğu noktayı sabit tutacak şekilde translateX/Y güncelle
        translateX = e.getX() - (e.getX() - translateX) * actualZoom;
        translateY = e.getY() - (e.getY() - translateY) * actualZoom;
        
        draw();
    }
    
    private void handleDrag(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.MIDDLE) {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;
            translateX += dx;
            translateY += dy;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            draw();
        }
    }
    
    private void handleHover(MouseEvent e) {
        double worldX = (e.getX() - translateX) / scale;
        double worldY = (e.getY() - translateY) / scale;
        
        Node prevHover = hoveredNode;
        hoveredNode = findNodeAt(worldX, worldY);
        
        // Sadece değişim varsa yeniden çiz (Performans)
        if (prevHover != hoveredNode) {
            draw();
        }
    }

    // --- FİZİK MOTORU (Force Directed Layout - Basit) ---
    // Düğümlerin üst üste binmesini engeller ve grafı yayar
    private void applyForces() {
        List<Node> nodes = visualGraph.getAllNodes();
        if (nodes.size() > 200) return; // Performans koruması: Çok düğüm varsa fiziği kapat

        double repulsion = 5000;
        double springLength = 100;
        double springStrength = 0.05;
        
        for (int i = 0; i < nodes.size(); i++) {
            Node n1 = nodes.get(i);
            double fx = 0, fy = 0;
            
            // İtme Kuvveti (Her düğüm diğerini iter)
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) continue;
                Node n2 = nodes.get(j);
                double dx = n1.x - n2.x;
                double dy = n1.y - n2.y;
                double distSq = dx*dx + dy*dy;
                if (distSq < 0.01) distSq = 0.01;
                
                double force = repulsion / distSq;
                double dist = Math.sqrt(distSq);
                fx += (dx / dist) * force;
                fy += (dy / dist) * force;
            }
            
            // Çekme Kuvveti (Bağlı düğümler birbirini çeker)
            // Visual grafta outgoing edge var mı kontrol et
            Node realN1 = fullGraph.getNode(n1.id);
            if (realN1 == null) continue; // Güvenlik kontrolü
            
            for(Node target : realN1.outgoingEdges) {
                Node n2 = visualGraph.getNode(target.id);
                if (n2 != null) {
                    double dx = n2.x - n1.x;
                    double dy = n2.y - n1.y;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    
                    double force = (dist - springLength) * springStrength;
                    fx += (dx / dist) * force;
                    fy += (dy / dist) * force;
                }
            }
            
            // Gelen kenarlar için de çekme (simetrik stabilite için)
            for(Node source : realN1.incomingEdges) {
                Node n2 = visualGraph.getNode(source.id);
                if (n2 != null) {
                    double dx = n2.x - n1.x;
                    double dy = n2.y - n1.y;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    double force = (dist - springLength) * springStrength;
                    fx += (dx / dist) * force;
                    fy += (dy / dist) * force;
                }
            }
            
            // Hızı uygula (Damping ile)
            n1.x += fx * 0.1; // Hız faktörü
            n1.y += fy * 0.1;
        }
    }

    // --- ÇİZİM ---
    private void draw() {
        // Ekranı temizle
        gc.setTransform(new Affine());
        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Zoom ve Pan uygula
        gc.translate(translateX, translateY);
        gc.scale(scale, scale);
        
        List<Node> nodes = visualGraph.getAllNodes();

        // 1. Yeşil Kenarlar (ID Sırası) - En altta çiz
        gc.setStroke(Color.LIGHTGREEN);
        gc.setLineWidth(1.5); // Yeşil kenarlar daha belirgin olabilir
        for (Node n : nodes) {
            if (n.nextById != null) {
                Node next = visualGraph.getNode(n.nextById.id);
                // Eğer ID komşusu da ekrandaysa yeşil çizgi çek
                if (next != null) {
                     // Ok yerine düz çizgi veya ok olabilir. PDF "Yeşil kenarlar" diyor.
                     // Genelde flow'u göstermek için ok iyidir ama karışıklığı önlemek için düz çizgi yapıyoruz.
                     gc.strokeLine(n.x, n.y, next.x, next.y);
                }
            }
        }
        
        // 2. Siyah Kenarlar (Referanslar)
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1.0); 
        
        for (Node n : nodes) {
            Node realSource = fullGraph.getNode(n.id);
            for (Node target : realSource.outgoingEdges) {
                Node vTarget = visualGraph.getNode(target.id);
                if (vTarget != null) {
                    drawArrow(n.x, n.y, vTarget.x, vTarget.y);
                }
            }
        }
        
        // 3. Düğümler
        for (Node n : nodes) {
            gc.setFill(n.color);
            gc.fillOval(n.x - n.radius, n.y - n.radius, n.radius*2, n.radius*2);
            
            // Seçililik veya özel durum için çerçeve
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.0);
            gc.strokeOval(n.x - n.radius, n.y - n.radius, n.radius*2, n.radius*2);
            
            // Metin
            if (scale > 0.5) {
                gc.setFill(Color.BLACK);
                gc.fillText(n.id, n.x - 10, n.y - 15);
            }
        }
        
        // 4. Hover Bilgi Kartı (Tooltip) - En üstte
        if (hoveredNode != null) {
            drawTooltip(hoveredNode);
        }
    }
    
    private void drawTooltip(Node n) {
        // Transformu sıfırla ki tooltip zoom'dan etkilenmesin (UI elemanı gibi dursun)
        // Veya zoomlu koordinatta çizilsin. Zoomlu olması daha doğal durabilir node yanında.
        
        double infoX = n.x + n.radius + 10;
        double infoY = n.y - 50;
        
        // İçerik Hazırla
        String authors = String.join(", ", n.authors);
        if(authors.length() > 30) authors = authors.substring(0, 27) + "...";
        String title = n.title;
        if(title.length() > 30) title = title.substring(0, 27) + "...";
        
        String[] lines = {
            "ID: " + n.id,
            "Yazar: " + authors,
            "Başlık: " + title,
            "Yıl: " + n.year,
            "Atıf: " + n.citationCount
        };
        
        // Kutu Boyutları
        double width = 220;
        double lineHeight = 15;
        double height = lines.length * lineHeight + 20;
        
        // Arkaplan
        gc.setFill(Color.rgb(255, 255, 220, 0.9)); // Hafif sarımsı, şeffaf
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.fillRect(infoX, infoY, width, height);
        gc.strokeRect(infoX, infoY, width, height);
        
        // Yazı
        gc.setFill(Color.BLACK);
        // Fontu biraz küçültmek gerekebilir, varsayılan fontu kullanıyoruz
        for(int i=0; i<lines.length; i++) {
            gc.fillText(lines[i], infoX + 10, infoY + 20 + (i * lineHeight));
        }
    }
    
    private void drawArrow(double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double dist = Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
        
        // Düğümlerin içine girmesin diye uçlardan kırp
        double nodeRadius = 15.0; 
        double startX = x1 + nodeRadius * Math.cos(angle);
        double startY = y1 + nodeRadius * Math.sin(angle);
        double endX = x2 - nodeRadius * Math.cos(angle);
        double endY = y2 - nodeRadius * Math.sin(angle);
        
        if (dist > nodeRadius * 2) {
            gc.strokeLine(startX, startY, endX, endY);
            
            // Ok ucu
            double arrowLen = 8;
            double arrowAngle = Math.toRadians(25);
            double x3 = endX - arrowLen * Math.cos(angle - arrowAngle);
            double y3 = endY - arrowLen * Math.sin(angle - arrowAngle);
            double x4 = endX - arrowLen * Math.cos(angle + arrowAngle);
            double y4 = endY - arrowLen * Math.sin(angle + arrowAngle);
            
            gc.strokeLine(endX, endY, x3, y3);
            gc.strokeLine(endX, endY, x4, y4);
        }
    }

    // --- YARDIMCILAR ---
    private Color getLevelColor(int depth) {
        return PALETTE[depth % PALETTE.length];
    }
    
    private Node findNodeAt(double x, double y) {
        for (Node n : visualGraph.getAllNodes()) {
            double dist = Math.sqrt(Math.pow(n.x - x, 2) + Math.pow(n.y - y, 2));
            if (dist < n.radius + 2) return n;
        }
        return null;
    }
    
    private void addNodeToVisual(Node realNode, Color c) {
        if (visualGraph.getNode(realNode.id) == null) {
            // Görsel özellikleri set et
            realNode.color = c;
            visualGraph.addNode(realNode);
        }
    }
    
    private void updateVisualEdges() {
        visualGraph.totalEdges = 0;
        // Kenar sayısı istatistiği için
        for (Node n : visualGraph.getAllNodes()) {
            Node real = fullGraph.getNode(n.id);
            for (Node t : real.outgoingEdges) {
                if (visualGraph.getNode(t.id) != null) visualGraph.totalEdges++;
            }
        }
    }
    
    private void updateInfo(Node node, List<Node> hCore) {
        int h = GraphAlgorithms.calculateHIndex(node);
        double median = GraphAlgorithms.calculateHMedian(hCore);
        
        StringBuilder sb = new StringBuilder();
        sb.append("SEÇİLİ MAKALE:\n");
        sb.append("ID: ").append(node.id).append("\n");
        sb.append("Başlık: ").append(node.title).append("\n");
        sb.append("Yıl: ").append(node.year).append("\n");
        sb.append("Atıf Sayısı: ").append(node.citationCount).append("\n\n");
        
        sb.append("ANALİZ SONUÇLARI:\n");
        sb.append("H-Index: ").append(h).append("\n");
        sb.append("H-Median: ").append(median).append("\n");
        sb.append("H-Core Boyutu: ").append(hCore.size()).append("\n");
        sb.append("H-Core Listesi (İlk 20):\n");
        
        for(int i=0; i<Math.min(20, hCore.size()); i++) {
            sb.append("- ").append(hCore.get(i).id).append("\n");
        }
        
        infoArea.setText(sb.toString());
    }

    private void runBetweenness() {
        if (fullGraph.nodes.isEmpty()) return;
        statusLabel.setText("Hesaplanıyor...");
        simulationRunning = false; // Hesaplama sırasında animasyonu durdur (CPU koru)
        
        new Thread(() -> {
            GraphAlgorithms.calculateBetweenness(fullGraph);
            
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Betweenness Bitti.");
                simulationRunning = true;
                
                // Görseldeki düğümleri boyuta göre güncelle
                for (Node n : visualGraph.getAllNodes()) {
                    Node real = fullGraph.getNode(n.id);
                    n.radius = 15 + Math.min(30, real.betweenness * 5);
                }
                
                StringBuilder sb = new StringBuilder("EN YÜKSEK BETWEENNESS:\n");
                fullGraph.getAllNodes().stream()
                    .sorted((a,b) -> Double.compare(b.betweenness, a.betweenness))
                    .limit(20)
                    .forEach(n -> sb.append(String.format("%.2f - %s\n", n.betweenness, n.title)));
                    
                infoArea.setText(sb.toString());
            });
        }).start();
    }
    
    private void runKCore() {
        try {
            int k = Integer.parseInt(kCoreField.getText());
            statusLabel.setText("K-Core çalışıyor...");
            simulationRunning = false;
            
            new Thread(() -> {
                List<String> survivors = GraphAlgorithms.calculateKCore(fullGraph, k);
                
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("K-Core Bitti.");
                    simulationRunning = true;
                    
                    visualGraph.clear();
                    clickDepth = 0;
                    
                    int count = 0;
                    double radius = 400.0;
                    // K-Core sonucunda limit daha yüksek olabilir
                    int limit = 500;
                    
                    for(String id : survivors) {
                        if(count++ > limit) break; 
                        Node n = fullGraph.getNode(id);
                        if (n != null) {
                            addNodeToVisual(n, Color.DEEPPINK);
                            // Merkez etrafına dağıt
                            n.x = (Math.random() - 0.5) * radius * 2;
                            n.y = (Math.random() - 0.5) * radius * 2;
                        }
                    }
                    updateVisualEdges();
                    
                    // Kamerayı sıfırla ki sonuç görünsün
                    translateX = canvas.getWidth() / 2;
                    translateY = canvas.getHeight() / 2;
                    scale = 0.8;
                    
                    infoArea.setText("K-Core (" + k + ") Sonuç:\n" +
                            "Toplam Kalan: " + survivors.size() + "\n" +
                            "(Performans için ilk " + limit + " tanesi görselleştirildi)");
                });
            }).start();
            
        } catch (Exception e) {
            statusLabel.setText("Hata: K sayısı girin.");
        }
    }
}