package com.hopelab.graph;

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

    // İstersen referans tutmak için:
    private CitationGraph graph;
    private GraphView graphView;
    private InfoPanel infoPanel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        stage.setTitle("Makale Graf Analiz Uygulaması");

        root = new BorderPane();
        root.setStyle("-fx-background-color: #202020;");

        // Uygulama ilk açıldığında dosya seçme ekranı gelsin
        showFileSelectScreen();

        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Başlangıç ekranı: ortada yazı + "JSON dosyası seç" butonu
     */
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

    /**
     * FileChooser aç, JSON dosyasını seçtir, GraphLoader.loadFromFile ile oku,
     * sonra GraphView + InfoPanel’i oluştur.
     */
    private void openJsonAndBuildGraph() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Makale JSON dosyası seç");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON dosyaları", "*.json")
        );

        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) {
            // kullanıcı iptal etti
            return;
        }

        try {
            // MANUEL JSON PARSER’LI GraphLoader:
            this.graph = GraphLoader.loadFromFile(file);
        } catch (IOException ex) {
            showError("JSON dosyası okunamadı", ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            showError("JSON formatı beklenmedik", ex.getMessage());
            return;
        }

        // Normalde eskiden yaptığımız kurulum:
        this.infoPanel = new InfoPanel(graph);
        this.graphView = new GraphView(graph, infoPanel);
        infoPanel.setGraphView(graphView);

        root.setCenter(graphView.getRoot());
        root.setRight(infoPanel.getRoot());
    }

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
