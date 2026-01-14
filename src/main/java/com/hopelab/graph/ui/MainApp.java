package com.hopelab.graph.ui;

import com.hopelab.graph.model.CitationGraph;
import com.hopelab.graph.service.GraphLoader;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane root;

    private CitationGraph graph;
    private GraphView graphView;
    private InfoPanel infoPanel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        stage.setTitle("Makale Graf Analiz Uygulaması");

        root = new BorderPane();
        root.setStyle("-fx-background-color: #202020;");

        // dosya secme ekranı
        showFileSelectScreen();

        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.show();
    }

    //json dosyası sec butonu
    private void showFileSelectScreen() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        Label label = new Label("Lütfen bir OpenAlex JSON makale dosyası seçin");
        label.setTextFill(Color.WHITE);

        Button chooseBtn = new Button("JSON Dosyası Seç");
        chooseBtn.setOnAction(e -> openJsonAndBuildGraph());

        box.getChildren().addAll(label, chooseBtn);

        root.setCenter(box);
        root.setRight(null); // InfoPanel yok, sadece seçim ekranı
    }



    //dosya secici ac, sectir, dosyayı graphloaderdeki ilgili fonksyonla oku, graphview ve infopanel olustur

    private void openJsonAndBuildGraph() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Makale JSON dosyası seç");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON dosyaları", "*.json")
        );

        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) {
            //secilmediyse
            return;
        }

        try {
            //jsonu oku
            this.graph = GraphLoader.loadFromFile(file);
        } catch (IOException ex) {
            showError("JSON dosyası okunamadı", ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            showError("JSON formatı beklenmedik", ex.getMessage());
            return;
        }

        //infopanel graphview olustur
        this.infoPanel = new InfoPanel(graph);
        this.graphView = new GraphView(graph, infoPanel);
        infoPanel.setGraphView(graphView);

        root.setCenter(graphView.getRoot());
        root.setRight(infoPanel.getRoot());
    }

    //hata cıkarsa mesaj
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hata");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}