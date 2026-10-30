package com.archivum.ui;

import com.archivum.service.AjustesService;
import com.archivum.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class AjustesDialog {

    private final AjustesService ajustesService;
    private final AuthService authService;
    private final Stage parentStage;
    private Stage stage;

    public AjustesDialog(AjustesService ajustesService, AuthService authService, Stage parentStage) {
        this.ajustesService = ajustesService;
        this.authService = authService;
        this.parentStage = parentStage;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Ajustes del Sistema");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        boolean esAdmin = authService.isAdministrador();

        // Pestaña General — solo admin
        if (esAdmin) {
            Tab tabGeneral = new Tab("General", crearSeccionGeneral());
            tabGeneral.setGraphic(new FontIcon("fas-cog"));
            tabPane.getTabs().add(tabGeneral);
        }

        // Pestaña Seguridad — todos
        Tab tabSeguridad = new Tab("Seguridad", crearSeccionSeguridad());
        tabSeguridad.setGraphic(new FontIcon("fas-shield-alt"));
        tabPane.getTabs().add(tabSeguridad);

        // Pestaña OCR — solo admin
        if (esAdmin) {
            Tab tabOcr = new Tab("Motor OCR", crearSeccionOcr());
            tabOcr.setGraphic(new FontIcon("fas-eye"));
            tabPane.getTabs().add(tabOcr);
        }

        VBox root = new VBox(20, tabPane);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: white;");

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 20, 20, 20));

        Button btnCerrar = new Button("Cerrar Panel");
        btnCerrar.setOnAction(e -> stage.close());
        btnCerrar.setStyle("-fx-background-color: #F4F7F5; -fx-text-fill: #4D6B58; -fx-background-radius: 6px; -fx-padding: 10 25; -fx-cursor: hand;");

        footer.getChildren().add(btnCerrar);
        root.getChildren().add(footer);

        Scene scene = new Scene(root, 600, 520);
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(520);

        stage.showAndWait();
    }

    private VBox crearSeccionGeneral() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);

        // 1) Campos de texto y controles PRIMERO
        TextField txtNombre = new TextField(ajustesService.getNombreInstitucion());
        txtNombre.setPrefWidth(300);

        CheckBox chkMarca = new CheckBox("Activar marca de agua en exportaciones");
        chkMarca.setSelected(ajustesService.isMarcaAguaActiva());

        // 2) Selector de logo (también ANTES del btnGuardar)
        TextField txtLogo = new TextField(ajustesService.getLogoRuta());
        txtLogo.setEditable(false);
        txtLogo.setPrefWidth(240);
        txtLogo.setPromptText("Ningún logo seleccionado");

        Button btnElegirLogo = new Button("Elegir...");
        btnElegirLogo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar logo institucional");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) txtLogo.setText(f.getAbsolutePath());
        });

        Button btnQuitarLogo = new Button("Quitar");
        btnQuitarLogo.setOnAction(e -> txtLogo.setText(""));

        HBox logoRow = new HBox(8, txtLogo, btnElegirLogo, btnQuitarLogo);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        // 3) Botón guardar DESPUÉS de que existan todos los controles que va a leer
        Button btnGuardar = new Button("Guardar Cambios Generales");
        btnGuardar.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnGuardar.setOnAction(e -> {
            ajustesService.setNombreInstitucion(txtNombre.getText());
            ajustesService.setMarcaAguaActiva(chkMarca.isSelected());
            ajustesService.setLogoRuta(txtLogo.getText());
            ajustesService.guardarAjustes();
            mostrarAlerta("Éxito", "Configuración general actualizada correctamente.");
        });

        // 4) Layout del grid
        grid.add(new Label("Nombre Institución:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Protección:"), 0, 1);
        grid.add(chkMarca, 1, 1);
        grid.add(new Label("Logo Institucional:"), 0, 2);
        grid.add(logoRow, 1, 2);

        box.getChildren().addAll(
                new Label("Identidad Institucional"),
                new Separator(),
                grid,
                btnGuardar);
        return box;
    }

    private VBox crearSeccionSeguridad() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label titulo = new Label("Cambiar Contraseña");
        titulo.setGraphic(crearIcono("fas-key", "#12261A", 14));
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        PasswordField currentPass = new PasswordField(); currentPass.setPromptText("Contraseña actual");
        PasswordField newPass = new PasswordField();     newPass.setPromptText("Nueva contraseña");
        PasswordField confirmPass = new PasswordField(); confirmPass.setPromptText("Confirmar nueva contraseña");

        Button btnCambiar = new Button("Actualizar Contraseña");
        btnCambiar.setGraphic(crearIcono("fas-save", "white", 14));
        btnCambiar.setStyle("-fx-background-color: #12261A; -fx-text-fill: white; -fx-cursor: hand;");
        btnCambiar.setOnAction(e -> {
            if (currentPass.getText().isBlank() || newPass.getText().isBlank()) {
                mostrarAlerta("Error", "Rellene todos los campos.");
                return;
            }
            if (!newPass.getText().equals(confirmPass.getText())) {
                mostrarAlerta("Error", "Las contraseñas nuevas no coinciden.");
                return;
            }
            try {
                authService.cambiarPasswordActual(currentPass.getText(), newPass.getText());
                mostrarAlerta("Seguridad", "Contraseña actualizada correctamente.");
                currentPass.clear(); newPass.clear(); confirmPass.clear();
            } catch (Exception ex) {
                mostrarAlerta("Error", ex.getMessage());
            }
        });

        box.getChildren().addAll(titulo, currentPass, newPass, confirmPass, btnCambiar);

        if (authService.isAdministrador()) {
            Label titulo2 = new Label("Gestión de Usuarios");
            titulo2.setGraphic(crearIcono("fas-users", "#2A6B44", 14));
            titulo2.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 20 0 0 0;");

            Button btnGestion = new Button("Administrar usuarios del sistema");
            btnGestion.setGraphic(crearIcono("fas-user-cog", "white", 14));
            btnGestion.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnGestion.setOnAction(e -> new GestionUsuariosDialog(authService, stage).mostrar());

            box.getChildren().addAll(titulo2, new Separator(), btnGestion);
        }

        return box;
    }

    private VBox crearSeccionOcr() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        CheckBox chkOcr = new CheckBox("Activar extracción automática de texto (Tess4J)");
        chkOcr.setSelected(ajustesService.isOcrActivo());

        Button btnGuardarOcr = new Button("Guardar Ajustes de Motor");
        btnGuardarOcr.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnGuardarOcr.setOnAction(e -> {
            ajustesService.setOcrActivo(chkOcr.isSelected());
            ajustesService.guardarAjustes();
            mostrarAlerta("Éxito", "Configuración del motor OCR actualizada.");
        });

        box.getChildren().addAll(
                new Label("Reconocimiento Óptico (Puede consumir CPU)"),
                new Separator(),
                chkOcr,
                btnGuardarOcr);
        return box;
    }

    private FontIcon crearIcono(String literal, String colorHex, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(colorHex));
        return icon;
    }

    private void mostrarAlerta(String tit, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        if (stage != null) a.initOwner(stage);
        a.setTitle(tit);
        a.setHeaderText(null);
        a.setContentText(msg);

        a.getDialogPane().setPrefWidth(450);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.getDialogPane().applyCss();
        a.getDialogPane().layout();

        a.showAndWait();
    }
}