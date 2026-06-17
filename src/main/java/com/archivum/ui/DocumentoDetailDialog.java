package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.AuthService;
import com.archivum.service.DocumentoService;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DocumentoDetailDialog {

    private final DocumentoService service;
    private final AuthService authService; // Campo añadido para la verificación de seguridad
    private final Documento documento;
    private final Runnable onDocumentoModificado;
    private Stage stage;

    // Constructor sincronizado con 4 parámetros
    public DocumentoDetailDialog(DocumentoService service, AuthService authService, Documento documento, Runnable onDocumentoModificado) {
        this.service = service;
        this.authService = authService;
        this.documento = documento;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(documento.getTitulo());

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FFFFFF;");

        HBox tituloBox = new HBox(12);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon tipoIcono = new FontIcon(obtenerIconoClase(documento.getTipo().name()));
        tipoIcono.setIconSize(24);
        tipoIcono.setIconColor(Color.web("#CFA144"));

        Label tituloLabel = new Label(documento.getTitulo());
        tituloLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 20));
        tituloLabel.setStyle("-fx-text-fill: #12261A;");
        tituloLabel.setWrapText(true);
        tituloBox.getChildren().addAll(tipoIcono, tituloLabel);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));

        agregarFila(grid, 0, "Tipo:", documento.getTipo().getEtiqueta());
        agregarFila(grid, 1, "Fecha:", formatearFecha());
        agregarFila(grid, 2, "Ubicación:", valorONoDisponible(documento.getUbicacionFisica()));
        agregarFila(grid, 3, "Subido:", documento.getFechaSubida() != null
                ? documento.getFechaSubida().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Desconocido");

        Label descTitulo = new Label("Descripción:");
        descTitulo.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        descTitulo.setStyle("-fx-text-fill: #12261A;");

        String descripcion = documento.getDescripcion() != null && !documento.getDescripcion().isBlank()
                ? documento.getDescripcion()
                : "Sin descripción";
        Label descContenido = new Label(descripcion);
        descContenido.setWrapText(true);
        descContenido.setStyle("-fx-text-fill: #4D6B58; -fx-line-spacing: 0.3em;");

        HBox archivoBox = new HBox(15);
        archivoBox.setAlignment(Pos.CENTER_LEFT);
        archivoBox.setPadding(new Insets(15));
        archivoBox.setStyle("-fx-background-color: #FAFCFB; -fx-background-radius: 8px; -fx-border-color: #D1DED5; -fx-border-radius: 8px;");

        FontIcon archivoIcono = new FontIcon("fas-file-pdf");
        archivoIcono.setIconSize(28);
        archivoIcono.setIconColor(Color.web("#2A6B44"));

        VBox archivoInfo = new VBox(4);
        Label archivoNombre = new Label(documento.getNombreArchivo());
        archivoNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #12261A;");
        Label archivoTamano = new Label(formatearTamanio(documento.getTamanioBytes()));
        archivoTamano.setStyle("-fx-text-fill: #6B7A70; -fx-font-size: 11px;");
        archivoInfo.getChildren().addAll(archivoNombre, archivoTamano);

        archivoBox.getChildren().addAll(archivoIcono, archivoInfo);

        HBox botonesBox = new HBox(12);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);
        botonesBox.setPadding(new Insets(15, 0, 0, 0));

        Button abrirBtn = crearBoton("Abrir visor", "fas-folder-open", "#12261A");
        abrirBtn.setOnAction(e -> abrirArchivo());

        Button descargarBtn = crearBoton("Descargar", "fas-download", "#2A6B44");
        descargarBtn.setOnAction(e -> descargarDocumento());

        // Botones con control de roles asignados dinámicamente
        Button editarBtn = crearBoton("Editar", "fas-edit", "#CFA144");
        editarBtn.setStyle("-fx-background-color: rgba(207, 161, 68, 0.15); -fx-text-fill: #CFA144; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        editarBtn.setOnAction(e -> editarDocumento());
        editarBtn.setVisible(authService.isAdministrador());

        Button eliminarBtn = crearBoton("Eliminar", "fas-trash-alt", "#8A3636");
        eliminarBtn.setOnAction(e -> eliminarDocumento());
        eliminarBtn.setVisible(authService.isAdministrador());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cerrarBtn = crearBoton("Cerrar", "fas-times", "#94A89A");
        cerrarBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cerrarBtn.setOnAction(e -> stage.close());

        botonesBox.getChildren().addAll(abrirBtn, descargarBtn, spacer, editarBtn, eliminarBtn, cerrarBtn);

        root.getChildren().addAll(tituloBox, new Separator(), grid, descTitulo, descContenido, archivoBox, botonesBox);

        Scene scene = new Scene(root, 650, -1);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void descargarDocumento() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Descargar Documento");
        fileChooser.setInitialFileName(documento.getNombreArchivo());

        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino != null) {
            try {
                byte[] bytesOriginales = Files.readAllBytes(Paths.get(documento.getRutaArchivo()));
                byte[] bytesFinales = bytesOriginales;

                if ("application/pdf".equals(documento.getTipoMime()) || documento.getNombreArchivo().toLowerCase().endsWith(".pdf")) {
                    com.archivum.service.AjustesService ajustes = new com.archivum.service.AjustesService();

                    if (ajustes.isMarcaAguaActiva()) {
                        com.archivum.service.MarcaAguaService marcaService = new com.archivum.service.MarcaAguaService();
                        bytesFinales = marcaService.aplicarMarcaAguaPDF(
                                bytesOriginales,
                                ajustes.getNombreInstitucion(),
                                ajustes.getMarcaAguaTamano()
                        );
                    }
                }

                Files.write(archivoDestino.toPath(), bytesFinales);
                mostrarAlerta("Documento descargado con éxito en:\n" + archivoDestino.getAbsolutePath());
                stage.close();

            } catch (Exception ex) {
                mostrarAlerta("Error al descargar el documento: " + ex.getMessage());
            }
        }
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

    private String formatearFecha() {
        if (documento.getFechaExacta() != null) return documento.getFechaExacta().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
        if (documento.getAnioAproximado() != null) return "ca. " + documento.getAnioAproximado();
        return "Fecha desconocida";
    }

    private String valorONoDisponible(String valor) {
        return (valor != null && !valor.isBlank()) ? valor : "No especificada";
    }

    private String obtenerIconoClase(String tipo) {
        return switch (tipo) {
            case "ACTA" -> "fas-file-signature";
            case "CARTA" -> "fas-envelope-open-text";
            case "FACTURA", "PRESUPUESTO" -> "fas-file-invoice-dollar";
            case "PLANO" -> "fas-drafting-compass";
            case "PARTITURA" -> "fas-music";
            case "FOTOGRAFIA" -> "fas-camera-retro";
            case "CONTRATO", "ESCRITURA" -> "fas-file-contract";
            case "BOLETIN", "CIRCULAR" -> "fas-newspaper";
            case "INFORME" -> "fas-chart-bar";
            default -> "fas-file-alt";
        };
    }

    private void agregarFila(GridPane grid, int fila, String etiqueta, String valor) {
        Label etiquetaLabel = new Label(etiqueta);
        etiquetaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4D6B58;");
        Label valorLabel = new Label(valor);
        valorLabel.setWrapText(true);
        valorLabel.setStyle("-fx-text-fill: #12261A;");
        grid.add(etiquetaLabel, 0, fila);
        grid.add(valorLabel, 1, fila);
    }

    private void abrirArchivo() {
        new VisorDocumentoDialog(documento).mostrar();
    }

    private void editarDocumento() {
        DocumentoFormDialog dialog = new DocumentoFormDialog(service, documento, () -> {
            if (onDocumentoModificado != null) onDocumentoModificado.run();
            stage.close();
        });
        dialog.mostrar();
    }

    private void eliminarDocumento() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Mover a la papelera");
        confirmacion.setHeaderText("¿Mover \"" + documento.getTitulo() + "\" a la papelera?");
        confirmacion.setContentText("Podrá recuperarlo después desde la papelera.");

        ButtonType btnMover = new ButtonType("Mover a papelera");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(btnMover, btnCancelar);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnMover) {
            try {
                service.eliminarDocumento(documento.getId());
                stage.close();
                if (onDocumentoModificado != null) onDocumentoModificado.run();
            } catch (Exception ex) {
                mostrarAlerta("Error al eliminar: " + ex.getMessage());
            }
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Archivum");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String formatearTamanio(Long bytes) {
        if (bytes == null) return "Desconocido";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}