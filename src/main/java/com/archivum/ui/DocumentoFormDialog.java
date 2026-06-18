package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.service.DocumentoService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;

public class DocumentoFormDialog {

    private final DocumentoService service;
    private final Stage parentStage;
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
    private Button guardarBtn;
    private Button cancelarBtn;

    public DocumentoFormDialog(DocumentoService service, Stage parentStage, Documento docAEditar, Runnable onDocumentoModificado) {
        this.service = service;
        this.parentStage = parentStage;
        this.documentoAEditar = docAEditar;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(documentoAEditar == null ? "Nuevo documento" : "Editar documento");

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FFFFFF;");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon headerIcon = new FontIcon(documentoAEditar == null ? "fas-file-medical" : "fas-edit");
        headerIcon.setIconSize(22);
        headerIcon.setIconColor(Color.web("#2A6B44"));
        Label headerLabel = new Label(documentoAEditar == null ? "Registrar nuevo documento" : "Modificar documento");
        headerLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 18));
        headerLabel.setStyle("-fx-text-fill: #12261A;");
        headerBox.getChildren().addAll(headerIcon, headerLabel);

        tituloField = new TextField();
        tituloField.setPromptText("Título del documento");

        tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll(TipoDocumento.values());
        tipoCombo.setPromptText("Seleccionar tipo...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);

        HBox fechaBox = new HBox(12);
        fechaPicker = new DatePicker();
        soloAnioCheck = new CheckBox("Solo año aproximado");
        soloAnioCheck.setStyle("-fx-text-fill: #4D6B58;");
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

        descripcionArea = new TextArea();
        descripcionArea.setPromptText("Descripción detallada...");
        descripcionArea.setPrefRowCount(4);
        descripcionArea.setWrapText(true);

        ubicacionField = new TextField();
        ubicacionField.setPromptText("Ubicación física del original (Ej: Estante 3, Caja 12)");

        HBox archivoBox = new HBox(12);
        archivoBox.setAlignment(Pos.CENTER_LEFT);

        Button seleccionarBtn = new Button("Seleccionar archivo...");
        FontIcon uploadIcon = new FontIcon("fas-upload");
        uploadIcon.setIconColor(Color.web("#12261A"));
        seleccionarBtn.setGraphic(uploadIcon);
        seleccionarBtn.setStyle("-fx-background-color: #FAFCFB; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        seleccionarBtn.setOnAction(e -> seleccionarArchivo());

        archivoLabel = new Label("Ningún archivo seleccionado");
        archivoLabel.setStyle("-fx-text-fill: #6B7A70;");
        archivoBox.getChildren().addAll(seleccionarBtn, archivoLabel);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        HBox botonesBox = new HBox(12);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(10, 0, 0, 0));

        cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelarBtn.setOnAction(e -> stage.close());

        guardarBtn = new Button("Guardar Documento");
        FontIcon saveIcon = new FontIcon("fas-save");
        saveIcon.setIconColor(Color.WHITE);
        guardarBtn.setGraphic(saveIcon);
        guardarBtn.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        guardarBtn.setOnAction(e -> guardar());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        botonesBox.getChildren().addAll(spacer, cancelarBtn, guardarBtn);

        root.getChildren().addAll(
                headerBox, new Separator(),
                crearLabelRequerido("Título del documento"), tituloField,
                crearLabelRequerido("Tipo de documento"), tipoCombo,
                crearLabel("Fecha de emisión o registro"), fechaBox,
                crearLabel("Descripción"), descripcionArea,
                crearLabel("Ubicación física"), ubicacionField,
                crearLabelRequerido("Archivo digital asociado"), archivoBox,
                errorLabel, botonesBox
        );

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
                archivoLabel.setStyle("-fx-text-fill: #2A6B44; -fx-font-weight: bold;");
            }
        }

        Scene scene = new Scene(root, 550, 680);
        stage.setScene(scene);

        stage.setMinWidth(550);
        stage.setMinHeight(680);

        stage.showAndWait();
    }

    private Label crearLabel(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        return l;
    }

    private Label crearLabelRequerido(String texto) {
        Label l = new Label(texto + " *");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #12261A;");
        return l;
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
                archivoLabel.setStyle("-fx-text-fill: #2A6B44; -fx-font-weight: bold;");
            } catch (IOException ex) {
                archivoLabel.setText("Error al leer archivo");
                archivoLabel.setStyle("-fx-text-fill: #E53E3E;");
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

        guardarBtn.setDisable(true);
        cancelarBtn.setDisable(true);
        errorLabel.setText("Guardando documento en el sistema...");
        errorLabel.setStyle("-fx-text-fill: #2A6B44; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        Integer finalAnio = anio;

        Task<Documento> task = new Task<>() {
            @Override
            protected Documento call() throws Exception {
                if (documentoAEditar == null) {
                    return service.crearDocumento(titulo, tipo, fecha, finalAnio,
                            descripcionArea.getText(), ubicacionField.getText(),
                            nombreArchivo, contenidoArchivo, tipoMime);
                } else {
                    Documento doc = service.actualizarDocumento(documentoAEditar.getId(),
                            titulo, tipo, fecha, finalAnio,
                            descripcionArea.getText(), ubicacionField.getText());
                    if (contenidoArchivo != null) {
                        service.reemplazarArchivo(documentoAEditar.getId(),
                                nombreArchivo, contenidoArchivo, tipoMime);
                    }
                    return doc;
                }
            }
        };

        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            if (onDocumentoModificado != null) onDocumentoModificado.run();
            stage.close();
        }));

        task.setOnFailed(ev -> Platform.runLater(() -> {
            guardarBtn.setDisable(false);
            cancelarBtn.setDisable(false);
            Throwable ex = task.getException();
            errorLabel.setText("Error: " + ex.getMessage());
            errorLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
            errorLabel.setVisible(true);
        }));

        new Thread(task).start();
    }

    private String formatearTamanio(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}