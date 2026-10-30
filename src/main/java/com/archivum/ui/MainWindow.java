package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.service.AjustesService;
import com.archivum.service.AuthService;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class MainWindow extends Application {

    private final ConfigurableApplicationContext context;
    private final DocumentoService service;
    private final BackupService backupService;
    private final AjustesService ajustesService;
    private final AuthService authService;

    private Stage stage;
    private TableView<Documento> tabla;
    private ObservableList<Documento> documentosList;
    private Label totalDocsLabel;
    private Label papeleraCountLabel;
    private Label ultimoBackupLabel;
    private Label estadoLabel;
    private TextField busquedaField;

    public MainWindow(ConfigurableApplicationContext context) {
        this.context = context;
        this.service = context.getBean(DocumentoService.class);
        this.backupService = context.getBean(BackupService.class);
        this.ajustesService = context.getBean(AjustesService.class);
        this.authService = context.getBean(AuthService.class);
    }

    public void iniciar(Stage stage) {
        this.stage = stage;
        stage.setTitle("Archivum");
        stage.setMinWidth(1200);
        stage.setMinHeight(750);

        cargarIconoVentana();

        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #F4F7F5;");

        mainLayout.setLeft(crearSidebar());

        VBox centerLayout = new VBox();
        centerLayout.getChildren().addAll(crearTopBar(), crearContenido());
        mainLayout.setCenter(centerLayout);

        Scene scene = new Scene(mainLayout);
        var css = getClass().getResource("/estilo.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.setResizable(true);

        // TRUCO LINUX: Inyectamos la resolución física del monitor antes de hacer "show"
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.show();

        // Maximizamos en el ciclo secundario para que los botones de Windows/Linux se acoplen
        Platform.runLater(() -> {
            stage.setMaximized(true);
            stage.toFront();
        });

        stage.setOnCloseRequest(e -> { context.close(); Platform.exit(); });

        actualizarDashboard();
        cargarDocumentos();

    }

    @Override
    public void start(Stage primaryStage) {
        // El ciclo controlado se gestiona desde el Launcher y LoginWindow
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

    private HBox crearTopBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setStyle("-fx-background-color: transparent;");

        FontIcon closeIcon = new FontIcon("fas-times");
        closeIcon.setIconSize(16);
        closeIcon.setIconColor(Color.web("#9CA3AF"));

        Button c = new Button();
        c.setGraphic(closeIcon);
        c.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");

        c.setOnMouseEntered(e -> {
            c.setStyle("-fx-background-color: #FEE2E2; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6px;");
            closeIcon.setIconColor(Color.web("#EF4444"));
        });
        c.setOnMouseExited(e -> {
            c.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");
            closeIcon.setIconColor(Color.web("#9CA3AF"));
        });
        c.setOnAction(e -> { context.close(); Platform.exit(); });

        bar.getChildren().add(c);
        return bar;
    }

    private VBox crearSidebar() {
        VBox bar = new VBox(0);
        bar.setPrefWidth(260);
        bar.setMinWidth(260);
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

        Label subt = new Label(authService.getRolActual());
        subt.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subt.setStyle("-fx-text-fill: #CFA144;");

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
        sep.setStyle("-fx-background-color: #1E3B2A;");

        VBox nav = new VBox(8);
        nav.setPadding(new Insets(25, 12, 20, 12));

        Button btnInicio = btnSidebar("Inicio", "fas-home", true, this::recargarInicio);

        Button btnNuevo = btnSidebar("Nuevo documento", "fas-file-alt", false, this::nuevoDocumento);
        btnNuevo.setVisible(authService.isAdministrador());
        btnNuevo.setManaged(authService.isAdministrador());

        Button btnPapelera = btnSidebar("Papelera", "fas-trash-alt", false, this::abrirPapelera);
        btnPapelera.setVisible(authService.isAdministrador());
        btnPapelera.setManaged(authService.isAdministrador());

        Button btnBackup = btnSidebar("Copias de seguridad", "fas-save", false, () -> new BackupDialog(backupService, context, stage, this::recargarInicio).mostrar());
        btnBackup.setManaged(authService.isAdministrador());
        btnBackup.setVisible(authService.isAdministrador());
        btnBackup.setManaged(authService.isAdministrador());

        Button btnUsuarios = btnSidebar("Gestión de usuarios", "fas-users-cog", false,
                () -> new GestionUsuariosDialog(authService, stage).mostrar());
        btnUsuarios.setVisible(authService.isAdministrador());
        btnUsuarios.setManaged(authService.isAdministrador());

        Button btnAjustes = btnSidebar("Ajustes", "fas-cog", false,
                () -> new AjustesDialog(ajustesService, authService, service, stage).mostrar());
        btnAjustes.setVisible(authService.isAdministrador());
        btnAjustes.setManaged(authService.isAdministrador());

        nav.getChildren().addAll(btnInicio, btnNuevo, btnPapelera, btnBackup, btnUsuarios, btnAjustes);

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox logoutBox = new VBox();
        logoutBox.setPadding(new Insets(0, 12, 5, 12));
        Button btnLogout = btnSidebar("Cerrar sesión", "fas-sign-out-alt", false, this::cerrarSesion);
        logoutBox.getChildren().add(btnLogout);

        VBox footer = new VBox(4);
        footer.setPadding(new Insets(20, 14, 24, 14));
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().addAll(
                lblFooter("v1.0.0"),
                lblFooter(authService.getUsernameActual())
        );

        bar.getChildren().addAll(logoBox, sep, nav, sp, logoutBox, footer);
        return bar;
    }

    private Button btnSidebar(String t, String iconCode, boolean act, Runnable a) {
        Button b = new Button(t);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        b.setOnAction(e -> a.run());

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        b.setGraphic(icon);
        b.setGraphicTextGap(12);

        if (act) {
            icon.setIconColor(Color.web("#CFA144"));
            b.setStyle("-fx-background-color: rgba(207, 161, 68, 0.1); -fx-text-fill: #CFA144; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #CFA144; -fx-border-width: 0 0 0 4px;");
        } else {
            icon.setIconColor(Color.web("#94A89A"));
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A89A; -fx-padding: 12px 18px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 0 4px;");

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

    private VBox crearContenido() {
        VBox c = new VBox(0);
        VBox.setVgrow(c, Priority.ALWAYS);

        if (authService.isAdministrador()) {
            c.getChildren().addAll(crearDashboard(), crearSeccionTabla());
        } else {
            // El consultor no ve la tabla general: navega por categorías
            ScrollPane sp = new ScrollPane(crearDashboard());
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: #F4F7F5; -fx-background: #F4F7F5;");
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            VBox.setVgrow(sp, Priority.ALWAYS);
            c.getChildren().add(sp);
        }
        return c;
    }

    private VBox crearDashboard() {
        VBox db = new VBox(18);
        db.setPadding(new Insets(10, 32, 20, 32));

        boolean esAdmin = authService.isAdministrador();

        // --- CABECERA CON LOGO A LA DERECHA ---
        Label tit = new Label(ajustesService.getNombreInstitucion());
        tit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        tit.setStyle("-fx-text-fill: #12261A;");

        Label subtitulo = new Label(esAdmin
                ? "Panel de administración documental"
                : "Sistema de consulta documental");
        subtitulo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        subtitulo.setStyle("-fx-text-fill: #2A6B44; -fx-font-weight: bold;");

        Label fecha = new Label(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy")));
        fecha.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        fecha.setStyle("-fx-text-fill: #6B7A70;");

        VBox headerText = new VBox(3, tit, subtitulo, fecha);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20, headerText, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView logo = cargarLogoInstitucion();
        if (logo != null) header.getChildren().add(logo);

        db.getChildren().add(header);

        // --- TARJETAS ---
        HBox tarjetas = new HBox(20);
        tarjetas.setPadding(new Insets(10, 0, 10, 0));

        if (esAdmin) {
            VBox t1 = tarjeta("Documentos archivados", "0", "#CFA144");
            totalDocsLabel = (Label) t1.getChildren().get(1);

            VBox t2 = tarjeta("En papelera", "0", "#8A3636");
            papeleraCountLabel = (Label) t2.getChildren().get(1);

            VBox t3 = tarjeta("Última copia de seguridad", "Nunca", "#2A6B44");
            ultimoBackupLabel = (Label) t3.getChildren().get(1);

            tarjetas.getChildren().addAll(t1, t2, t3);
            db.getChildren().add(tarjetas);

        } else {
            Label lblCat = new Label("Categorías de documentos");
            lblCat.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            lblCat.setStyle("-fx-text-fill: #12261A; -fx-padding: 5 0 0 0;");

            FlowPane categorias = crearCategoriasConsulta();
            db.getChildren().addAll(lblCat, categorias);
        }

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

    private ImageView cargarLogoInstitucion() {
        String ruta = ajustesService.getLogoRuta();
        Image img = null;

        if (ruta != null && !ruta.isBlank()) {
            File f = new File(ruta);
            if (f.exists()) {
                try { img = new Image(f.toURI().toString()); }
                catch (Exception ignored) {}
            }
        }
        if (img == null || img.isError()) {
            img = cargarImagenSidebar(); // fallback al icono de la app
        }
        if (img == null) return null;

        ImageView iv = new ImageView(img);
        iv.setFitWidth(80);
        iv.setFitHeight(80);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    private FlowPane categoriasPane;

    private FlowPane crearCategoriasConsulta() {
        categoriasPane = new FlowPane(16, 16);
        categoriasPane.setPadding(new Insets(5, 0, 5, 0));
        categoriasPane.setPrefWrapLength(900);
        refrescarCategorias();
        return categoriasPane;
    }

    private void refrescarCategorias() {
        if (categoriasPane == null) return;
        categoriasPane.getChildren().clear();

        Map<TipoDocumento, Long> porTipo = service.obtenerTodos().stream()
                .filter(d -> d.getTipo() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Documento::getTipo,
                        java.util.stream.Collectors.counting()));

        for (TipoDocumento tipo : TipoDocumento.values()) {
            long count = porTipo.getOrDefault(tipo, 0L);
            categoriasPane.getChildren().add(crearCategoriaCard(tipo, count));
        }
    }

    private VBox crearCategoriaCard(TipoDocumento tipo, long count) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setPrefWidth(200);
        aplicarEstiloCard(card, false);

        Label lblTipo = new Label(tipo.getEtiqueta());
        lblTipo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTipo.setStyle("-fx-text-fill: #12261A;");

        Label lblCount = new Label(String.valueOf(count));
        lblCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblCount.setStyle("-fx-text-fill: #2A6B44;");

        Label lblSuf = new Label(count == 1 ? "documento" : "documentos");
        lblSuf.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        lblSuf.setStyle("-fx-text-fill: #6B7A70;");

        card.getChildren().addAll(lblTipo, lblCount, lblSuf);

        card.setOnMouseEntered(e -> aplicarEstiloCard(card, true));
        card.setOnMouseExited(e -> aplicarEstiloCard(card, false));
        card.setOnMouseClicked(e -> abrirCategoria(tipo));

        return card;
    }

    private void aplicarEstiloCard(VBox card, boolean hover) {
        String bg = hover ? "#F4F7F5" : "#FFFFFF";
        String border = hover ? "#CFA144" : "transparent";
        String shadow = hover
                ? "rgba(18, 38, 26, 0.12), 12, 0, 0, 4"
                : "rgba(18, 38, 26, 0.06), 10, 0, 0, 3";
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10px; "
                + "-fx-effect: dropshadow(three-pass-box, " + shadow + "); "
                + "-fx-cursor: hand; -fx-border-color: " + border
                + "; -fx-border-width: 2px; -fx-border-radius: 10px;");
    }

    private void abrirCategoria(TipoDocumento tipo) {
        new CategoriaDocumentosDialog(service, authService, tipo, stage).mostrar();
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

        Button nuevo = new Button("Nuevo documento");
        FontIcon plusIcon = new FontIcon("fas-plus");
        plusIcon.setIconSize(12);
        plusIcon.setIconColor(Color.WHITE);
        nuevo.setGraphic(plusIcon);
        nuevo.setGraphicTextGap(6);
        nuevo.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 10px 20px; -fx-font-weight: bold; -fx-cursor: hand;");
        nuevo.setOnAction(e -> nuevoDocumento());
        nuevo.setVisible(authService.isAdministrador());
        nuevo.setManaged(authService.isAdministrador());

        toolbar.getChildren().addAll(tSec, busquedaField, filtros, sp, nuevo);

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No se encontraron documentos"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Documento, String> cTit = new TableColumn<>("Título");
        cTit.setCellFactory(col -> new TableCell<>() {
            private final org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon("fas-file-alt");

            {
                icon.setIconSize(11);
                icon.setIconColor(Color.web("#CFA144"));
            }

            @Override
            protected void updateItem(String titulo, boolean empty) {
                super.updateItem(titulo, empty);
                if (empty || titulo == null) { setText(null); setGraphic(null); setTooltip(null); return; }
                Documento d = getTableView().getItems().get(getIndex());
                setText(titulo);

                String busqueda = busquedaField != null ? busquedaField.getText() : null;
                String snippet = SnippetHelper.snippetOcr(d, busqueda);

                if (snippet != null) {
                    setGraphic(icon);
                    Tooltip tip = new Tooltip("Coincidencia en el contenido:\n\n" + snippet);
                    tip.setWrapText(true);
                    tip.setMaxWidth(420);
                    setTooltip(tip);
                } else if (SnippetHelper.tieneOcr(d)) {
                    setGraphic(null);
                    setTooltip(new Tooltip("Documento con texto OCR indexado."));
                } else {
                    setGraphic(null);
                    setTooltip(null);
                }
            }
        });

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

    private void actualizarDashboard() {
        // Total de documentos — siempre se actualiza si la etiqueta existe
        if (totalDocsLabel != null)
            totalDocsLabel.setText(String.valueOf(service.contarDocumentos()));

        // Métricas sensibles — solo existen para el admin
        if (authService.isAdministrador()) {
            if (papeleraCountLabel != null) {
                papeleraCountLabel.setText(String.valueOf(service.contarDocumentosEnPapelera()));
            }
            if (ultimoBackupLabel != null) {
                File bd = new File("./backups");
                if (bd.exists()) {
                    File[] bk = bd.listFiles((d, n) -> n.endsWith(".zip"));
                    if (bk != null && bk.length > 0) {
                        java.util.Arrays.sort(bk,
                                (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                        ultimoBackupLabel.setText(
                                new java.text.SimpleDateFormat("dd/MM/yyyy")
                                        .format(bk[0].lastModified()));
                        return;
                    }
                }
                ultimoBackupLabel.setText("Nunca");
            }
        }
        refrescarCategorias();
    }

    private void cargarDocumentos() {
        if (documentosList != null) {
            documentosList.setAll(service.obtenerTodos());
        }
        actualizarDashboard();
    }

    private void buscar() {
        if (documentosList == null) return;
        String t = busquedaField.getText();
        documentosList.setAll((t == null || t.isBlank()) ? service.obtenerTodos() : service.buscar(t, null, null, null, null));

    }

    private void recargarInicio() {
        cargarDocumentos();
        actualizarDashboard();
    }

    private void mostrarFiltros() {
        if (documentosList == null) return;
        FiltroDialog f = new FiltroDialog(stage);
        if (f.mostrar())
            documentosList.setAll(service.buscar(null, f.getTipo(), f.getDesde(), f.getHasta(), f.getAnio()));
    }

    private void nuevoDocumento() {
        new DocumentoFormDialog(service, stage, null, () -> Platform.runLater(this::cargarDocumentos)).mostrar();
    }

    private void verDetalle(Documento d) {
        new DocumentoDetailDialog(service, authService, stage, d, () -> Platform.runLater(this::cargarDocumentos)).mostrar();
    }

    private void abrirPapelera() {
        new PapeleraDialog(service, stage, () -> Platform.runLater(this::cargarDocumentos)).mostrar();
    }

    private void hacerBackup() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar destino del backup");
        File carpetaDestino = dc.showDialog(stage);
        if (carpetaDestino == null) return;

        estadoLabel.setText("Generando copia de seguridad asíncrona...");

        javafx.concurrent.Task<File> task = new javafx.concurrent.Task<>() {
            @Override
            protected File call() throws Exception {
                File tempBk = backupService.crearBackupCompleto();
                File destFile = new File(carpetaDestino, tempBk.getName());
                java.nio.file.Files.move(tempBk.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return destFile;
            }
        };

        task.setOnSucceeded(e -> {
            estadoLabel.setText("Copia guardada con éxito: " + task.getValue().getName());
            actualizarDashboard();
        });
        task.setOnFailed(e -> estadoLabel.setText("Error crítico al empaquetar el backup."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void cerrarSesion() {
        authService.logout();
        stage.close();
        LoginWindow loginWindow = context.getBean(LoginWindow.class);
        loginWindow.iniciar(new Stage());
    }
}