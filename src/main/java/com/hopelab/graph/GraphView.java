package com.hopelab.graph;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class GraphView {

    private final CitationGraph graph;
    private final InfoPanel infoPanel;

    private final Pane root;
    private final Group graphGroup;

    private final Map<Article, Circle> nodeMap = new HashMap<>();
    private final Map<Article, Text> labelMap = new HashMap<>();
    private final Map<Article, Double> baseRadiusMap = new HashMap<>();
    private final Map<Article, Point2D> defaultPosMap = new HashMap<>();

    private final Set<Article> hCore = new HashSet<>();
    private final Set<Article> kCoreSet = new HashSet<>();
    private final Set<Article> visibleNodes = new HashSet<>();

    private boolean showIdEdges = true;
    private boolean labelsEnabled = false;
    private boolean enlargedMode = false;

    // Arama ağacı modu
    private boolean searchMode = false;

    // Analiz modu (betweenness + k-core decomposition)
    private boolean analysisMode = false;

    private final Map<Article, Integer> treeDepth = new HashMap<>();
    private final Map<Article, Integer> treeIndex = new HashMap<>();
    private final Map<Integer, Integer> depthNextIndex = new HashMap<>();

    private static class EdgeView {
        final Article from;
        final Article to;
        final Line main;
        final Line arrow1;
        final Line arrow2;

        EdgeView(Article from, Article to, Line main, Line arrow1, Line arrow2) {
            this.from = from;
            this.to = to;
            this.main = main;
            this.arrow1 = arrow1;
            this.arrow2 = arrow2;
        }
    }

    private final List<EdgeView> citationEdges = new ArrayList<>();
    private final List<EdgeView> idEdges = new ArrayList<>();

    private Article selected;

    // Zoom & pan
    private double scale = 1.0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean dragging = false;

    // Betweenness (tüm graf için, node boyutunda kullandığımız)
    private final Map<Article, Double> betweenness;
    private double bcMin = Double.MAX_VALUE;
    private double bcMax = Double.MIN_VALUE;

    private static final double SEARCH_RADIUS_FACTOR = 4.0;

    public GraphView(CitationGraph graph, InfoPanel infoPanel) {
        this.graph = graph;
        this.infoPanel = infoPanel;

        root = new Pane();
        root.setPrefSize(900, 800);
        root.setStyle("-fx-background-color: #202020;");

        graphGroup = new Group();
        root.getChildren().add(graphGroup);

        // 1) Betweenness tüm graf için (node radius)
        betweenness = GraphMetrics.betweenness(graph);
        for (double v : betweenness.values()) {
            if (v < bcMin) bcMin = v;
            if (v > bcMax) bcMax = v;
        }
        if (bcMin == Double.MAX_VALUE) bcMin = 0;
        if (bcMax == Double.MIN_VALUE) bcMax = 1;

        // 2) Nodes & edges
        layoutAndCreateNodes();
        createCitationEdges();
        resetEdgeColorsToDefault();
        createIdOrderEdges();
        setupZoomAndPan();

        visibleNodes.addAll(graph.getArticles());
        updateVisibility();
        updateNodeColors();
        refreshStats();
    }

    // ----------------------------------------------------
    // NODE + LABEL
    // ----------------------------------------------------

    private void layoutAndCreateNodes() {
        List<Article> list = new ArrayList<>(graph.getArticles());
        list.sort(Comparator.comparing(Article::getId));
        int n = list.size();

        double w = 900;
        double h = 800;
        double cx = w / 2.0;
        double cy = h / 2.0;

        int ringCount = 20;
        int perRing = (int) Math.ceil(n / (double) ringCount);

        double baseR = 170;
        double ringGap = 35;

        double minR = 8;
        double maxR = 20;
        double range = bcMax - bcMin;
        if (range == 0) range = 1;

        for (int i = 0; i < n; i++) {
            Article a = list.get(i);

            int ringIndex = i / perRing;
            int indexInRing = i % perRing;
            int countInThisRing = Math.min(perRing, n - ringIndex * perRing);

            double R = baseR + ringIndex * ringGap;
            double angle = 2 * Math.PI * indexInRing / countInThisRing;

            double x = cx + R * Math.cos(angle);
            double y = cy + R * Math.sin(angle);

            double bc = betweenness.getOrDefault(a, 0.0);
            double t = (bc - bcMin) / range;
            t = Math.sqrt(Math.max(0, t));
            double radius = minR + t * (maxR - minR);

            Circle c = new Circle(x, y, radius);
            c.setFill(Color.GREY);
            c.setStroke(Color.BLACK);
            c.setVisible(false);

            baseRadiusMap.put(a, radius);
            defaultPosMap.put(a, new Point2D(x, y));

            String tooltipText = """
                    ID: %s
                    Title: %s
                    Yıl: %d
                    Yazar(lar): %s
                    Alınan atıf: %d
                    """.formatted(
                    a.getId(),
                    a.getTitle(),
                    a.getYear(),
                    a.getAuthors(),
                    a.getInNeighbors().size()
            );
            Tooltip.install(c, new Tooltip(tooltipText));

            // Label: ID + global citation
            int citations = a.getInNeighbors().size();
            String labelStr = "%s\n%d".formatted(a.getId(), citations);
            Text label = new Text(labelStr);
            label.setFill(Color.BLUE);
            label.setStroke(Color.BLUE);
            label.setStrokeWidth(0.4);
            label.setTextAlignment(TextAlignment.CENTER);
            label.setTextOrigin(VPos.CENTER);
            label.setMouseTransparent(true);

            updateLabelAppearance(c, label);
            label.setVisible(false);

            // Hover
            c.setOnMouseEntered(e -> {
                c.setScaleX(1.25);
                c.setScaleY(1.25);
                c.setStrokeWidth(2.0);
                Text lbl = labelMap.get(a);
                if (lbl != null) {
                    lbl.setScaleX(1.25);
                    lbl.setScaleY(1.25);
                }
            });

            c.setOnMouseExited(e -> {
                c.setScaleX(1.0);
                c.setScaleY(1.0);
                c.setStrokeWidth(1.0);
                Text lbl = labelMap.get(a);
                if (lbl != null) {
                    lbl.setScaleX(1.0);
                    lbl.setScaleY(1.0);
                }
            });

            // Tıklama
            c.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (analysisMode) {
                        // Analiz modunda: sadece bilgi göster, grafı genişletme / renkleri değiştirme
                        selected = a;
                        HIndexService.Result res = HIndexService.computeFor(a);
                        infoPanel.updateSelected(a, res);
                        e.consume();
                        return;
                    }

                    // Normal mod: grafı genişlet
                    focusArticle(a, false);
                    e.consume();
                }
            });


            nodeMap.put(a, c);
            labelMap.put(a, label);

            graphGroup.getChildren().add(c);
            graphGroup.getChildren().add(label);
        }
    }

    private void updateLabelAppearance(Circle c, Text label) {
        double radius = c.getRadius();
        double centerX = c.getCenterX();
        double centerY = c.getCenterY();

        double fontSize = Math.max(6, Math.min(18, radius * 0.9));
        label.setStyle("-fx-font-size: " + fontSize + "px;");
        label.applyCss();

        double maxDiam = radius * 2.0 * 0.9;
        int safety = 25;
        while (safety-- > 0) {
            Bounds b = label.getLayoutBounds();
            double wTxt = b.getWidth();
            double hTxt = b.getHeight();
            if (wTxt <= maxDiam && hTxt <= maxDiam) break;

            fontSize -= 0.8;
            if (fontSize < 6) {
                fontSize = 6;
                label.setStyle("-fx-font-size: " + fontSize + "px;");
                label.applyCss();
                break;
            }
            label.setStyle("-fx-font-size: " + fontSize + "px;");
            label.applyCss();
        }

        label.setTextOrigin(VPos.CENTER);
        label.applyCss();
        Bounds b = label.getLayoutBounds();
        double wTxt = b.getWidth();

        label.setX(centerX - wTxt / 2.0);
        label.setY(centerY);
    }

    // ----------------------------------------------------
    // FOCUS / SEARCH
    // ----------------------------------------------------

    private void focusArticle(Article a, boolean resetVisible) {
        if (a == null) return;

        Set<Article> prevVisible = new HashSet<>(visibleNodes);

        if (resetVisible) {
            visibleNodes.clear();
            hCore.clear();
            kCoreSet.clear();
            treeDepth.clear();
            treeIndex.clear();
            depthNextIndex.clear();
        }

        selected = a;
        HIndexService.Result res = HIndexService.computeFor(a);
        hCore.clear();
        hCore.addAll(res.hCore);

        showIdEdges = false;
        visibleNodes.add(a);
        visibleNodes.addAll(res.hCore);

        highlightNewEdges(prevVisible);

        if (searchMode) {
            expandTreeFrom(a, res.hCore);
            updateAllEdges();
        }

        updateVisibility();
        updateNodeColors();
        refreshStats();
        infoPanel.updateSelected(a, res);
    }

    public boolean focusArticleById(String rawId, boolean resetVisible) {
        if (rawId == null || rawId.isBlank()) return false;

        String id = rawId.trim();
        int slash = id.lastIndexOf('/');
        if (slash >= 0 && slash < id.length() - 1) {
            id = id.substring(slash + 1);
        }

        Article a = graph.getArticle(id);
        if (a == null) return false;

        searchMode = true;
        focusArticle(a, true);
        return true;
    }

    public void removeSelected() {
        if (selected == null) return;
        if (analysisMode) return; // analiz modunda node silemeyiz

        Article r = selected;
        visibleNodes.remove(r);
        hCore.remove(r);
        kCoreSet.remove(r);
        selected = null;

        updateVisibility();
        updateNodeColors();
        refreshStats();
        infoPanel.clearSelected();
    }

    // Arama modunda label + radius büyütme
    public void setLabelsEnabled(boolean enabled) {
        this.labelsEnabled = enabled;

        if (enabled && !enlargedMode) {
            applyRadiusScale(SEARCH_RADIUS_FACTOR);
            enlargedMode = true;
        } else if (!enabled && enlargedMode) {
            applyRadiusScale(1.0);
            enlargedMode = false;
        }

        updateVisibility();
    }

    private void applyRadiusScale(double factor) {
        // Düğüm yarıçaplarını büyüt / küçült
        for (Map.Entry<Article, Circle> e : nodeMap.entrySet()) {
            Article a = e.getKey();
            Circle c = e.getValue();
            Double baseR = baseRadiusMap.get(a);
            if (baseR == null) continue;

            c.setRadius(baseR * factor);

            Text label = labelMap.get(a);
            if (label != null) {
                updateLabelAppearance(c, label);
            }
        }

        // Kenar kalınlıklarını da aynı oranla büyüt
        double edgeWidth = 1.0 * factor;
        double arrowWidth = Math.max(0.5, edgeWidth * 0.8);

        for (EdgeView e : citationEdges) {
            e.main.setStrokeWidth(edgeWidth);
            e.arrow1.setStrokeWidth(arrowWidth);
            e.arrow2.setStrokeWidth(arrowWidth);
        }
        for (EdgeView e : idEdges) {
            e.main.setStrokeWidth(edgeWidth);
            e.arrow1.setStrokeWidth(arrowWidth);
            e.arrow2.setStrokeWidth(arrowWidth);
        }

        updateAllEdges();
    }


    // ----------------------------------------------------
    // EDGES
    // ----------------------------------------------------

    private EdgeView createDirectedEdge(Article fromA, Article toA,
                                        Circle fromC, Circle toC,
                                        Color color, double opacity) {

        double x1 = fromC.getCenterX();
        double y1 = fromC.getCenterY();
        double x2 = toC.getCenterX();
        double y2 = toC.getCenterY();

        double angle = Math.atan2(y2 - y1, x2 - x1);

        double r1 = fromC.getRadius();
        double r2 = toC.getRadius();
        double margin = 2.0;

        double sx = x1 + (r1 + margin) * Math.cos(angle);
        double sy = y1 + (r1 + margin) * Math.sin(angle);

        double ex = x2 - (r2 + margin) * Math.cos(angle);
        double ey = y2 - (r2 + margin) * Math.sin(angle);

        Line main = new Line(sx, sy, ex, ey);
        main.setStroke(color);
        main.setOpacity(opacity);
        main.setStrokeWidth(1.0);

        double arrowLength = 10;
        double arrowAngle = Math.toRadians(25);

        double x3 = ex - arrowLength * Math.cos(angle - arrowAngle);
        double y3 = ey - arrowLength * Math.sin(angle - arrowAngle);
        double x4 = ex - arrowLength * Math.cos(angle + arrowAngle);
        double y4 = ey - arrowLength * Math.sin(angle + arrowAngle);

        Line arrow1 = new Line(ex, ey, x3, y3);
        Line arrow2 = new Line(ex, ey, x4, y4);
        arrow1.setStroke(color);
        arrow2.setStroke(color);
        arrow1.setOpacity(opacity);
        arrow2.setOpacity(opacity);

        graphGroup.getChildren().add(0, arrow2);
        graphGroup.getChildren().add(0, arrow1);
        graphGroup.getChildren().add(0, main);

        main.setVisible(false);
        arrow1.setVisible(false);
        arrow2.setVisible(false);

        return new EdgeView(fromA, toA, main, arrow1, arrow2);
    }

    private void createCitationEdges() {
        for (Article a : graph.getArticles()) {
            Circle fromCircle = nodeMap.get(a);
            if (fromCircle == null) continue;

            for (Article b : a.getOutNeighbors()) {
                Circle toCircle = nodeMap.get(b);
                if (toCircle == null) continue;

                EdgeView ev = createDirectedEdge(
                        a, b, fromCircle, toCircle,
                        Color.WHITE, 0.3
                );
                citationEdges.add(ev);
            }
        }
    }

    private void createIdOrderEdges() {
        List<Article> list = new ArrayList<>(graph.getArticles());
        list.sort(Comparator.comparing(Article::getId));

        for (int i = 0; i < list.size() - 1; i++) {
            Article a = list.get(i);
            Article b = list.get(i + 1);

            Circle fromCircle = nodeMap.get(a);
            Circle toCircle = nodeMap.get(b);
            if (fromCircle == null || toCircle == null) continue;

            EdgeView ev = createDirectedEdge(
                    a, b, fromCircle, toCircle,
                    Color.LIMEGREEN, 1.0
            );
            idEdges.add(ev);
        }
    }

    private void updateEdgeGeometry(EdgeView e) {
        Circle fromC = nodeMap.get(e.from);
        Circle toC   = nodeMap.get(e.to);
        if (fromC == null || toC == null) return;

        double x1 = fromC.getCenterX();
        double y1 = fromC.getCenterY();
        double x2 = toC.getCenterX();
        double y2 = toC.getCenterY();

        double angle = Math.atan2(y2 - y1, x2 - x1);

        double r1 = fromC.getRadius();
        double r2 = toC.getRadius();
        double margin = 2.0;

        double sx = x1 + (r1 + margin) * Math.cos(angle);
        double sy = y1 + (r1 + margin) * Math.sin(angle);

        double ex = x2 - (r2 + margin) * Math.cos(angle);
        double ey = y2 - (r2 + margin) * Math.sin(angle);

        e.main.setStartX(sx);
        e.main.setStartY(sy);
        e.main.setEndX(ex);
        e.main.setEndY(ey);

        double arrowLength = Math.max(8, Math.min(r1, r2) * 0.9);
        double arrowAngle = Math.toRadians(25);

        double x3 = ex - arrowLength * Math.cos(angle - arrowAngle);
        double y3 = ey - arrowLength * Math.sin(angle - arrowAngle);
        double x4 = ex - arrowLength * Math.cos(angle + arrowAngle);
        double y4 = ey - arrowLength * Math.sin(angle + arrowAngle);

        e.arrow1.setStartX(ex);
        e.arrow1.setStartY(ey);
        e.arrow1.setEndX(x3);
        e.arrow1.setEndY(y3);

        e.arrow2.setStartX(ex);
        e.arrow2.setStartY(ey);
        e.arrow2.setEndX(x4);
        e.arrow2.setEndY(y4);
    }

    private void updateAllEdges() {
        for (EdgeView e : citationEdges) {
            updateEdgeGeometry(e);
        }
        for (EdgeView e : idEdges) {
            updateEdgeGeometry(e);
        }
    }

    // ----------------------------------------------------
    // AĞAÇ YERLEŞİMİ
    // ----------------------------------------------------

    private void expandTreeFrom(Article center, Collection<Article> newCore) {
        if (center == null) return;

        double width  = root.getWidth()  > 0 ? root.getWidth()  : root.getPrefWidth();
        double height = root.getHeight() > 0 ? root.getHeight() : root.getPrefHeight();
        if (width <= 0)  width  = 900;
        if (height <= 0) height = 700;

        double topMargin  = 80;
        double rowGap     = 160;  // satırlar arası mesafe
        double leftMargin = 150;
        double colGap     = 260;  // aynı satırdaki düğümler arası mesafe

        if (!treeDepth.containsKey(center)) {
            treeDepth.put(center, 0);
            treeIndex.put(center, 0);

            Circle c = nodeMap.get(center);
            if (c != null) {
                double x = width / 2.0;
                double y = topMargin;
                c.setCenterX(x);
                c.setCenterY(y);

                Text label = labelMap.get(center);
                if (label != null) {
                    updateLabelAppearance(c, label);
                }
            }

            depthNextIndex.put(0, 1);
        }

        int centerDepth = treeDepth.get(center);

        for (Article a : newCore) {
            if (a == center) continue;
            if (treeDepth.containsKey(a)) continue;

            int depth = centerDepth + 1;
            int idx   = depthNextIndex.getOrDefault(depth, 0);
            depthNextIndex.put(depth, idx + 1);

            treeDepth.put(a, depth);
            treeIndex.put(a, idx);

            Circle c = nodeMap.get(a);
            if (c != null) {
                double y = topMargin + depth * rowGap;
                double x = leftMargin + idx * colGap;

                c.setCenterX(x);
                c.setCenterY(y);

                Text label = labelMap.get(a);
                if (label != null) {
                    updateLabelAppearance(c, label);
                }
            }
        }
    }

    // ----------------------------------------------------
    // ZOOM & PAN & TIKLAMA
    // ----------------------------------------------------

    private void setupZoomAndPan() {
        root.addEventFilter(ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY();
            double factor = (deltaY > 0) ? 1.1 : 0.9;
            scale *= factor;
            scale = Math.max(0.3, Math.min(scale, 5.0));

            graphGroup.setScaleX(scale);
            graphGroup.setScaleY(scale);

            event.consume();
        });

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                dragging = true;
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (dragging && event.isSecondaryButtonDown()) {
                double dx = event.getSceneX() - lastMouseX;
                double dy = event.getSceneY() - lastMouseY;

                graphGroup.setTranslateX(graphGroup.getTranslateX() + dx);
                graphGroup.setTranslateY(graphGroup.getTranslateY() + dy);

                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                dragging = false;
            }
        });

        // Boş alana sol tık → reset yok, sadece seçili düğümü kaldır
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY &&
                    !(event.getTarget() instanceof Circle)) {

                selected = null;
                if (!analysisMode) {
                    // h-core renkleri kalsın, sadece seçili düğüm kalkıyor
                    infoPanel.clearSelected();
                    updateNodeColors();
                }
            }
        });
    }

    // ----------------------------------------------------
    // GÖRÜNÜRLÜK, RENK, İSTATİSTİK
    // ----------------------------------------------------

    private void updateVisibility() {
        for (Map.Entry<Article, Circle> entry : nodeMap.entrySet()) {
            Article a = entry.getKey();
            Circle c = entry.getValue();
            boolean vis = visibleNodes.contains(a);
            c.setVisible(vis);

            Text label = labelMap.get(a);
            if (label != null) {
                label.setVisible(vis && labelsEnabled);
            }
        }

        for (EdgeView e : citationEdges) {
            boolean show = visibleNodes.contains(e.from) && visibleNodes.contains(e.to);
            e.main.setVisible(show);
            if (analysisMode) {
                e.arrow1.setVisible(false);
                e.arrow2.setVisible(false);
            } else {
                e.arrow1.setVisible(show);
                e.arrow2.setVisible(show);
            }
        }

        for (EdgeView e : idEdges) {
            boolean show = showIdEdges &&
                    visibleNodes.contains(e.from) &&
                    visibleNodes.contains(e.to);

            if (analysisMode) {
                e.main.setVisible(false);
                e.arrow1.setVisible(false);
                e.arrow2.setVisible(false);
            } else {
                e.main.setVisible(show);
                e.arrow1.setVisible(show);
                e.arrow2.setVisible(show);
            }
        }
    }

    private void resetPositionsToCircleLayout() {
        for (Map.Entry<Article, Circle> entry : nodeMap.entrySet()) {
            Article a = entry.getKey();
            Circle c = entry.getValue();
            Point2D p = defaultPosMap.get(a);
            if (p != null) {
                c.setCenterX(p.getX());
                c.setCenterY(p.getY());
                Text label = labelMap.get(a);
                if (label != null) {
                    updateLabelAppearance(c, label);
                }
            }
        }
        updateAllEdges();
    }

    public void resetView() {
        selected = null;
        hCore.clear();
        kCoreSet.clear();

        showIdEdges = true;
        labelsEnabled = false;
        searchMode = false;
        analysisMode = false;

        treeDepth.clear();
        treeIndex.clear();
        depthNextIndex.clear();

        if (enlargedMode) {
            applyRadiusScale(1.0);
            enlargedMode = false;
        }

        visibleNodes.clear();
        visibleNodes.addAll(graph.getArticles());

        resetPositionsToCircleLayout();

        resetEdgeColorsToDefault();

        updateVisibility();
        updateNodeColors();
        refreshStats();
        infoPanel.clearSelected();
    }

    private void updateNodeColors() {
        for (Map.Entry<Article, Circle> entry : nodeMap.entrySet()) {
            Article a = entry.getKey();
            Circle c = entry.getValue();

            Color fill;
            Color stroke = Color.BLACK;

            if (analysisMode) {
                // Analiz modunda sadece k-core mavi, diğerleri gri
                if (kCoreSet.contains(a)) {
                    fill = Color.web("#4aa3ff");
                } else {
                    fill = Color.LIGHTGRAY;
                }
            } else {
                fill = Color.LIGHTGRAY;
                if (kCoreSet.contains(a)) {
                    fill = Color.web("#4aa3ff");
                }
                if (hCore.contains(a)) {
                    fill = Color.GOLD;
                }
                if (a == selected) {
                    fill = Color.RED;
                }
            }
            c.setFill(fill);
            c.setStroke(stroke);
        }

        // k-core kenar renklerini de güncelle
        updateKCoreEdgeColors();
    }

    private void updateKCoreEdgeColors() {
        for (EdgeView e : citationEdges) {
            if (!visibleNodes.contains(e.from) || !visibleNodes.contains(e.to)) continue;

            boolean inCore = kCoreSet.contains(e.from) && kCoreSet.contains(e.to);

            if (analysisMode) {
                if (inCore) {
                    e.main.setStroke(Color.web("#4aa3ff"));
                } else {
                    e.main.setStroke(Color.web("#999999"));
                }
            } else {
                if (inCore) {
                    e.main.setStroke(Color.web("#4aa3ff"));
                } else {
                    // normal modda core'da değilse default gri olsun
                    e.main.setStroke(Color.web("#999999"));
                }
            }
        }
    }


    private void highlightNewEdges(Set<Article> prevVisible) {
        // Bu sadece normal (analiz dışı) modda kullanılacak
        if (analysisMode) return;

        for (EdgeView e : citationEdges) {
            boolean wasVisible = prevVisible.contains(e.from) && prevVisible.contains(e.to);
            boolean isVisible = visibleNodes.contains(e.from) && visibleNodes.contains(e.to);

            if (!isVisible) continue;

            if (!wasVisible && isVisible) {
                e.main.setStroke(Color.RED);
                e.arrow1.setStroke(Color.RED);
                e.arrow2.setStroke(Color.RED);
            } else {
                Color c = Color.web("#999999");
                e.main.setStroke(c);
                e.arrow1.setStroke(c);
                e.arrow2.setStroke(c);
            }
        }
    }

    // Kenar renklerini başlangıçtaki "default" haline döndürür
    private void resetEdgeColorsToDefault() {
        // Atıf (citation) kenarları: gri/beyaz
        for (EdgeView e : citationEdges) {
            Color c = Color.web("#999999"); // sen istersen Color.WHITE, 0.3 de yapabilirsin
            e.main.setStroke(c);
            e.arrow1.setStroke(c);
            e.arrow2.setStroke(c);
        }

        // ID sıralama kenarları: yeşil
        for (EdgeView e : idEdges) {
            Color c = Color.LIMEGREEN;
            e.main.setStroke(c);
            e.arrow1.setStroke(c);
            e.arrow2.setStroke(c);
        }
    }

    private void refreshStats() {
        if (analysisMode) {
            // analiz modunda betweenness sonuçlarını enterAnalysisMode yazıyor,
            // burada üzerine yazmayalım.
            return;
        }
        infoPanel.updateGlobalStats(Collections.unmodifiableSet(visibleNodes));
    }

    // ----------------------------------------------------
    // ANALİZ MODU
    // ----------------------------------------------------

    public void enterAnalysisMode() {
        if (analysisMode) return;

        analysisMode = true;

        // yönsüz görünüm: ID oklarını kapat, siyah okların uçlarını kaldır
        for (EdgeView e : idEdges) {
            e.main.setVisible(false);
            e.arrow1.setVisible(false);
            e.arrow2.setVisible(false);
        }
        for (EdgeView e : citationEdges) {
            boolean show = visibleNodes.contains(e.from) && visibleNodes.contains(e.to);
            e.main.setVisible(show);
            e.arrow1.setVisible(false);
            e.arrow2.setVisible(false);
        }

        // kırmızı / sarı renklendirmeyi temizle
        selected = null;
        hCore.clear();
        infoPanel.clearSelected();
        updateNodeColors();

        // Görünen alt-graf + yönsüz kenarlar için TAM SAYILI betweenness hesapla
        Map<Article, Integer> betVis = computeIntegerBetweennessOnVisible();

        infoPanel.showBetweennessResultsInteger(
                Collections.unmodifiableSet(visibleNodes),
                betVis
        );
    }


    // Sadece görünür alt-graf üzerinde k-core
    public void computeKCoreOnVisible(int k) {
        Set<Article> core = GraphMetrics.kCore(graph, k);
        core.retainAll(visibleNodes);
        setKCore(core);
    }

    public void setKCore(Set<Article> core) {
        kCoreSet.clear();
        if (core != null) {
            kCoreSet.addAll(core);
            visibleNodes.addAll(core);
        }
        updateVisibility();
        updateNodeColors();
        if (!analysisMode) {
            refreshStats();
        }
    }

    // Görünür düğümlerden oluşan yönsüz alt-graf için adjacency list
    private Map<Article, List<Article>> buildUndirectedAdjacencyForVisible() {
        Map<Article, List<Article>> adj = new HashMap<>();
        // set olsun ki contains hızlı olsun
        Set<Article> nodeSet = new HashSet<>(visibleNodes);

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

    // Ödev tanımına göre: her (s,t) çifti için, v en az bir
// en kısa yolun üzerindeyse +1. Sonuçlar TAM SAYI.
    public Map<Article, Integer> computeIntegerBetweennessOnVisible() {
        List<Article> nodes = new ArrayList<>(visibleNodes);
        int n = nodes.size();

        Map<Article, Integer> result = new HashMap<>();
        for (Article a : nodes) {
            result.put(a, 0);
        }

        if (n == 0) return result;

        Map<Article, List<Article>> adj = buildUndirectedAdjacencyForVisible();

        // Her kaynak için bir BFS
        for (int i = 0; i < n; i++) {
            Article s = nodes.get(i);

            // BFS için parent haritası (bir tane parent yeterli: tek bir shortest path)
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

            // s'den ulaşılan her t (i < j) için path'i geriye yürüt
            for (int j = i + 1; j < n; j++) {
                Article t = nodes.get(j);
                if (!parent.containsKey(t)) {
                    // s ile t arasında yol yok
                    continue;
                }

                Article cur = parent.get(t);
                while (cur != null && cur != s) {
                    // s ve t hariç aradaki düğümler +1 alır
                    result.put(cur, result.get(cur) + 1);
                    cur = parent.get(cur);
                }
            }
        }

        return result;
    }



    public Pane getRoot() {
        return root;
    }
}
