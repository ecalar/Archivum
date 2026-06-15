package com.archivum.ui;

import com.archivum.model.Documento;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class VisorDocumentoDialog {

    private final Documento documento;
    private Stage stage;
    private ImageView imageView;
    private Label pageLabel;
    private Button prevBtn, nextBtn;

    private PDDocument pdfDocument;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;
    private int totalPages = 0;

    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.25;

    private Image singleImage;

    public VisorDocumentoDialog(Documento documento) {
        this.documento = documento;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initModality(Modality.NONE); // Permite ver el documento y usar la app al mismo tiempo
        stage.setTitle("Visor Documental: " + documento.getTitulo());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1A221E;"); // Gris-verdoso oscuro de fondo

        // ==========================================
        // TOOLBAR (Verde bosque oscuro)
        // ==========================================
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(10, 20, 10, 20));
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setStyle("-fx-background-color: #12261A; -fx-border-width: 0 0 1px 0; -fx-border-color: #0B1710;");

        // Controles de navegación
        prevBtn = crearBotonToolbar("Anterior", "fas-chevron-left", false);
        nextBtn = crearBotonToolbar("Siguiente", "fas-chevron-right", true);

        pageLabel = new Label("");
        pageLabel.setFont(Font.font("Segoe UI", 14));
        pageLabel.setStyle("-fx-text-fill: #CFA144; -fx-font-weight: bold;"); // Dorado institucional

        // Controles de Zoom
        Button zoomOutBtn = crearBotonToolbar("", "fas-search-minus", false);
        Button zoomResetBtn = crearBotonToolbar("100%", "fas-expand", false);
        Button zoomInBtn = crearBotonToolbar("", "fas-search-plus", false);

        prevBtn.setOnAction(e -> cambiarPagina(-1));
        nextBtn.setOnAction(e -> cambiarPagina(1));
        zoomInBtn.setOnAction(e -> zoom(1));
        zoomOutBtn.setOnAction(e -> zoom(-1));
        zoomResetBtn.setOnAction(e -> { zoomFactor = 1.0; actualizarZoom(); });

        // Botón cerrar (con un toque sutil rojo en el hover dentro del método de creación si se desea, o estándar)
        Button cerrarBtn = crearBotonToolbar("Cerrar visor", "fas-times", false);
        cerrarBtn.setOnAction(e -> stage.close());

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                prevBtn, pageLabel, nextBtn,
                spacer1,
                zoomOutBtn, zoomResetBtn, zoomInBtn,
                spacer2,
                cerrarBtn
        );

        // ==========================================
        // ÁREA DEL DOCUMENTO (Centrada y con sombra)
        // ==========================================
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        // Sombra suave para dar efecto de "hoja de papel" física
        imageView.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.6)));

        // Usar StackPane para asegurar que la imagen quede centrada cuando se hace zoom out
        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-background-color: #1A221E; -fx-padding: 30px;");

        ScrollPane scrollPane = new ScrollPane(imageContainer);
        scrollPane.setStyle("-fx-background: #1A221E; -fx-background-color: #1A221E; -fx-border-color: transparent;");
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        root.setTop(toolbar);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 1000, 800);
        // Opcional: cargar la hoja de estilos global si la necesitas
        // var css = getClass().getResource("/estilo.css");
        // if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (pdfDocument != null) {
                try { pdfDocument.close(); } catch (IOException ignored) {}
            }
        });

        cargarDocumento();
        stage.show();
    }

    /**
     * Método helper para crear botones con Iconos vectoriales y efectos hover integrados.
     */
    private Button crearBotonToolbar(String texto, String iconCode, boolean iconRight) {
        Button btn = new Button(texto);
        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(14);
            icon.setIconColor(Color.web("#F4F7F5"));
            btn.setGraphic(icon);
            btn.setGraphicTextGap(8);
            if (iconRight) {
                btn.setContentDisplay(ContentDisplay.RIGHT);
            }
        }

        // Estilo base
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #F4F7F5; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8px 12px; -fx-border-radius: 6px;");

        // Efectos Hover (Dorado)
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: rgba(207, 161, 68, 0.15); -fx-text-fill: #CFA144; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8px 12px; -fx-border-radius: 6px;");
            if (btn.getGraphic() != null) ((FontIcon)btn.getGraphic()).setIconColor(Color.web("#CFA144"));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #F4F7F5; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8px 12px; -fx-border-radius: 6px;");
            if (btn.getGraphic() != null) ((FontIcon)btn.getGraphic()).setIconColor(Color.web("#F4F7F5"));
        });

        return btn;
    }

    private void cargarDocumento() {
        String ruta = documento.getRutaArchivo();
        String tipo = documento.getTipoMime();
        File archivo = new File(ruta);

        if (!archivo.exists()) {
            mostrarError("El archivo físico no se encuentra en disco.");
            return;
        }

        try {
            if (tipo != null && tipo.equals("application/pdf")) {
                cargarPDF(archivo);
            } else if (tipo != null && (tipo.startsWith("image/"))) {
                cargarImagen(archivo);
            } else if (ruta.toLowerCase().endsWith(".pdf")) {
                cargarPDF(archivo);
            } else {
                cargarImagen(archivo);
            }
        } catch (Exception e) {
            mostrarError("Error al cargar el documento: " + e.getMessage());
        }
    }

    private void cargarPDF(File archivo) throws IOException {
        pdfDocument = Loader.loadPDF(archivo);
        pdfRenderer = new PDFRenderer(pdfDocument);
        totalPages = pdfDocument.getNumberOfPages();
        currentPage = 0;
        actualizarBotonesNavegacion();
        cargarPagina(0);
    }

    private void cargarImagen(File archivo) {
        try {
            singleImage = new Image(archivo.toURI().toString());

            // Comprobación de seguridad vital para JavaFX
            if (singleImage.isError()) {
                mostrarError("Formato de imagen no soportado o archivo dañado.");
                return;
            }

            imageView.setImage(singleImage);
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            pageLabel.setText("Documento de Imagen");

            // Auto-ajustar el zoom inicial para que la imagen quepa en la pantalla
            zoomFactor = 1.0;
            if (singleImage.getWidth() > 850) {
                zoomFactor = 850 / singleImage.getWidth();
            }
            actualizarZoom();

        } catch (Exception e) {
            mostrarError("No se pudo cargar la imagen: " + e.getMessage());
        }
    }

    private void cargarPagina(int pageNum) {
        try {
            // Renderiza con un DPI decente para que no se vea pixelado
            BufferedImage bufferedImage = pdfRenderer.renderImage(pageNum, 1.5f);
            Image fxImage = bufferedImageToFxImage(bufferedImage);
            imageView.setImage(fxImage);
            pageLabel.setText("Página " + (pageNum + 1) + " de " + totalPages);
            zoomFactor = 1.0;
            actualizarZoom();
            actualizarBotonesNavegacion();
        } catch (IOException e) {
            mostrarError("Error al renderizar página.");
        }
    }

    private Image bufferedImageToFxImage(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return new Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    private void cambiarPagina(int delta) {
        if (pdfDocument == null) return;
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            cargarPagina(currentPage);
        }
    }

    private void actualizarBotonesNavegacion() {
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage == totalPages - 1);
    }

    private void zoom(int direction) {
        zoomFactor += direction * ZOOM_STEP;
        if (zoomFactor < 0.25) zoomFactor = 0.25;
        if (zoomFactor > 5.0) zoomFactor = 5.0;
        actualizarZoom();
    }

    private void actualizarZoom() {
        if (imageView.getImage() != null) {
            double w = imageView.getImage().getWidth() * zoomFactor;
            double h = imageView.getImage().getHeight() * zoomFactor;
            imageView.setFitWidth(w);
            imageView.setFitHeight(h);
        }
    }

    private void mostrarError(String msg) {
        imageView.setImage(null);
        pageLabel.setText("⚠ Error de lectura");
        pageLabel.setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}