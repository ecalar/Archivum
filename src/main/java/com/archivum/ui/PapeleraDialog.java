package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.DocumentoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PapeleraDialog {

    private final DocumentoService service;
    private final Runnable onDocumentoModificado;
    private Stage stage;
    private TableView<Documento> tabla;

    public PapeleraDialog(DocumentoService service, Runnable onDocumentoModificado) {
        this.service = service;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Papelera");
        stage.setMinWidth(650);
        stage.setMinHeight(450);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // Título
        Label titulo = new Label("🗑 Papelera");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4A0E17;");

        Label descripcion = new Label("Documentos eliminados. Puede restaurarlos o eliminarlos definitivamente.");
        descripcion.setStyle("-fx-text-fill: #6C6C6C;");
        descripcion.setWrapText(true);

        // Tabla
        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Documento, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTitulo.setPrefWidth(250);

        TableColumn<Documento, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(120);

        TableColumn<Documento, LocalDateTime> colFechaEliminacion = new TableColumn<>("Eliminado el");
        colFechaEliminacion.setCellValueFactory(new PropertyValueFactory<>("fechaEliminacion"));
        colFechaEliminacion.setPrefWidth(180);
        colFechaEliminacion.setCellFactory(col -> new TableCell<Documento, LocalDateTime>() {
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

        tabla.getColumns().addAll(colTitulo, colTipo, colFechaEliminacion);

        // Botones
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_LEFT);

        Button restaurarBtn = new Button("🔄 Restaurar");
        restaurarBtn.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; -fx-font-weight: bold;");
        restaurarBtn.setOnAction(e -> restaurarSeleccionado());

        Button eliminarDefBtn = new Button("❌ Eliminar definitivamente");
        eliminarDefBtn.setStyle("-fx-background-color: #C1292E; -fx-text-fill: white; -fx-font-weight: bold;");
        eliminarDefBtn.setOnAction(e -> eliminarDefinitivamente());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setOnAction(e -> stage.close());

        botonesBox.getChildren().addAll(restaurarBtn, eliminarDefBtn, spacer, cerrarBtn);

        // Contador
        Label contadorLabel = new Label();
        contadorLabel.setStyle("-fx-text-fill: #6C6C6C;");

        // Añadir todo
        root.getChildren().addAll(titulo, descripcion, tabla, botonesBox, contadorLabel);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // Cargar datos
        cargarPapelera();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void cargarPapelera() {
        List<Documento> documentos = service.obtenerPapelera();
        tabla.getItems().setAll(documentos);
    }

    private void restaurarSeleccionado() {
        Documento seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un documento para restaurar.");
            return;
        }

        try {
            service.restaurarDocumento(seleccionado.getId());
            cargarPapelera();
            if (onDocumentoModificado != null) {
                onDocumentoModificado.run();
            }
        } catch (Exception ex) {
            mostrarAlerta("Error al restaurar: " + ex.getMessage());
        }
    }

    private void eliminarDefinitivamente() {
        Documento seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un documento para eliminar definitivamente.");
            return;
        }

        // Primera confirmación
        Alert confirmacion1 = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion1.setTitle("Eliminar definitivamente");
        confirmacion1.setHeaderText("¿Eliminar \"" + seleccionado.getTitulo() + "\" para siempre?");
        confirmacion1.setContentText("Esta acción NO se puede deshacer.\nEl archivo digital también se borrará.");

        ButtonType btnEliminar = new ButtonType("Eliminar definitivamente");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion1.getButtonTypes().setAll(btnEliminar, btnCancelar);

        Optional<ButtonType> resultado = confirmacion1.showAndWait();
        if (resultado.isEmpty() || resultado.get() != btnEliminar) {
            return;
        }

        // Segunda confirmación: escribir RESTAURAR
        TextInputDialog confirmacion2 = new TextInputDialog();
        confirmacion2.setTitle("Confirmación final");
        confirmacion2.setHeaderText("Escriba ELIMINAR para confirmar");
        confirmacion2.setContentText("Palabra:");

        Optional<String> texto = confirmacion2.showAndWait();
        if (texto.isPresent() && "ELIMINAR".equals(texto.get().trim())) {
            try {
                service.eliminarDefinitivamente(seleccionado.getId());
                cargarPapelera();
                if (onDocumentoModificado != null) {
                    onDocumentoModificado.run();
                }
            } catch (Exception ex) {
                mostrarAlerta("Error al eliminar: " + ex.getMessage());
            }
        } else if (texto.isPresent()) {
            mostrarAlerta("Texto incorrecto. La eliminación fue cancelada.");
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Archivum");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}