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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        mainLayout.setStyle("-fx-background-color: #F4F7F5;"); // Fondo claro con un levísimo toque verde

        mainLayout.setLeft(crearSidebar());

        VBox centerLayout = new VBox();
        centerLayout.getChildren().addAll(crearTopBar(), crearContenido());
        mainLayout.setCenter(centerLayout);

        Scene scene = new Scene(mainLayout);
        var css = getClass().getResource("/estilo.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);

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
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setStyle("-fx-background-color: transparent;");
        Button c = new Button("✕");
        c.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        c.setOnMouseEntered(e -> c.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 6px;"));
        c.setOnMouseExited(e -> c.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;"));
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
        // Verde bosque muy oscuro, casi negro
        bar.setStyle("-fx-background-color: #12261A;");

        VBox logoBox = new VBox(10);
        logoBox.setPadding(new Insets(15, 20, 25, 20));
        logoBox.setAlignment(Pos.CENTER);

        ImageView logoImg = new ImageView();
        Image img = cargarImagenSidebar();
        if (img != null) {
            logoImg.setImage(img);
            logoImg.setFitWidth(64);
            logoImg.setFitHeight(64);
            logoImg.setPreserveRatio(true);
        }

        VBox textLogoBox = new VBox(3);
        textLogoBox.setAlignment(Pos.CENTER);

        Label nombre = new Label("ARCHIVUM");
        nombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        nombre.setStyle("-fx-text-fill: #F4F7F5; -fx-letter-spacing: 2px;");

        Label subt = new Label("Archivo Documental");
        subt.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subt.setStyle("-fx-text-fill: #CFA144;"); // Dorado

        textLogoBox.getChildren().addAll(nombre, subt);

        if (img != null) {
            logoBox.getChildren().addAll(logoImg, textLogoBox);
        } else {
            Label l = new Label("A");
            l.setFont(Font.font("Serif", FontWeight.BOLD, 48));
            l.setStyle("-fx-text-fill: #CFA144;");
            logoBox.getChildren().addAll(l, textLogoBox);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1E3B2A;"); // Verde un poco más claro para la línea

        VBox nav = new VBox(8);
        nav.setPadding(new Insets(25, 12, 20, 12));
        nav.getChildren().addAll(
                btnSidebar("Inicio", "fas-home", true, this::recargarInicio),
                btnSidebar("Nuevo documento", "fas-file-alt", false, this::nuevoDocumento),
                btnSidebar("Papelera", "fas-trash-alt", false, this::abrirPapelera),
                btnSidebar("Copias de seguridad", "fas-save", false, this::hacerBackup)
        );

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox footer = new VBox(4);
        footer.setPadding(new Insets(20, 14, 24, 14));
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().addAll(
                lblFooter("v1.0.0"),
                lblFooter("Enrique Cala Rodríguez")
        );

        bar.getChildren().addAll(logoBox, sep, nav, sp, footer);
        return bar;
    }

    private Button btnSidebar(String t, String iconCode, boolean act, Runnable a) {
        Button b = new Button(t);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        b.setOnAction(e -> a.run());

        // Crear y configurar el icono vectorial
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        b.setGraphic(icon);
        b.setGraphicTextGap(12); // Espacio entre el icono y el texto

        if (act) {
            icon.setIconColor(Color.web("#CFA144")); // Icono dorado
            b.setStyle("-fx-background-color: rgba(207, 161, 68, 0.1); -fx-text-fill: #CFA144; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #CFA144; -fx-border-width: 0 0 0 4px;");
        } else {
            icon.setIconColor(Color.web("#94A89A")); // Icono gris verdoso
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A89A; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 0 4px;");

            // Efecto Hover: Cambia fondo, texto y el color del icono a blanco
            b.setOnMouseEntered(e -> {
                b.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-text-fill: #FFFFFF; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 0 4px;");
                icon.setIconColor(Color.WHITE);
            });
            b.setOnMouseExited(e -> {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A89A; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 0 4px;");
                icon.setIconColor(Color.web("#94A89A"));
            });
        }
        return b;
    }

    private Label lblFooter(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        l.setStyle("-fx-text-fill: #4D6B58;");
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
        VBox db = new VBox(20);
        db.setPadding(new Insets(10, 32, 20, 32));

        Label tit = new Label("Dashboard General");
        tit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        tit.setStyle("-fx-text-fill: #12261A;"); // Verde oscuro

        Label fecha = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy")));
        fecha.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        fecha.setStyle("-fx-text-fill: #6B7A70;");

        VBox headerText = new VBox(2);
        headerText.getChildren().addAll(tit, fecha);

        HBox tarjetas = new HBox(20);
        tarjetas.setPadding(new Insets(10, 0, 10, 0));

        // Acentos: Dorado, Granate oscuro y Verde esmeralda
        VBox t1 = tarjeta("Documentos archivados", "0", "#CFA144");
        totalDocsLabel = (Label) t1.getChildren().get(1);
        VBox t2 = tarjeta("En papelera", "0", "#8A3636");
        papeleraCountLabel = (Label) t2.getChildren().get(1);
        VBox t3 = tarjeta("Última copia de seguridad", "Nunca", "#2A6B44");
        ultimoBackupLabel = (Label) t3.getChildren().get(1);

        tarjetas.getChildren().addAll(t1, t2, t3);
        db.getChildren().addAll(headerText, tarjetas);
        return db;
    }

    private VBox tarjeta(String tit, String val, String color) {
        VBox t = new VBox(12);
        t.setPadding(new Insets(24, 24, 24, 24));
        t.setPrefWidth(260);
        t.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(18, 38, 26, 0.06), 15, 0, 0, 5);");

        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setMaxWidth(40);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4px;");

        Label vl = new Label(val);
        vl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        vl.setStyle("-fx-text-fill: #12261A;");

        Label lb = new Label(tit);
        lb.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        lb.setStyle("-fx-text-fill: #6B7A70;");

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
        sec.setPadding(new Insets(10, 32, 32, 32));
        VBox.setVgrow(sec, Priority.ALWAYS);

        VBox tableContainer = new VBox(15);
        tableContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-padding: 20px; -fx-effect: dropshadow(three-pass-box, rgba(18, 38, 26, 0.06), 15, 0, 0, 5);");
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label tSec = new Label("Documentos");
        tSec.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        tSec.setStyle("-fx-text-fill: #12261A;");

        busquedaField = new TextField();
        busquedaField.setPromptText("Buscar por título o descripción...");
        busquedaField.setPrefWidth(320);
        busquedaField.textProperty().addListener((o, ov, nv) -> buscar());

        Button filtros = new Button("Filtros");
        filtros.setStyle("-fx-background-color: white; -fx-text-fill: #4D6B58; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        filtros.setOnAction(e -> mostrarFiltros());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Botón principal de acción: Verde medio elegante
        Button nuevo = new Button("+ Nuevo documento");
        nuevo.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 10px 20px; -fx-font-weight: bold; -fx-cursor: hand;");
        nuevo.setOnAction(e -> nuevoDocumento());

        toolbar.getChildren().addAll(tSec, busquedaField, filtros, sp, nuevo);

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No se encontraron documentos"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Documento, String> cTit = new TableColumn<>("Título");
        cTit.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        cTit.setPrefWidth(300);

        TableColumn<Documento, String> cTip = new TableColumn<>("Tipo");
        cTip.setCellValueFactory(d -> d.getValue().getTipo() != null ?
                javafx.beans.binding.Bindings.createStringBinding(() -> d.getValue().getTipo().getEtiqueta()) : null);

        TableColumn<Documento, LocalDate> cFec = new TableColumn<>("Fecha");
        cFec.setCellValueFactory(new PropertyValueFactory<>("fechaExacta"));
        cFec.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate f, boolean e) {
                super.updateItem(f, e);
                setText((e || f == null) ? null : f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        TableColumn<Documento, Integer> cAni = new TableColumn<>("Año aprox.");
        cAni.setCellValueFactory(new PropertyValueFactory<>("anioAproximado"));
        cAni.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer a, boolean e) {
                super.updateItem(a, e);
                setText((e || a == null) ? null : "c. " + a);
            }
        });

        TableColumn<Documento, String> cUbi = new TableColumn<>("Ubicación física");
        cUbi.setCellValueFactory(new PropertyValueFactory<>("ubicacionFisica"));

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
        estadoLabel = new Label("Listo");
        estadoLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        estadoLabel.setStyle("-fx-text-fill: #94A89A;");
        statusBar.getChildren().add(estadoLabel);

        tableContainer.getChildren().addAll(toolbar, tabla, statusBar);
        sec.getChildren().add(tableContainer);
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
                Platform.runLater(() -> { cargarDocumentos(); actualizarDashboard(); })).mostrar();
    }

    @Override
    public void stop() {
        if (context != null) context.close();
    }
}