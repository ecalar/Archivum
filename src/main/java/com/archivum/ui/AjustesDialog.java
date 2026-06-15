package com.archivum.ui;

import com.archivum.service.AjustesService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class AjustesDialog {

    private final AjustesService ajustesService;

    public AjustesDialog(AjustesService ajustesService) {
        this.ajustesService = ajustesService;
    }

    public void mostrar() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Configuración General del Sistema");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FFFFFF;");

        // Cabecera Principal
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon gearIcon = new FontIcon("fas-sliders-h");
        gearIcon.setIconSize(24);
        gearIcon.setIconColor(Color.web("#12261A"));
        Label titulo = new Label("Panel de Control General");
        titulo.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 20));
        titulo.setStyle("-fx-text-fill: #12261A;");
        headerBox.getChildren().addAll(gearIcon, titulo);

        GridPane grid = new GridPane();
        grid.setVgap(18);
        grid.setHgap(15);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // --- SECCIÓN: PROTECCIÓN Y MARCA DE AGUA ---
        Label subMarca = new Label("Seguridad y Copias de Salida");
        subMarca.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));
        subMarca.setStyle("-fx-text-fill: #CFA144;");
        grid.add(subMarca, 0, 0, 2, 1);

        Label lblNombre = new Label("Texto de la marca:");
        lblNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        TextField txtNombre = new TextField(ajustesService.getNombreInstitucion());
        txtNombre.setPrefWidth(280);

        Label lblMarcaActiva = new Label("Estado de protección:");
        lblMarcaActiva.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        CheckBox chkMarcaActiva = new CheckBox("Activar marca de agua en descargas");
        chkMarcaActiva.setSelected(ajustesService.isMarcaAguaActiva());

        Label lblTamano = new Label("Tamaño de fuente (pt):");
        lblTamano.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        ComboBox<Float> comboTamano = new ComboBox<>();
        comboTamano.getItems().addAll(30f, 36f, 45f, 54f, 64f);
        comboTamano.setValue(ajustesService.getMarcaAguaTamano());

        grid.add(lblNombre, 0, 1); grid.add(txtNombre, 1, 1);
        grid.add(lblMarcaActiva, 0, 2); grid.add(chkMarcaActiva, 1, 2);
        grid.add(lblTamano, 0, 3); grid.add(comboTamano, 1, 3);

        // --- SECCIÓN: MOTOR OCR Y RENDIMIENTO ---
        Label subOcr = new Label("Motor de Indexación Inteligente (OCR)");
        subOcr.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));
        subOcr.setStyle("-fx-text-fill: #CFA144;");
        grid.add(subOcr, 0, 4, 2, 1);

        Label lblOcrActivo = new Label("Procesamiento OCR:");
        lblOcrActivo.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        CheckBox chkOcrActivo = new CheckBox("Analizar texto al guardar (Requiere RAM)");
        chkOcrActivo.setSelected(ajustesService.isOcrActivo());

        Label lblIdioma = new Label("Idioma del motor:");
        lblIdioma.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        ComboBox<String> comboIdioma = new ComboBox<>();
        comboIdioma.getItems().addAll("spa", "lat", "eng");
        comboIdioma.setValue(ajustesService.getOcrIdioma());
        comboIdioma.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.equals("spa") ? "Español" : item.equals("lat") ? "Latín Clásico" : "Inglés");
            }
        });
        comboIdioma.setButtonCell(comboIdioma.getCellFactory().call(null));

        grid.add(lblOcrActivo, 0, 5); grid.add(chkOcrActivo, 1, 5);
        grid.add(lblIdioma, 0, 6); grid.add(comboIdioma, 1, 6);

        // --- SECCIÓN: ALMACENAMIENTO Y ALMANAQUE ---
        Label subAlmacen = new Label("Rutas de Almacenamiento");
        subAlmacen.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));
        subAlmacen.setStyle("-fx-text-fill: #CFA144;");
        grid.add(subAlmacen, 0, 7, 2, 1);

        Label lblBackup = new Label("Copias de seguridad:");
        lblBackup.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        HBox backupBox = new HBox(8);
        TextField txtBackup = new TextField(ajustesService.getRutaBackup());
        txtBackup.setPrefWidth(200);
        Button btnExaminar = new Button("...");
        btnExaminar.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Seleccionar Carpeta de Backups");
            File f = dc.showDialog(stage);
            if (f != null) txtBackup.setText(f.getAbsolutePath());
        });
        backupBox.getChildren().addAll(txtBackup, btnExaminar);
        grid.add(lblBackup, 0, 8); grid.add(backupBox, 1, 8);

        // Botonera de Cierre
        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        btnCancelar.setOnAction(e -> stage.close());

        Button btnGuardar = new Button("Guardar Ajustes");
        FontIcon saveIcon = new FontIcon("fas-save");
        saveIcon.setIconColor(Color.WHITE);
        btnGuardar.setGraphic(saveIcon);
        btnGuardar.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        btnGuardar.setOnAction(e -> {
            if (txtNombre.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El texto de la marca no puede estar vacío.").show();
                return;
            }
            ajustesService.setNombreInstitucion(txtNombre.getText().trim());
            ajustesService.setMarcaAguaActiva(chkMarcaActiva.isSelected());
            ajustesService.setMarcaAguaTamano(comboTamano.getValue());
            ajustesService.setOcrActivo(chkOcrActivo.isSelected());
            ajustesService.setOcrIdioma(comboIdioma.getValue());
            ajustesService.setRutaBackup(txtBackup.getText().trim());

            ajustesService.guardarAjustes();
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        botones.getChildren().addAll(spacer, btnCancelar, btnGuardar);

        root.getChildren().addAll(headerBox, new Separator(), grid, botones);

        Scene scene = new Scene(root, 550, -1);
        stage.setScene(scene);
        stage.showAndWait();
    }
}