package com.archivum.ui;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.service.AuthService;
import com.archivum.service.DocumentoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CategoriaDocumentosDialog {

    private final DocumentoService service;
    private final AuthService authService;
    private final TipoDocumento tipo;
    private final Stage parentStage;
    private Stage stage;
    private TableView<Documento> tabla;
    private ObservableList<Documento> documentosList;
    private TextField busquedaField;
    private Label estadoLabel;

    public CategoriaDocumentosDialog(DocumentoService service,
                                     AuthService authService,
                                     TipoDocumento tipo,
                                     Stage parentStage) {
        this.service = service;
        this.authService = authService;
        this.tipo = tipo;
        this.parentStage = parentStage;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(tipo.getEtiqueta() + " - Archivum");
        stage.setMinWidth(1000);
        stage.setMinHeight(620);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 32, 20, 32));
        root.setStyle("-fx-background-color: #F4F7F5;");

        // --- CABECERA ---
        Label tit = new Label(tipo.getEtiqueta());
        tit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        tit.setStyle("-fx-text-fill: #12261A;");

        Label subt = new Label("Documentos disponibles para consulta");
        subt.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subt.setStyle("-fx-text-fill: #6B7A70;");

        VBox headerBox = new VBox(3, tit, subt);

        // --- TOOLBAR ---
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        busquedaField = new TextField();
        busquedaField.setPromptText("Buscar por título o descripción...");
        busquedaField.setPrefWidth(320);
        busquedaField.textProperty().addListener((o, ov, nv) -> buscar());

        Button filtros = new Button("Filtros");
        filtros.setStyle("-fx-background-color: white; -fx-text-fill: #4D6B58; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand; -fx-font-weight: bold;");
        filtros.setOnAction(e -> mostrarFiltros());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #12261A; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-weight: bold;");
        btnCerrar.setOnAction(e -> stage.close());

        toolbar.getChildren().addAll(busquedaField, filtros, sp, btnCerrar);

        // --- TABLA ---
        VBox tableContainer = new VBox(15);
        tableContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-padding: 20px; -fx-effect: dropshadow(three-pass-box, rgba(18, 38, 26, 0.06), 15, 0, 0, 5);");
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay documentos en esta categoría"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<Documento, String> cTit = new TableColumn<>("Título");
        cTit.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        cTit.setPrefWidth(300);

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

        tabla.getColumns().addAll(cTit, cFec, cAni, cUbi);

        tabla.setRowFactory(tv -> {
            TableRow<Documento> r = new TableRow<>();
            r.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !r.isEmpty()) {
                    new DocumentoDetailDialog(service, authService, stage, r.getItem(),
                            () -> javafx.application.Platform.runLater(this::cargarTodo))
                            .mostrar();
                }
            });
            return r;
        });

        documentosList = FXCollections.observableArrayList();
        tabla.setItems(documentosList);

        estadoLabel = new Label();
        estadoLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        estadoLabel.setStyle("-fx-text-fill: #94A89A;");

        tableContainer.getChildren().addAll(tabla, estadoLabel);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        root.getChildren().addAll(headerBox, toolbar, tableContainer);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        cargarTodo();
        stage.show();
    }

    private void cargarTodo() {
        documentosList.setAll(service.buscar(null, tipo, null, null, null));
        actualizarEstado();
    }

    private void buscar() {
        String t = busquedaField.getText();
        documentosList.setAll(service.buscar(
                (t == null || t.isBlank()) ? null : t,
                tipo, null, null, null));
        actualizarEstado();
    }

    private void mostrarFiltros() {
        // false → sin desplegable de categoría, ya estamos dentro de una
        FiltroDialog f = new FiltroDialog(stage, false);
        if (f.mostrar()) {
            documentosList.setAll(service.buscar(
                    busquedaField.getText(),
                    tipo,               // la categoría siempre es la de esta ventana
                    f.getDesde(),
                    f.getHasta(),
                    f.getAnio()
            ));
            actualizarEstado();
        }
    }

    private void actualizarEstado() {
        int n = documentosList.size();
        estadoLabel.setText(n + (n == 1 ? " documento encontrado" : " documentos encontrados"));
    }
}