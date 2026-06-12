package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.DocumentoService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PapeleraDialog {

    private final DocumentoService service;
    private final Runnable onDocumentoModificado;

    public PapeleraDialog(DocumentoService service, Runnable onDocumentoModificado) {
        this.service = service;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        // Tabla
        TableView<Documento> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setPrefWidth(700);
        tabla.setPrefHeight(350);

        TableColumn<Documento, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitulo()));
        colTitulo.setPrefWidth(300);

        TableColumn<Documento, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTipo() != null ? data.getValue().getTipo().getEtiqueta() : ""));
        colTipo.setPrefWidth(120);

        TableColumn<Documento, LocalDateTime> colFecha = new TableColumn<>("Eliminado el");
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getFechaEliminacion()));
        colFecha.setPrefWidth(180);
        colFecha.setCellFactory(col -> new TableCell<Documento, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime fecha, boolean empty) {
                super.updateItem(fecha, empty);
                if (empty || fecha == null) {
                    setText(null);
                } else {
                    setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
            }
        });

        tabla.getColumns().addAll(colTitulo, colTipo, colFecha);
        cargarPapelera(tabla);

        // Botones
        Button btnRestaurar = new Button("Restaurar");
        btnRestaurar.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white;");
        btnRestaurar.setOnAction(e -> {
            var sel = tabla.getSelectionModel().getSelectedItems();
            if (sel.isEmpty()) {
                alerta("Seleccione al menos un documento.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Restaurar " + sel.size() + " documento(s)?");
            confirm.setTitle("Restaurar");
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                for (Documento d : sel) service.restaurarDocumento(d.getId());
                cargarPapelera(tabla);
                if (onDocumentoModificado != null) onDocumentoModificado.run();
            }
        });

        Button btnEliminar = new Button("Eliminar definitivamente");
        btnEliminar.setStyle("-fx-background-color: #C1292E; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> {
            var sel = tabla.getSelectionModel().getSelectedItems();
            if (sel.isEmpty()) {
                alerta("Seleccione al menos un documento.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar " + sel.size() + " documento(s) para siempre?");
            confirm.setTitle("Eliminar");
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                TextInputDialog input = new TextInputDialog();
                input.setTitle("Confirmación");
                input.setHeaderText("Escriba ELIMINAR para confirmar");
                if (input.showAndWait().map(t -> "ELIMINAR".equals(t.trim())).orElse(false)) {
                    for (Documento d : sel) service.eliminarDefinitivamente(d.getId());
                    cargarPapelera(tabla);
                    if (onDocumentoModificado != null) onDocumentoModificado.run();
                }
            }
        });

        Button btnVaciar = new Button("Vaciar papelera");
        btnVaciar.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white;");
        btnVaciar.setOnAction(e -> {
            List<Documento> todos = service.obtenerPapelera();
            if (todos.isEmpty()) { alerta("Papelera vacía."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar TODOS los documentos?");
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                TextInputDialog input = new TextInputDialog();
                input.setTitle("Confirmación");
                input.setHeaderText("Escriba VACIAR para confirmar");
                if (input.showAndWait().map(t -> "VACIAR".equals(t.trim())).orElse(false)) {
                    for (Documento d : todos) service.eliminarDefinitivamente(d.getId());
                    cargarPapelera(tabla);
                    if (onDocumentoModificado != null) onDocumentoModificado.run();
                }
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> {
            // Cerramos el diálogo obteniendo la ventana desde el botón
            ((Stage) btnCerrar.getScene().getWindow()).close();
        });

        HBox botones = new HBox(10, btnRestaurar, btnEliminar, btnVaciar, btnCerrar);
        botones.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, new Label("Documentos eliminados. Seleccione y elija una acción."), tabla, botones);
        root.setPadding(new Insets(15));

        // Diálogo personalizado
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Papelera");
        stage.setScene(new javafx.scene.Scene(root));
        stage.setWidth(750);
        stage.setHeight(500);
        stage.showAndWait();
    }

    private void cargarPapelera(TableView<Documento> tabla) {
        tabla.getItems().setAll(service.obtenerPapelera());
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}