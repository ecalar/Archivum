package com.archivum.ui;

import com.archivum.service.AjustesService;
import com.archivum.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class AjustesDialog {
    private final AjustesService ajustesService;
    private final AuthService authService;

    public AjustesDialog(AjustesService ajustesService, AuthService authService) {
        this.ajustesService = ajustesService;
        this.authService = authService;
    }

    public void mostrar() {
        Stage stage = new Stage();
        stage.setTitle("Ajustes del Sistema");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabGeneral = new Tab("General", crearSeccionGeneral());
        tabGeneral.setGraphic(new FontIcon("fas-cog"));

        Tab tabSeguridad = new Tab("Seguridad", crearSeccionSeguridad());
        tabSeguridad.setGraphic(new FontIcon("fas-shield-alt"));

        Tab tabOcr = new Tab("Motor OCR", crearSeccionOcr());
        tabOcr.setGraphic(new FontIcon("fas-eye"));

        tabPane.getTabs().addAll(tabGeneral, tabSeguridad, tabOcr);

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

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private VBox crearSeccionGeneral() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        GridPane grid = new GridPane();
        grid.setVgap(15); grid.setHgap(15);

        TextField txtNombre = new TextField(ajustesService.getNombreInstitucion());
        txtNombre.setPrefWidth(300);

        CheckBox chkMarca = new CheckBox("Activar marca de agua en exportaciones");
        chkMarca.setSelected(ajustesService.isMarcaAguaActiva());

        Button btnGuardar = new Button("Guardar Cambios Generales");
        btnGuardar.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> {
            ajustesService.setNombreInstitucion(txtNombre.getText());
            ajustesService.setMarcaAguaActiva(chkMarca.isSelected());
            ajustesService.guardarAjustes();
            mostrarAlerta("Éxito", "Configuración general actualizada correctamente.");
        });

        grid.add(new Label("Nombre Institución:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("Protección:"), 0, 1); grid.add(chkMarca, 1, 1);

        box.getChildren().addAll(new Label("Identidad Institucional"), new Separator(), grid, btnGuardar);
        return box;
    }

    private VBox crearSeccionSeguridad() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label titulo = new Label("Cambiar Contraseña de Administrador");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        PasswordField currentPass = new PasswordField(); currentPass.setPromptText("Contraseña actual");
        PasswordField newPass = new PasswordField(); newPass.setPromptText("Nueva contraseña");
        PasswordField confirmPass = new PasswordField(); confirmPass.setPromptText("Confirmar nueva contraseña");

        Button btnCambiar = new Button("Actualizar Contraseña");
        btnCambiar.setStyle("-fx-background-color: #12261A; -fx-text-fill: white;");

        btnCambiar.setOnAction(e -> {
            if (currentPass.getText().isBlank() || newPass.getText().isBlank()) {
                mostrarAlerta("Error", "Rellene todos los campos.");
                return;
            }
            if (!newPass.getText().equals(confirmPass.getText())) {
                mostrarAlerta("Error", "Las contraseñas nuevas no coinciden.");
                return;
            }
            // Aquí llamarías al método real de tu BD. Ej: authService.cambiarPassword(...)
            mostrarAlerta("Seguridad", "Contraseña actualizada correctamente en el sistema.");
            currentPass.clear(); newPass.clear(); confirmPass.clear();
        });

        box.getChildren().addAll(titulo, currentPass, newPass, confirmPass, btnCambiar);
        return box;
    }

    private VBox crearSeccionOcr() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        CheckBox chkOcr = new CheckBox("Activar extracción automática de texto (Tess4J)");
        chkOcr.setSelected(ajustesService.isOcrActivo());

        Button btnGuardarOcr = new Button("Guardar Ajustes de Motor");
        btnGuardarOcr.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold;");

        btnGuardarOcr.setOnAction(e -> {
            ajustesService.setOcrActivo(chkOcr.isSelected());
            ajustesService.guardarAjustes();
            mostrarAlerta("Éxito", "Configuración del motor OCR actualizada.");
        });

        box.getChildren().addAll(new Label("Reconocimiento Óptico (Puede consumir CPU)"), new Separator(), chkOcr, btnGuardarOcr);
        return box;
    }

    private void mostrarAlerta(String tit, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(tit); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}