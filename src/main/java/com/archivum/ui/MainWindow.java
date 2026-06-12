package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.service.BackupService;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Component
public class MainWindow extends Application {

    private static ConfigurableApplicationContext context;
    private DocumentoService service;
    private BackupService backupService;
    private Stage stage;
    private TableView<Documento> tabla;
    private ObservableList<Documento> documentosList;
    private TextField busquedaField;
    private Label estadoLabel, totalDocsLabel, papeleraCountLabel, ultimoBackupLabel;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(com.archivum.ArchivumApplication.class).run();
        service = context.getBean(DocumentoService.class);
        backupService = context.getBean(BackupService.class);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Archivum");
        stage.setMinWidth(1200);
        stage.setMinHeight(750);

        cargarIconoVentana();

        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #F5F1EC;");
        mainLayout.setLeft(crearSidebar());
        mainLayout.setTop(crearTopBar());
        mainLayout.setCenter(crearContenido());

        Scene scene = new Scene(mainLayout);
        var css = getClass().getResource("/estilo.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);

        // Usar tamaño de pantalla sin fullscreen
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.show();
        stage.setOnCloseRequest(e -> { context.close(); Platform.exit(); });

        actualizarDashboard();
        cargarDocumentos();
    }

    private void cargarIconoVentana() {
        try {
            Image icono = null;
            var s = getClass().getResourceAsStream("/images/icono.png");
            if (s != null) icono = new Image(s);
            if (icono == null || icono.isError()) {
                File f = new File("src/main/resources/images/icono.png");
                if (f.exists()) icono = new Image(f.toURI().toString());
            }
            if (icono != null && !icono.isError()) stage.getIcons().add(icono);
        } catch (Exception ignored) {}
    }

    private Image cargarImagenSidebar() {
        try {
            Image img = null;
            var s = getClass().getResourceAsStream("/images/icono.png");
            if (s != null) img = new Image(s);
            if (img == null || img.isError()) {
                File f = new File("src/main/resources/images/icono.png");
                if (f.exists()) img = new Image(f.toURI().toString());
            }
            return (img != null && !img.isError()) ? img : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    // TOP BAR
    // ================================================================
    private HBox crearTopBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.setStyle("-fx-background-color: #2C1810;");
        Button c = new Button("X");
        c.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0886E; -fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        c.setOnAction(e -> { context.close(); Platform.exit(); });
        bar.getChildren().add(c);
        return bar;
    }

    // ================================================================
    // SIDEBAR
    // ================================================================
    private VBox crearSidebar() {
        VBox bar = new VBox(0);
        bar.setPrefWidth(260);
        bar.setMinWidth(260);
        bar.setStyle("-fx-background-color: #2C1810;");

        VBox logoBox = new VBox(10);
        logoBox.setPadding(new Insets(30, 24, 24, 24));
        logoBox.setAlignment(Pos.CENTER_LEFT);

        ImageView logoImg = new ImageView();
        Image img = cargarImagenSidebar();
        if (img != null) {
            logoImg.setImage(img);
            logoImg.setFitWidth(52);
            logoImg.setFitHeight(52);
            logoImg.setPreserveRatio(true);
        }

        Label nombre = new Label("ARCHIVUM");
        nombre.setFont(Font.font("System", FontWeight.BOLD, 18));
        nombre.setStyle("-fx-text-fill: #F5F1EC; -fx-letter-spacing: 3px;");

        Label subt = new Label("Archivo Documental");
        subt.setFont(Font.font("System", FontWeight.NORMAL, 12));
        subt.setStyle("-fx-text-fill: #A0886E;");

        if (img != null) {
            logoBox.getChildren().addAll(logoImg, nombre, subt);
        } else {
            Label l = new Label("A");
            l.setFont(Font.font("Serif", FontWeight.BOLD, 40));
            l.setStyle("-fx-text-fill: #D4A853;");
            logoBox.getChildren().addAll(l, nombre, subt);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #3E2418;");

        VBox nav = new VBox(6);
        nav.setPadding(new Insets(20, 14, 20, 14));
        nav.getChildren().addAll(
                btnSidebar("Inicio", true, this::recargarInicio),
                btnSidebar("Nuevo documento", false, this::nuevoDocumento),
                btnSidebar("Papelera", false, this::abrirPapelera),
                btnSidebar("Copias de seguridad", false, this::hacerBackup)
        );

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox footer = new VBox(4);
        footer.setPadding(new Insets(20, 14, 24, 14));
        footer.getChildren().addAll(
                lblFooter("v1.0.0"),
                lblFooter("Enrique Cala Rodriguez")
        );

        bar.getChildren().addAll(logoBox, sep, nav, sp, footer);
        return bar;
    }

    private Button btnSidebar(String t, boolean act, Runnable a) {
        Button b = new Button(t);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("System", FontWeight.NORMAL, 14));
        b.setOnAction(e -> a.run());
        if (act) {
            b.setStyle("-fx-background-color: #3E2418; -fx-text-fill: #D4A853; -fx-background-radius: 8px; -fx-padding: 12px 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #C4B5A8; -fx-background-radius: 8px; -fx-padding: 12px 16px; -fx-cursor: hand;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #3A1F14; -fx-text-fill: #F5F1EC; -fx-background-radius: 8px; -fx-padding: 12px 16px; -fx-cursor: hand;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #C4B5A8; -fx-background-radius: 8px; -fx-padding: 12px 16px; -fx-cursor: hand;"));
        }
        return b;
    }

