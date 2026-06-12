package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.DocumentoService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DocumentoDetailDialog {

    private final DocumentoService service;
    private final Documento documento;
    private final Runnable onDocumentoModificado;
    private Stage stage;

    public DocumentoDetailDialog(DocumentoService service, Documento documento, Runnable onDocumentoModificado) {
        this.service = service;
        this.documento = documento;
        this.onDocumentoModificado = onDocumentoModificado;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(documento.getTitulo());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        // Título con emoji según tipo
        String emoji = obtenerEmoji(documento.getTipo().name());
        Label tituloLabel = new Label(emoji + " " + documento.getTitulo());
        tituloLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4A0E17;");
        tituloLabel.setWrapText(true);

        // Grid con datos
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        agregarFila(grid, 0, "Tipo:", documento.getTipo().getEtiqueta());
        agregarFila(grid, 1, "Fecha:", formatearFecha());
        agregarFila(grid, 2, "Ubicación:", valorONoDisponible(documento.getUbicacionFisica()));
        agregarFila(grid, 3, "Subido:", documento.getFechaSubida() != null
                ? documento.getFechaSubida().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Desconocido");

        // Descripción
        Label descTitulo = new Label("Descripción:");
        descTitulo.setStyle("-fx-font-weight: bold;");
        String descripcion = documento.getDescripcion() != null && !documento.getDescripcion().isBlank()
                ? documento.getDescripcion()
                : "Sin descripción";
        Label descContenido = new Label(descripcion);
        descContenido.setWrapText(true);
        descContenido.setStyle("-fx-text-fill: #2D2D2D;");

        // Archivo
        HBox archivoBox = new HBox(10);
        archivoBox.setAlignment(Pos.CENTER_LEFT);
        archivoBox.setPadding(new Insets(10));
        archivoBox.setStyle("-fx-background-color: #F5F0E8; -fx-background-radius: 6px;");

        Label archivoIcono = new Label("📄");
        archivoIcono.setStyle("-fx-font-size: 20px;");

        VBox archivoInfo = new VBox(3);
        Label archivoNombre = new Label(documento.getNombreArchivo());
        archivoNombre.setStyle("-fx-font-weight: bold;");
        Label archivoTamano = new Label(formatearTamanio(documento.getTamanioBytes()));
        archivoTamano.setStyle("-fx-text-fill: #6C6C6C; -fx-font-size: 11px;");
        archivoInfo.getChildren().addAll(archivoNombre, archivoTamano);

        archivoBox.getChildren().addAll(archivoIcono, archivoInfo);

        // Botones
        HBox botonesBox = new HBox(10);
        botonesBox.setAlignment(Pos.CENTER_RIGHT);

        Button abrirBtn = new Button("📂 Abrir archivo");
        abrirBtn.setStyle("-fx-background-color: #4A0E17; -fx-text-fill: white;");
        abrirBtn.setOnAction(e -> abrirArchivo());

        Button editarBtn = new Button("✏️ Editar");
        editarBtn.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #4A0E17; -fx-font-weight: bold;");
        editarBtn.setOnAction(e -> editarDocumento());

        Button eliminarBtn = new Button("🗑 Eliminar");
        eliminarBtn.setStyle("-fx-background-color: #C1292E; -fx-text-fill: white;");
        eliminarBtn.setOnAction(e -> eliminarDocumento());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setOnAction(e -> stage.close());

        botonesBox.getChildren().addAll(abrirBtn, spacer, editarBtn, eliminarBtn, cerrarBtn);

        // Añadir todo
        root.getChildren().addAll(tituloLabel, grid, descTitulo, descContenido, archivoBox, botonesBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private String formatearFecha() {
        if (documento.getFechaExacta() != null) {
            return documento.getFechaExacta().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
        }
        if (documento.getAnioAproximado() != null) {
            return "ca. " + documento.getAnioAproximado();
        }
        return "Fecha desconocida";
    }

    private String valorONoDisponible(String valor) {
        return (valor != null && !valor.isBlank()) ? valor : "No especificada";
    }

    private String obtenerEmoji(String tipo) {
        return switch (tipo) {
            case "ACTA" -> "📜";
            case "CARTA" -> "✉️";
            case "FACTURA", "PRESUPUESTO" -> "🧾";
            case "PLANO" -> "📐";
            case "PARTITURA" -> "🎼";
            case "FOTOGRAFIA" -> "📷";
            case "CONTRATO", "ESCRITURA" -> "📝";
            case "BOLETIN", "CIRCULAR" -> "📰";
            case "INFORME" -> "📋";
            default -> "📄";
        };
    }

    private void agregarFila(GridPane grid, int fila, String etiqueta, String valor) {
        Label etiquetaLabel = new Label(etiqueta);
        etiquetaLabel.setStyle("-fx-font-weight: bold;");
        Label valorLabel = new Label(valor);
        valorLabel.setWrapText(true);
        grid.add(etiquetaLabel, 0, fila);
        grid.add(valorLabel, 1, fila);
    }

    private void abrirArchivo() {
        try {
            File archivo = new File(documento.getRutaArchivo());
            System.out.println("Intentando abrir: " + archivo.getAbsolutePath());

            if (!archivo.exists()) {
                mostrarAlerta("El archivo no se encuentra en disco.\n\n" +
                        "Nota: Los documentos de prueba no contienen archivos reales.\n" +
                        "Cree un documento nuevo con un archivo PDF o imagen real para probar esta función.");
                return;
            }

            // Intentar con Desktop primero
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(archivo);
                    return;
                }
            }

            // Fallback para Linux: usar xdg-open
            try {
                new ProcessBuilder("xdg-open", archivo.getAbsolutePath()).start();
                return;
            } catch (IOException ignored) {}

            // Fallback: mostrar ruta
            mostrarAlerta("No se pudo abrir el archivo automáticamente.\n\n" +
                    "Ruta del archivo:\n" + archivo.getAbsolutePath() +
                    "\n\nPuede abrirlo manualmente desde su explorador de archivos.");

        } catch (Exception ex) {
            mostrarAlerta("Error al abrir el archivo: " + ex.getMessage());
        }
    }

    private void editarDocumento() {
        DocumentoFormDialog dialog = new DocumentoFormDialog(service, documento, () -> {
            if (onDocumentoModificado != null) onDocumentoModificado.run();
            stage.close();
        });
        dialog.mostrar(); // ACTUALIZADO para usar el nuevo método sin parámetros
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
                if (onDocumentoModificado != null) {
                    onDocumentoModificado.run();
                }
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