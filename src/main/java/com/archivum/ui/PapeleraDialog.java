package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.DocumentoService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PapeleraDialog {

    private final DocumentoService service;
    private final Stage parentStage;
    private final Runnable onDocumentoModificado;
    private Stage stage;

    public PapeleraDialog(DocumentoService service, Stage parentStage, Runnable onDocumentoModificado) {
        this.service = service;
        this.parentStage = parentStage;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Papelera de reciclaje");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF;");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon trashIcon = new FontIcon("fas-trash-alt");
        trashIcon.setIconSize(24);
        trashIcon.setIconColor(Color.web("#8A3636"));

        VBox headerText = new VBox(2);
        Label titulo = new Label("Papelera de reciclaje");
        titulo.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 18));
        titulo.setStyle("-fx-text-fill: #12261A;");
        Label subtitulo = new Label("Los documentos aquí pueden ser restaurados o eliminados para siempre.");
        subtitulo.setStyle("-fx-text-fill: #6B7A70; -fx-font-size: 12px;");
        headerText.getChildren().addAll(titulo, subtitulo);

        headerBox.getChildren().addAll(trashIcon, headerText);

        TableView<Documento> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setPrefHeight(350);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Documento, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitulo()));

        TableColumn<Documento, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTipo() != null ? data.getValue().getTipo().getEtiqueta() : ""));

        TableColumn<Documento, LocalDateTime> colFecha = new TableColumn<>("Eliminado el");
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getFechaEliminacion()));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime fecha, boolean empty) {
                super.updateItem(fecha, empty);
                if (empty || fecha == null) setText(null);
                else setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        tabla.getColumns().addAll(colTitulo, colTipo, colFecha);
        cargarPapelera(tabla);

        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnRestaurar = crearBoton("Restaurar selección", "fas-trash-restore", "#2A6B44");
        btnRestaurar.setOnAction(e -> {
            var sel = tabla.getSelectionModel().getSelectedItems();
            if (sel.isEmpty()) { alerta("Seleccione al menos un documento."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Restaurar " + sel.size() + " documento(s)?");
            confirm.initOwner(stage);
            confirm.setTitle("Restaurar");

            confirm.getDialogPane().setPrefWidth(450);
            confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            confirm.getDialogPane().applyCss();
            confirm.getDialogPane().layout();

            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                for (Documento d : sel) service.restaurarDocumento(d.getId());
                cargarPapelera(tabla);
                if (onDocumentoModificado != null) onDocumentoModificado.run();
            }
        });

        Button btnEliminar = crearBoton("Eliminar definitivamente", "fas-times-circle", "#8A3636");
        btnEliminar.setOnAction(e -> {
            var sel = tabla.getSelectionModel().getSelectedItems();
            if (sel.isEmpty()) { alerta("Seleccione al menos un documento."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar " + sel.size() + " documento(s) para siempre?");
            confirm.initOwner(stage);
            confirm.setTitle("Eliminar");

            confirm.getDialogPane().setPrefWidth(450);
            confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            confirm.getDialogPane().applyCss();
            confirm.getDialogPane().layout();

            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                TextInputDialog input = new TextInputDialog();
                input.initOwner(stage);
                input.setTitle("Confirmación de seguridad");
                input.setHeaderText("Esta acción es irreversible.\nEscriba ELIMINAR para confirmar.");

                input.getDialogPane().setPrefWidth(450);
                input.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                input.getDialogPane().applyCss();
                input.getDialogPane().layout();

                if (input.showAndWait().map(t -> "ELIMINAR".equals(t.trim())).orElse(false)) {
                    for (Documento d : sel) service.eliminarDefinitivamente(d.getId());
                    cargarPapelera(tabla);
                    if (onDocumentoModificado != null) onDocumentoModificado.run();
                }
            }
        });

        Button btnVaciar = crearBoton("Vaciar papelera", "fas-dumpster", "#611818");
        btnVaciar.setOnAction(e -> {
            List<Documento> todos = service.obtenerPapelera();
            if (todos.isEmpty()) { alerta("La papelera ya está vacía."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar TODOS los documentos permanentemente?");
            confirm.initOwner(stage);

            confirm.getDialogPane().setPrefWidth(450);
            confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            confirm.getDialogPane().applyCss();
            confirm.getDialogPane().layout();

            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                TextInputDialog input = new TextInputDialog();
                input.initOwner(stage);
                input.setTitle("Confirmación extrema");
                input.setHeaderText("¡Atención! Perderá todos los documentos.\nEscriba VACIAR para confirmar.");

                input.getDialogPane().setPrefWidth(450);
                input.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                input.getDialogPane().applyCss();
                input.getDialogPane().layout();

                if (input.showAndWait().map(t -> "VACIAR".equals(t.trim())).orElse(false)) {
                    for (Documento d : todos) service.eliminarDefinitivamente(d.getId());
                    cargarPapelera(tabla);
                    if (onDocumentoModificado != null) onDocumentoModificado.run();
                }
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> stage.close());

        botones.getChildren().addAll(btnRestaurar, btnEliminar, spacer, btnVaciar, btnCerrar);
        root.getChildren().addAll(headerBox, new Separator(), tabla, botones);

        Scene scene = new Scene(root, 800, 550);
        stage.setScene(scene);

        stage.setMinWidth(800);
        stage.setMinHeight(550);

        stage.showAndWait();
    }

    private Button crearBoton(String texto, String iconCode, String colorHex) {
        Button btn = new Button(texto);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(14);
        icon.setIconColor(Color.WHITE);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(8);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        return btn;
    }

    private void cargarPapelera(TableView<Documento> tabla) {
        tabla.getItems().setAll(service.obtenerPapelera());
    }

    private void alerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg);
        alert.initOwner(stage);
        alert.setHeaderText(null);

        alert.getDialogPane().setPrefWidth(450);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().applyCss();
        alert.getDialogPane().layout();

        alert.showAndWait();
    }
}