    private Label lblFooter(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("System", FontWeight.NORMAL, 10));
        l.setStyle("-fx-text-fill: #6B4F42;");
        return l;
    }

    private void recargarInicio() {
        actualizarDashboard();
        cargarDocumentos();
    }

    // ================================================================
    // CONTENIDO PRINCIPAL
    // ================================================================
    private VBox crearContenido() {
        VBox c = new VBox(0);
        VBox.setVgrow(c, Priority.ALWAYS);
        c.getChildren().addAll(crearDashboard(), crearSeccionTabla());
        return c;
    }

    private VBox crearDashboard() {
        VBox db = new VBox(18);
        db.setPadding(new Insets(28, 32, 12, 32));
        db.setStyle("-fx-background-color: white; -fx-border-color: #E8E0D8; -fx-border-width: 0 0 1px 0;");

        Label tit = new Label("Archivo Documental de la Hermandad");
        tit.setFont(Font.font("System", FontWeight.BOLD, 20));
        tit.setStyle("-fx-text-fill: #2C1810;");

        Label fecha = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy")));
        fecha.setFont(Font.font("System", FontWeight.NORMAL, 14));
        fecha.setStyle("-fx-text-fill: #8B7B6E;");

        HBox tarjetas = new HBox(20);
        tarjetas.setPadding(new Insets(8, 0, 8, 0));

        VBox t1 = tarjeta("Documentos archivados", "0", "#4A6741");
        totalDocsLabel = (Label) t1.getChildren().get(1);
        VBox t2 = tarjeta("En papelera", "0", "#A0522D");
        papeleraCountLabel = (Label) t2.getChildren().get(1);
        VBox t3 = tarjeta("Ultima copia de seguridad", "Nunca", "#6B5B4F");
        ultimoBackupLabel = (Label) t3.getChildren().get(1);

        tarjetas.getChildren().addAll(t1, t2, t3);
        db.getChildren().addAll(tit, fecha, tarjetas);
        return db;
    }

    private VBox tarjeta(String tit, String val, String color) {
        VBox t = new VBox(12);
        t.setPadding(new Insets(22, 24, 22, 24));
        t.setPrefWidth(240);
        t.setStyle("-fx-background-color: #FAF7F4; -fx-background-radius: 10px; -fx-border-color: #E8E0D8; -fx-border-radius: 10px; -fx-border-width: 1px;");

        Region bar = new Region();
        bar.setPrefHeight(5);
        bar.setMaxWidth(240);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px;");

        Label vl = new Label(val);
        vl.setFont(Font.font("System", FontWeight.BOLD, 32));
        vl.setStyle("-fx-text-fill: #2C1810;");

        Label lb = new Label(tit);
        lb.setFont(Font.font("System", FontWeight.NORMAL, 13));
        lb.setStyle("-fx-text-fill: #8B7B6E;");

        t.getChildren().addAll(bar, vl, lb);
        return t;
    }

    private void actualizarDashboard() {
        if (totalDocsLabel != null) totalDocsLabel.setText(String.valueOf(service.contarDocumentos()));
        if (papeleraCountLabel != null) papeleraCountLabel.setText(String.valueOf(service.contarDocumentosEnPapelera()));
        if (ultimoBackupLabel != null) {
            File bd = new File("./backups");
            if (bd.exists()) {
                File[] bk = bd.listFiles((d, n) -> n.endsWith(".zip"));
                if (bk != null && bk.length > 0) {
                    java.util.Arrays.sort(bk, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    ultimoBackupLabel.setText(new java.text.SimpleDateFormat("dd/MM/yyyy").format(bk[0].lastModified()));
                    return;
                }
            }
            ultimoBackupLabel.setText("Nunca");
        }
    }

    private VBox crearSeccionTabla() {
        VBox sec = new VBox(0);
        sec.setPadding(new Insets(0, 32, 24, 32));
        VBox.setVgrow(sec, Priority.ALWAYS);

        HBox toolbar = new HBox(14);
        toolbar.setPadding(new Insets(18, 0, 14, 0));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label tSec = new Label("Documentos");
        tSec.setFont(Font.font("System", FontWeight.BOLD, 16));
        tSec.setStyle("-fx-text-fill: #2C1810;");

        busquedaField = new TextField();
        busquedaField.setPromptText("Buscar por titulo o descripcion...");
        busquedaField.setPrefWidth(380);
        busquedaField.setStyle("-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 10px 16px; -fx-border-color: #D5CCC2; -fx-background-color: white;");
        busquedaField.textProperty().addListener((o, ov, nv) -> buscar());

        Button filtros = new Button("Filtros");
        filtros.setStyle("-fx-background-color: white; -fx-text-fill: #5D4037; -fx-border-color: #D5CCC2; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px 18px; -fx-cursor: hand;");
        filtros.setOnAction(e -> mostrarFiltros());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button nuevo = new Button("+ Nuevo documento");
        nuevo.setStyle("-fx-background-color: #4A6741; -fx-text-fill: white; -fx-background-radius: 8px; -fx-padding: 11px 20px; -fx-font-weight: bold; -fx-cursor: hand;");
        nuevo.setOnAction(e -> nuevoDocumento());

        toolbar.getChildren().addAll(tSec, busquedaField, filtros, sp, nuevo);

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No se encontraron documentos"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Documento, String> cTit = new TableColumn<>("Titulo");
        cTit.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        cTit.setPrefWidth(320);
        cTit.setSortable(true);

        TableColumn<Documento, String> cTip = new TableColumn<>("Tipo");
        cTip.setCellValueFactory(d -> d.getValue().getTipo() != null ?
                javafx.beans.binding.Bindings.createStringBinding(() -> d.getValue().getTipo().getEtiqueta()) : null);
        cTip.setPrefWidth(140);
        cTip.setSortable(true);

        TableColumn<Documento, LocalDate> cFec = new TableColumn<>("Fecha");
        cFec.setCellValueFactory(new PropertyValueFactory<>("fechaExacta"));
        cFec.setPrefWidth(120);
        cFec.setSortable(true);
        cFec.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate f, boolean e) {
                super.updateItem(f, e);
                setText((e || f == null) ? null : f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        TableColumn<Documento, Integer> cAni = new TableColumn<>("Anio aprox.");
        cAni.setCellValueFactory(new PropertyValueFactory<>("anioAproximado"));
        cAni.setPrefWidth(90);
        cAni.setSortable(true);
        cAni.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer a, boolean e) {
                super.updateItem(a, e);
                setText((e || a == null) ? null : "c. " + a);
            }
        });

        TableColumn<Documento, String> cUbi = new TableColumn<>("Ubicacion fisica");
        cUbi.setCellValueFactory(new PropertyValueFactory<>("ubicacionFisica"));
        cUbi.setPrefWidth(200);
        cUbi.setSortable(true);

        tabla.getColumns().addAll(cTit, cTip, cFec, cAni, cUbi);
        tabla.getSortOrder().add(cFec);
        cFec.setSortType(TableColumn.SortType.DESCENDING);

        tabla.setRowFactory(tv -> {
            TableRow<Documento> r = new TableRow<>();
            r.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !r.isEmpty()) verDetalle(r.getItem());
            });
            return r;
        });

        documentosList = FXCollections.observableArrayList();
        tabla.setItems(documentosList);

        HBox statusBar = new HBox(8);
        statusBar.setPadding(new Insets(10, 0, 0, 0));
        estadoLabel = new Label("Listo");
        estadoLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        estadoLabel.setStyle("-fx-text-fill: #8B7B6E;");
        statusBar.getChildren().add(estadoLabel);

        sec.getChildren().addAll(toolbar, tabla, statusBar);
        return sec;
    }

    // ================================================================
    // ACCIONES
    // ================================================================
    private void cargarDocumentos() {
        documentosList.setAll(service.obtenerTodos());
        actualizarDashboard();
    }

    private void buscar() {
        String t = busquedaField.getText();
        documentosList.setAll((t == null || t.isBlank()) ? service.obtenerTodos() : service.buscar(t, null, null, null, null));
    }

    private void mostrarFiltros() {
        FiltroDialog f = new FiltroDialog();
        if (f.mostrar())
            documentosList.setAll(service.buscar(null, f.getTipo(), f.getDesde(), f.getHasta(), f.getAnio()));
        Platform.runLater(() -> {
            // Se ha eliminado stage.setFullScreen(true);
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().getRoot().requestLayout();
            }
            if (tabla != null) tabla.refresh();
        });
    }

    private void nuevoDocumento() {
        new DocumentoFormDialog(service, null, () ->
                Platform.runLater(() -> { cargarDocumentos(); actualizarDashboard(); })).mostrar();
        Platform.runLater(() -> {
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().getRoot().requestLayout();
            }
            if (tabla != null) tabla.refresh();
        });
    }

    private void verDetalle(Documento d) {
        new DocumentoDetailDialog(service, d, () ->
                Platform.runLater(() -> { cargarDocumentos(); actualizarDashboard(); })).mostrar();
        Platform.runLater(() -> {
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().getRoot().requestLayout();
            }
            if (tabla != null) tabla.refresh();
        });
    }

    private void hacerBackup() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Guardar copia de seguridad");
        File c = dc.showDialog(stage);
        if (c != null) {
            try {
                File bk = backupService.crearBackup();
                File dst = new File(c, bk.getName());
                java.nio.file.Files.move(bk.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                estadoLabel.setText("Copia creada: " + dst.getName());
                actualizarDashboard();
            } catch (Exception ex) {
                estadoLabel.setText("Error al crear copia");
            }
        }
    }

    private void abrirPapelera() {
        new PapeleraDialog(service, () ->
                Platform.runLater(() -> { cargarDocumentos(); actualizarDashboard(); })).mostrar(); // (Este ya estaba bien)
    }

    @Override
    public void stop() {
        if (context != null) context.close();
    }
}