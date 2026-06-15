package com.archivum.ui;

import com.archivum.model.TipoDocumento;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

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

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FFFFFF;");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon filterIcon = new FontIcon("fas-filter");
        filterIcon.setIconSize(20);
        filterIcon.setIconColor(Color.web("#12261A"));

        Label titulo = new Label("Filtrar documentos");
        titulo.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 18));
        titulo.setStyle("-fx-text-fill: #12261A;");
        headerBox.getChildren().addAll(filterIcon, titulo);

        // Tipo
        Label tipoLabel = new Label("Tipo de documento:");
        tipoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
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
        fechasLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        HBox fechasBox = new HBox(12);
        fechasBox.setAlignment(Pos.CENTER_LEFT);
        desdePicker = new DatePicker();
        desdePicker.setPromptText("Desde");
        Label separador = new Label("—");
        separador.setStyle("-fx-text-fill: #6B7A70;");
        hastaPicker = new DatePicker();
        hastaPicker.setPromptText("Hasta");
        fechasBox.getChildren().addAll(desdePicker, separador, hastaPicker);

        // Año
        Label anioLabel = new Label("Año aproximado:");
        anioLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        anioField = new TextField();
        anioField.setPromptText("Ej: 1814");
        anioField.setMaxWidth(100);

        // Botones
        HBox botonesBox = new HBox(12);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));

        Button limpiarBtn = new Button("Limpiar");
        FontIcon eraserIcon = new FontIcon("fas-eraser");
        eraserIcon.setIconColor(Color.web("#6B7A70"));
        limpiarBtn.setGraphic(eraserIcon);
        limpiarBtn.setStyle("-fx-background-color: #FAFCFB; -fx-text-fill: #4D6B58; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        limpiarBtn.setOnAction(e -> {
            tipoCombo.setValue(null);
            desdePicker.setValue(null);
            hastaPicker.setValue(null);
            anioField.clear();
        });

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> stage.close());

        Button aplicarBtn = new Button("Aplicar filtros");
        FontIcon checkIcon = new FontIcon("fas-check");
        checkIcon.setIconColor(Color.WHITE);
        aplicarBtn.setGraphic(checkIcon);
        aplicarBtn.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        aplicarBtn.setOnAction(e -> {
            aplicar = true;
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        botonesBox.getChildren().addAll(limpiarBtn, spacer, cancelarBtn, aplicarBtn);

        root.getChildren().addAll(headerBox, new Separator(), tipoLabel, tipoCombo, fechasLabel, fechasBox, anioLabel, anioField, botonesBox);

        Scene scene = new Scene(root, 450, -1);
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