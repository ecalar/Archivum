package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.service.DocumentoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentoFormDialog {

    private final DocumentoService service;
    private final Runnable onDocumentoCreado;
    private final Documento documentoAEditar;
    private Stage stage;

    // Campos del formulario
    private TextField tituloField;
    private ComboBox<TipoDocumento> tipoCombo;
    private DatePicker fechaPicker;
    private CheckBox soloAnioCheck;
    private TextField anioField;
    private TextArea descripcionArea;
    private TextField ubicacionField;
    private Label archivoLabel;
    private byte[] contenidoArchivo;
    private String nombreArchivo;
    private String tipoMime;
    private Label errorLabel;

    // Constructor para nuevo documento
    public DocumentoFormDialog(DocumentoService service, Runnable onDocumentoCreado) {
        this.service = service;
        this.onDocumentoCreado = onDocumentoCreado;
        this.documentoAEditar = null;
    }

    // Constructor para editar documento existente (Día 5)
    public DocumentoFormDialog(DocumentoService service, Documento docAEditar, Runnable onDocumentoEditado) {
        this.service = service;
        this.onDocumentoCreado = onDocumentoEditado;
        this.documentoAEditar = docAEditar;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(documentoAEditar == null ? "Nuevo documento" : "Editar documento");
        stage.setMinWidth(550);
        stage.setMinHeight(650);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        // Título
        Label tituloLabel = new Label("Título *");
        tituloLabel.setStyle("-fx-font-weight: bold;");
        tituloField = new TextField();
        tituloField.setPromptText("Ej: Acta fundacional de la hermandad");

        // Tipo
        Label tipoLabel = new Label("Tipo *");
        tipoLabel.setStyle("-fx-font-weight: bold;");
        tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll(TipoDocumento.values());
        tipoCombo.setPromptText("Seleccionar tipo...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);

        // Fecha
        Label fechaLabel = new Label("Fecha");
        fechaLabel.setStyle("-fx-font-weight: bold;");

        HBox fechaBox = new HBox(10);
        fechaPicker = new DatePicker();
        fechaPicker.setPromptText("DD/MM/AAAA");
        fechaPicker.setShowWeekNumbers(false);

        soloAnioCheck = new CheckBox("Solo conozco el año aproximado");
        anioField = new TextField();
        anioField.setPromptText("Año");
        anioField.setPrefWidth(80);
        anioField.setVisible(false);

        soloAnioCheck.setOnAction(e -> {
            boolean seleccionado = soloAnioCheck.isSelected();
            fechaPicker.setDisable(seleccionado);
            anioField.setVisible(seleccionado);
            if (seleccionado) {
                fechaPicker.setValue(null);
            } else {
                anioField.clear();
            }
        });

        fechaBox.getChildren().addAll(fechaPicker, soloAnioCheck, anioField);

        // Descripción
        Label descLabel = new Label("Descripción");
        descLabel.setStyle("-fx-font-weight: bold;");
        descripcionArea = new TextArea();
        descripcionArea.setPromptText("Describe el contenido o contexto del documento...");
        descripcionArea.setPrefRowCount(4);
        descripcionArea.setWrapText(true);

        // Ubicación física
        Label ubicacionLabel = new Label("Ubicación física del original");
        ubicacionLabel.setStyle("-fx-font-weight: bold;");
        ubicacionField = new TextField();
        ubicacionField.setPromptText("Ej: Estantería 3, Archivador A, Carpeta 2");

        // Archivo
        Label archivoTituloLabel = new Label("Archivo digital *");
        archivoTituloLabel.setStyle("-fx-font-weight: bold;");

        HBox archivoBox = new HBox(10);
        archivoBox.setAlignment(Pos.CENTER_LEFT);

        Button seleccionarBtn = new Button("Seleccionar archivo...");
        seleccionarBtn.setStyle("-fx-background-color: #4A0E17; -fx-text-fill: white;");
        seleccionarBtn.setOnAction(e -> seleccionarArchivo());

        archivoLabel = new Label("Ningún archivo seleccionado");
        archivoLabel.setStyle("-fx-text-fill: #6C6C6C;");

        archivoBox.getChildren().addAll(seleccionarBtn, archivoLabel);

        // Mensaje de error
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #C1292E; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        // Botones
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setOnAction(e -> stage.close());

        Button guardarBtn = new Button("Guardar");
        guardarBtn.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; -fx-font-weight: bold;");
        guardarBtn.setOnAction(e -> guardar());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        botonesBox.getChildren().addAll(spacer, cancelarBtn, guardarBtn);

        // Campos obligatorios
        Label obligatorioLabel = new Label("* Campos obligatorios");
        obligatorioLabel.setStyle("-fx-text-fill: #6C6C6C; -fx-font-size: 11px;");

        // Añadir todo al root
        root.getChildren().addAll(
                tituloLabel, tituloField,
                tipoLabel, tipoCombo,
                fechaLabel, fechaBox,
                descLabel, descripcionArea,
                ubicacionLabel, ubicacionField,
                archivoTituloLabel, archivoBox,
                errorLabel,
                botonesBox,
                obligatorioLabel
        );

        // Si es edición, cargar datos (Día 5)
        if (documentoAEditar != null) {
            cargarDatosDocumento();
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void seleccionarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos permitidos", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png")
        );

        File archivo = fileChooser.showOpenDialog(stage);
        if (archivo != null) {
            try {
                contenidoArchivo = Files.readAllBytes(archivo.toPath());
                nombreArchivo = archivo.getName();
                tipoMime = Files.probeContentType(archivo.toPath());

                if (tipoMime == null) {
                    // Fallback según extensión
                    String nombre = archivo.getName().toLowerCase();
                    if (nombre.endsWith(".pdf")) tipoMime = "application/pdf";
                    else if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) tipoMime = "image/jpeg";
                    else if (nombre.endsWith(".png")) tipoMime = "image/png";
                }

                String tamanio = formatearTamanio(contenidoArchivo.length);
                archivoLabel.setText(nombreArchivo + " (" + tamanio + ")");
                archivoLabel.setStyle("-fx-text-fill: #2D6A4F;");
                errorLabel.setVisible(false);

            } catch (IOException ex) {
                archivoLabel.setText("Error al leer el archivo");
                archivoLabel.setStyle("-fx-text-fill: #C1292E;");
            }
        }
    }

    private void guardar() {
        // Limpiar error anterior
        errorLabel.setVisible(false);

        // Validar campos obligatorios
        String titulo = tituloField.getText();
        TipoDocumento tipo = tipoCombo.getValue();
        LocalDate fecha = fechaPicker.getValue();
        Integer anio = null;

        if (soloAnioCheck.isSelected() && !anioField.getText().isBlank()) {
            try {
                anio = Integer.parseInt(anioField.getText().trim());
            } catch (NumberFormatException ex) {
                mostrarError("El año debe ser un número válido");
                return;
            }
        }

        String descripcion = descripcionArea.getText();
        String ubicacion = ubicacionField.getText();

        try {
            if (documentoAEditar == null) {
                // Nuevo documento
                service.crearDocumento(
                        titulo, tipo, fecha, anio, descripcion, ubicacion,
                        nombreArchivo, contenidoArchivo, tipoMime
                );
            }
            // La edición se implementará el Día 5

            if (onDocumentoCreado != null) {
                onDocumentoCreado.run();
            }
            stage.close();

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("Error al guardar el archivo: " + ex.getMessage());
        }
    }

    private void cargarDatosDocumento() {
        // Implementar Día 5
    }

    private void mostrarError(String mensaje) {
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
    }

    private String formatearTamanio(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}