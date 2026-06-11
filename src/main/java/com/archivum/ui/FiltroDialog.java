package com.archivum.ui;

import com.archivum.model.TipoDocumento;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;

public class FiltroDialog {

    private Stage stage;
    private ComboBox<TipoDocumento> tipoCombo;
    private DatePicker desdePicker;
    private DatePicker hastaPicker;
    private TextField anioField;
    private boolean aplicar = false;

    public boolean mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Filtros de búsqueda");
        stage.setMinWidth(400);
        stage.setMinHeight(350);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titulo = new Label("🔍 Filtrar documentos");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4A0E17;");

        // Tipo
        Label tipoLabel = new Label("Tipo de documento:");
        tipoCombo = new ComboBox<>();
        tipoCombo.getItems().add(null);
        tipoCombo.getItems().addAll(TipoDocumento.values());
        tipoCombo.setPromptText("Todos los tipos");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setCellFactory(lv -> new ListCell<TipoDocumento>() {
            @Override
            protected void updateItem(TipoDocumento item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Todos los tipos" : item.getEtiqueta());
            }
        });
        tipoCombo.setButtonCell(new ListCell<TipoDocumento>() {
            @Override
            protected void updateItem(TipoDocumento item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Todos los tipos" : item.getEtiqueta());
            }
        });

        // Rango de fechas
        Label fechasLabel = new Label("Rango de fechas:");
        HBox fechasBox = new HBox(10);
        fechasBox.setAlignment(Pos.CENTER_LEFT);
        desdePicker = new DatePicker();
        desdePicker.setPromptText("Desde");
        Label separador = new Label("—");
        hastaPicker = new DatePicker();
        hastaPicker.setPromptText("Hasta");
        fechasBox.getChildren().addAll(desdePicker, separador, hastaPicker);

        // Año
        Label anioLabel = new Label("Año aproximado:");
        anioField = new TextField();
        anioField.setPromptText("Ej: 1814");
        anioField.setMaxWidth(100);

        // Botones
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);

        Button limpiarBtn = new Button("Limpiar filtros");
        limpiarBtn.setStyle("-fx-background-color: #6C6C6C; -fx-text-fill: white;");
        limpiarBtn.setOnAction(e -> {
            tipoCombo.setValue(null);
            desdePicker.setValue(null);
            hastaPicker.setValue(null);
            anioField.clear();
        });

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setOnAction(e -> stage.close());

        Button aplicarBtn = new Button("Aplicar filtros");
        aplicarBtn.setStyle("-fx-background-color: #4A0E17; -fx-text-fill: white; -fx-font-weight: bold;");
        aplicarBtn.setOnAction(e -> {
            aplicar = true;
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        botonesBox.getChildren().addAll(limpiarBtn, spacer, cancelarBtn, aplicarBtn);

        root.getChildren().addAll(titulo, tipoLabel, tipoCombo, fechasLabel, fechasBox, anioLabel, anioField, botonesBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return aplicar;
    }

    public TipoDocumento getTipo() { return tipoCombo.getValue(); }
    public LocalDate getDesde() { return desdePicker.getValue(); }
    public LocalDate getHasta() { return hastaPicker.getValue(); }
    public Integer getAnio() {
        try {
            return anioField.getText().isBlank() ? null : Integer.parseInt(anioField.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}