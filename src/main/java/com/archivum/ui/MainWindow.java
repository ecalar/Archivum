package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.service.DocumentoService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MainWindow extends Application {

    private static ConfigurableApplicationContext context;
    private DocumentoService service;

    private TableView<Documento> tabla;
    private ObservableList<Documento> documentosList;
    private TextField busquedaField;
    private Label estadoLabel;
    private Label contadorLabel;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(com.archivum.ArchivumApplication.class).run();
        service = context.getBean(DocumentoService.class);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Archivum - Archivo Documental");
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        BorderPane root = new BorderPane();
        root.setTop(crearBarraBusqueda());
        root.setCenter(crearTabla());
        root.setBottom(crearBarraEstado());

        Scene scene = new Scene(root);
        String cssPath = "/estilo.css";
        var cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("AVISO: No se encontró el archivo estilo.css, continuando sin estilos");
        }

        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            context.close();
            Platform.exit();
        });

        cargarDocumentos();
        stage.show();
    }

    private VBox crearBarraBusqueda() {
        VBox barra = new VBox(10);
        barra.setPadding(new Insets(15));
        barra.setStyle("-fx-background-color: #4A0E17;");

        Label tituloApp = new Label("ARCHIVUM");
        tituloApp.setStyle("-fx-text-fill: #C9A84C; -fx-font-size: 20px; -fx-font-weight: bold;");

        HBox buscadorBox = new HBox(10);
        buscadorBox.setAlignment(Pos.CENTER_LEFT);

        busquedaField = new TextField();
        busquedaField.setPromptText("Buscar documentos...");
        busquedaField.setPrefWidth(400);
        busquedaField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
        busquedaField.textProperty().addListener((obs, old, nuevo) -> buscar());

        Button filtrosBtn = new Button("Filtros");
        filtrosBtn.setStyle("-fx-background-color: #C9A84C; -fx-text-fill: #4A0E17; -fx-font-weight: bold;");
        filtrosBtn.setOnAction(e -> mostrarFiltros());

        Button nuevoBtn = new Button("+ Nuevo documento");
        nuevoBtn.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; -fx-font-weight: bold;");
        nuevoBtn.setOnAction(e -> nuevoDocumento());

        Button backupBtn = new Button("Backup");
        backupBtn.setStyle("-fx-background-color: #6C6C6C; -fx-text-fill: white;");
        backupBtn.setOnAction(e -> hacerBackup());

        Button papeleraBtn = new Button("Papelera");
        papeleraBtn.setStyle("-fx-background-color: #C1292E; -fx-text-fill: white;");
        papeleraBtn.setOnAction(e -> abrirPapelera());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buscadorBox.getChildren().addAll(busquedaField, filtrosBtn, spacer, nuevoBtn, backupBtn, papeleraBtn);

        barra.getChildren().addAll(tituloApp, buscadorBox);
        return barra;
    }

    private VBox crearTabla() {
        VBox contenedor = new VBox(10);
        contenedor.setPadding(new Insets(15));

        contadorLabel = new Label("0 documentos");
        contadorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6C6C6C;");

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Documento, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTitulo.setPrefWidth(300);

        TableColumn<Documento, TipoDocumento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(150);

        TableColumn<Documento, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaExacta"));
        colFecha.setPrefWidth(120);
        colFecha.setCellFactory(col -> new TableCell<Documento, LocalDate>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                if (empty || fecha == null) {
                    setText(null);
                } else {
                    setText(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        TableColumn<Documento, Integer> colAnio = new TableColumn<>("Año");
        colAnio.setCellValueFactory(new PropertyValueFactory<>("anioAproximado"));
        colAnio.setPrefWidth(80);
        colAnio.setCellFactory(col -> new TableCell<Documento, Integer>() {
            @Override
            protected void updateItem(Integer anio, boolean empty) {
                super.updateItem(anio, empty);
                if (empty || anio == null) {
                    setText(null);
                } else {
                    setText("ca. " + anio);
                }
            }
        });

        TableColumn<Documento, String> colUbicacion = new TableColumn<>("Ubicación");
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionFisica"));
        colUbicacion.setPrefWidth(200);

        tabla.getColumns().addAll(colTitulo, colTipo, colFecha, colAnio, colUbicacion);
        tabla.setRowFactory(tv -> {
            TableRow<Documento> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    verDetalle(row.getItem());
                }
            });
            return row;
        });

        documentosList = FXCollections.observableArrayList();
        tabla.setItems(documentosList);

        contenedor.getChildren().addAll(contadorLabel, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return contenedor;
    }

    private HBox crearBarraEstado() {
        HBox barra = new HBox(15);
        barra.setPadding(new Insets(10, 15, 10, 15));
        barra.setStyle("-fx-background-color: #F5F0E8; -fx-border-color: #E8E5DF; -fx-border-width: 1px 0px 0px 0px;");

        estadoLabel = new Label("✅ Listo");
        estadoLabel.setStyle("-fx-text-fill: #2D6A4F;");

        barra.getChildren().add(estadoLabel);
        return barra;
    }

    private void cargarDocumentos() {
        List<Documento> docs = service.obtenerTodos();
        documentosList.setAll(docs);
        actualizarContador();
    }

    private void buscar() {
        String texto = busquedaField.getText();
        List<Documento> resultados;
        if (texto == null || texto.isBlank()) {
            resultados = service.obtenerTodos();
        } else {
            resultados = service.buscar(texto, null, null, null, null);
        }
        documentosList.setAll(resultados);
        actualizarContador();
    }

    private void actualizarContador() {
        long total = documentosList.size();
        contadorLabel.setText(total + " documento" + (total != 1 ? "s" : ""));
    }

    private void mostrarFiltros() {
        // Implementar Día 5
        mostrarEstado("Filtros disponibles próximamente");
    }

    private void nuevoDocumento() {
        DocumentoFormDialog dialog = new DocumentoFormDialog(service, () -> {
            Platform.runLater(this::cargarDocumentos);
        });
        dialog.mostrar();
        actualizarEstadoListo();
    }

    private void verDetalle(Documento doc) {
        mostrarEstado("Detalle de: " + doc.getTitulo() + " (Día 5)");
    }

    private void hacerBackup() {
        mostrarEstado("Backup (Día 7)");
    }

    private void abrirPapelera() {
        mostrarEstado("Papelera (Día 6)");
    }

    private void mostrarEstado(String mensaje) {
        estadoLabel.setText("ℹ️ " + mensaje);
        estadoLabel.setStyle("-fx-text-fill: #6C6C6C;");
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    private void actualizarEstadoListo() {
        estadoLabel.setText("Listo");
        estadoLabel.setStyle("-fx-text-fill: #2D6A4F;");
    }
}