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

public class DocumentoFormDialog {

    private final DocumentoService service;
    private final Runnable onDocumentoModificado;
    private final Documento documentoAEditar;
    private Stage stage;

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

    public DocumentoFormDialog(DocumentoService service, Documento docAEditar, Runnable onDocumentoModificado) {
        this.service = service;
        this.documentoAEditar = docAEditar;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(documentoAEditar == null ? "Nuevo documento" : "Editar documento");
        stage.setMinWidth(550);
        stage.setMinHeight(650);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        // Título
        tituloField = new TextField();
        tituloField.setPromptText("Título del documento");

        // Tipo
        tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll(TipoDocumento.values());
        tipoCombo.setPromptText("Seleccionar tipo...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);

        // Fecha
        HBox fechaBox = new HBox(10);
        fechaPicker = new DatePicker();
        soloAnioCheck = new CheckBox("Solo año aproximado");
        anioField = new TextField();
        anioField.setPromptText("Año");
        anioField.setPrefWidth(80);
        anioField.setVisible(false);

        soloAnioCheck.setOnAction(e -> {
            fechaPicker.setDisable(soloAnioCheck.isSelected());
            anioField.setVisible(soloAnioCheck.isSelected());
            if (soloAnioCheck.isSelected()) fechaPicker.setValue(null);
            else anioField.clear();
        });

        fechaBox.getChildren().addAll(fechaPicker, soloAnioCheck, anioField);

        // Descripción
        descripcionArea = new TextArea();
        descripcionArea.setPromptText("Descripción...");
        descripcionArea.setPrefRowCount(4);
        descripcionArea.setWrapText(true);

        // Ubicación
        ubicacionField = new TextField();
        ubicacionField.setPromptText("Ubicación física del original");

        // Archivo
        HBox archivoBox = new HBox(10);
        archivoBox.setAlignment(Pos.CENTER_LEFT);
        Button seleccionarBtn = new Button("Seleccionar archivo...");
        seleccionarBtn.setOnAction(e -> seleccionarArchivo());
        archivoLabel = new Label("Ningún archivo seleccionado");
        archivoBox.getChildren().addAll(seleccionarBtn, archivoLabel);

        // Error
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
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

        // Construir layout
        root.getChildren().addAll(
                new Label("Título *"), tituloField,
                new Label("Tipo *"), tipoCombo,
                new Label("Fecha"), fechaBox,
                new Label("Descripción"), descripcionArea,
                new Label("Ubicación física"), ubicacionField,
                new Label("Archivo digital *"), archivoBox,
                errorLabel, botonesBox,
                new Label("* Campos obligatorios")
        );

        // Cargar datos si es edición
        if (documentoAEditar != null) {
            tituloField.setText(documentoAEditar.getTitulo());
            tipoCombo.setValue(documentoAEditar.getTipo());
            if (documentoAEditar.getFechaExacta() != null) {
                fechaPicker.setValue(documentoAEditar.getFechaExacta());
            } else if (documentoAEditar.getAnioAproximado() != null) {
                soloAnioCheck.setSelected(true);
                fechaPicker.setDisable(true);
                anioField.setVisible(true);
                anioField.setText(String.valueOf(documentoAEditar.getAnioAproximado()));
            }
            if (documentoAEditar.getDescripcion() != null) descripcionArea.setText(documentoAEditar.getDescripcion());
            if (documentoAEditar.getUbicacionFisica() != null) ubicacionField.setText(documentoAEditar.getUbicacionFisica());
            if (documentoAEditar.getNombreArchivo() != null) {
                archivoLabel.setText(documentoAEditar.getNombreArchivo() + " (se mantendrá si no cambia)");
            }
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void seleccionarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF e imágenes", "*.pdf", "*.jpg", "*.jpeg", "*.png")
        );
        File archivo = fileChooser.showOpenDialog(stage);
        if (archivo != null) {
            try {
                contenidoArchivo = Files.readAllBytes(archivo.toPath());
                nombreArchivo = archivo.getName();
                tipoMime = Files.probeContentType(archivo.toPath());
                if (tipoMime == null) {
                    String n = archivo.getName().toLowerCase();
                    if (n.endsWith(".pdf")) tipoMime = "application/pdf";
                    else if (n.endsWith(".jpg") || n.endsWith(".jpeg")) tipoMime = "image/jpeg";
                    else if (n.endsWith(".png")) tipoMime = "image/png";
                }
                archivoLabel.setText(nombreArchivo + " (" + formatearTamanio(contenidoArchivo.length) + ")");
                archivoLabel.setStyle("-fx-text-fill: green;");
            } catch (IOException ex) {
                archivoLabel.setText("Error al leer archivo");
            }
        }
    }

    private void guardar() {
        errorLabel.setVisible(false);
        String titulo = tituloField.getText();
        TipoDocumento tipo = tipoCombo.getValue();
        LocalDate fecha = fechaPicker.getValue();
        Integer anio = null;

        if (soloAnioCheck.isSelected() && !anioField.getText().isBlank()) {
            try {
                anio = Integer.parseInt(anioField.getText().trim());
            } catch (NumberFormatException e) {
                errorLabel.setText("Año no válido");
                errorLabel.setVisible(true);
                return;
            }
        }

        try {
            if (documentoAEditar == null) {
                service.crearDocumento(titulo, tipo, fecha, anio,
                        descripcionArea.getText(), ubicacionField.getText(),
                        nombreArchivo, contenidoArchivo, tipoMime);
            } else {
                service.actualizarDocumento(documentoAEditar.getId(),
                        titulo, tipo, fecha, anio,
                        descripcionArea.getText(), ubicacionField.getText());
                if (contenidoArchivo != null) {
                    service.reemplazarArchivo(documentoAEditar.getId(),
                            nombreArchivo, contenidoArchivo, tipoMime);
                }
            }
            if (onDocumentoModificado != null) onDocumentoModificado.run();
            stage.close();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
        }
    }

    private String formatearTamanio(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}