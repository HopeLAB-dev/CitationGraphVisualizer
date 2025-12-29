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

import static java.lang.Math.*;

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

    //agac modu
    private boolean searchMode = false;

    //analiz modu ( betweenness , k-core decomposition )
    private boolean analysisMode = false;

    private final Map<Article, Integer> treeDepth = new HashMap<>();
    private final Map<Article, Integer> treeIndex = new HashMap<>();
    private final Map<Integer, Integer> depthNextIndex = new HashMap<>();

    private record EdgeView(Article from, Article to, Line main, Line arrow1, Line arrow2) {
    }

    private final List<EdgeView> citationEdges = new ArrayList<>();
    private final List<EdgeView> idEdges = new ArrayList<>();

    private Article selected;

    //zoompan
    private double scale = 1.0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean dragging = false;

    private static final double SEARCH_RADIUS_FACTOR = 4.0;

    //constructor
    public GraphView(CitationGraph graph, InfoPanel infoPanel) {
        this.graph = graph;
        this.infoPanel = infoPanel;

        root = new Pane();
        root.setPrefSize(900, 800);
        root.setStyle("-fx-background-color: #202020;");

        graphGroup = new Group();
        root.getChildren().add(graphGroup);


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

    //-----------------------------------

    /** GORSEL OLUSTURMA FONKSIYONLARI */

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

        for (int i = 0; i < n; i++) {
            Article a = list.get(i);

            int ringIndex = i / perRing;
            int indexInRing = i % perRing;
            int countInThisRing = Math.min(perRing, n - ringIndex * perRing);

            double R = baseR + ringIndex * ringGap;
            double angle = 2 * Math.PI * indexInRing / countInThisRing;

            double x = cx + R * Math.cos(angle);
            double y = cy + R * Math.sin(angle);

            double radius = 5 + 3 * log(a.getInNeighbors().size() + 1);

            Circle c = new Circle(x, y, radius);
            c.setFill(Color.GREY);
            c.setStroke(Color.BLACK);
            c.setVisible(false);

            baseRadiusMap.put(a, radius);
            defaultPosMap.put(a, new Point2D(x, y));

            String authorsText = a.getAuthors() == null || a.getAuthors().isEmpty()
                    ? "Bilinmiyor"
                    : String.join(", ", a.getAuthors());

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
                                authorsText,
                                a.getInNeighbors().size()
            );
            Tooltip.install(c, new Tooltip(tooltipText));


            // id yerleştirme
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

            //mouse hover
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



            //tıklayınca bilgi
            c.setOnMouseClicked(e -> handleNodeClick(a, e));


            nodeMap.put(a, c);
            labelMap.put(a, label);

            graphGroup.getChildren().add(c);
            graphGroup.getChildren().add(label);
        }
    }
    //ok olusturma (yönlü kenar)
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

    //olusturulan kenarları ciz
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

    //yesil( sıraya göre olan kenarlar) kenarları ciz
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


    //----------------------------------------------------

    /** ARAYUZ FONKSIYONLARI ARAMA SECME ZOOM KAYDIRMA*/
    public boolean focusArticleById(String rawId) {
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
        hCore.addAll(res.hCore());

        showIdEdges = false;
        visibleNodes.add(a);
        visibleNodes.addAll(res.hCore());

        highlightNewEdges(prevVisible);

        if (searchMode) {
            expandTreeFrom(a, res.hCore());
            updateAllEdges();
        }

        updateVisibility();
        updateNodeColors();
        refreshStats();
        infoPanel.updateSelected(a, res);
    }

    //secileni kaldır
    public void removeSelected() {
        if (selected == null) return;
        if (analysisMode) return; // analiz modunda silme yok

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

    //zoompan sagclickle

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

        // bos alaana tıklama secili dugumu kaldırır
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY &&
                    !(event.getTarget() instanceof Circle)) {

                selected = null;
                if (!analysisMode) {
                    // analiz modu ozel durum renkleri updatele
                    infoPanel.clearSelected();
                    updateNodeColors();
                }
            }
        });
    }
    // tıklayınca bilgi cıkması ( her modda aynı)
    private void handleNodeClick(Article a, MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (analysisMode) {
            //analiz ozel durum genisletme yok
            selected = a;
            HIndexService.Result res = HIndexService.computeFor(a);
            infoPanel.updateSelected(a, res);
        } else {
            //arama modunda genislet
            focusArticle(a, false);
        }

        e.consume();
    }

    //---------------------------------------------------------

    /** ONEMLI FONKSIYON-SİLME (GORUNUM SIFIRLAMA) */
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
    // kenar renklerini bastaki hale dondur
    private void resetEdgeColorsToDefault() {

        for (EdgeView e : citationEdges) {
            Color c = Color.web("#999999");
            e.main.setStroke(c);
            e.arrow1.setStroke(c);
            e.arrow2.setStroke(c);
        }


        for (EdgeView e : idEdges) {
            Color c = Color.LIMEGREEN;
            e.main.setStroke(c);
            e.arrow1.setStroke(c);
            e.arrow2.setStroke(c);
        }
    }
    //gorunumu sıfırlayınca ilk hale dön
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

    //------------------------------------------------------------


    /** ARAMA MODU AĞAÇ YAPISI FONKSIYONLARI */

    private void expandTreeFrom(Article center, Collection<Article> newCore) {
        if (center == null) return;

        double width  = root.getWidth()  > 0 ? root.getWidth()  : root.getPrefWidth();

        if (width <= 0)  width  = 900;


        double topMargin  = 80; //yukarıdan mesafe
        double rowGap     = 160;  //satır aralıgı

        double leftMargin = 150;    //soldan mesafe
        double colGap     = 260;  //sutun aralıgı

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
    //arama modunda node buyutme (görsel)
    private void applyRadiusScale(double factor) {
        //düğümlei büyüt ( aramamodu)
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

        //kenarları büyütme
        double edgeWidth = factor;
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
    // arma modu için genişletme( görsel)
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

    //agacta yeni olusan nodeları highlight et
    private void highlightNewEdges(Set<Article> prevVisible) {

        //sadece arama modda kullanılacak
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

    //------------------------------------------------------------


    /** ANALIZ MODUN FONKSIYONLARI */
    public void enterAnalysisMode() {
        if (analysisMode) return;

        analysisMode = true;
        System.out.println(visibleNodes.size());
        System.out.println(visibleNodes);
       //yönsüzleştirme
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

        //renklendirmeleri temizle, seçili node ı unselect yap
        selected = null;
        hCore.clear();
        infoPanel.clearSelected();
        updateNodeColors();

        //BETWEENNES FONKSIYONU CAGIR
        Map<Article, Integer> betVis =
                GraphMetrics.integerBetweenness(graph, visibleNodes);

        //info panelde betweeness sonuclarını goster
        infoPanel.showBetweennessResultsInteger(
                Collections.unmodifiableSet(visibleNodes),
                betVis
        );
    }


    //k-core cagır
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


    //-------------------------------------------------------------



    /** UPDATE FONKSYIONLARI */

    //ok updatei
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
    //yazılar ( id)
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

    //tüm kenarlar
    private void updateAllEdges() {
        for (EdgeView e : citationEdges) {
            updateEdgeGeometry(e);
        }
        for (EdgeView e : idEdges) {
            updateEdgeGeometry(e);
        }
    }

    //duugm renkleri
    private void updateNodeColors() {
        for (Map.Entry<Article, Circle> entry : nodeMap.entrySet()) {
            Article a = entry.getKey();
            Circle c = entry.getValue();

            c.setFill(getFillColorFor(a));
            c.setStroke(Color.BLACK);
        }
    }

    //gorunurlugu updatele
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
    private void refreshStats() {
        if (analysisMode) {

            return;
        }
        infoPanel.updateGlobalStats(Collections.unmodifiableSet(visibleNodes));
    }
    //dugum renkleri guncelleme
    private Color getFillColorFor(Article a) {
        //analiz modu ozel durum
        if (analysisMode) {
            return kCoreSet.contains(a)
                    ? Color.web("#4aa3ff")
                    : Color.LIGHTGRAY;
        }

        // normal mod
        if (a == selected) {
            return Color.RED;
        }
        if (hCore.contains(a)) {
            return Color.GOLD;
        }
        if (kCoreSet.contains(a)) {
            return Color.web("#4aa3ff");
        }

        return Color.LIGHTGRAY;
    }


    //----------------------------------------------------------------
    public Pane getRoot() {
        return root;
    }
}
