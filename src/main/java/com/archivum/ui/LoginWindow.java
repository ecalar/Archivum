package com.archivum.ui;

import com.archivum.service.AuthService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class LoginWindow {

    private final AuthService authService;
    private final ConfigurableApplicationContext context;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button btnLogin;

    public LoginWindow(AuthService authService, ConfigurableApplicationContext context) {
        this.authService = authService;
        this.context = context;
    }

    public void iniciar(Stage stage) {
        stage.setTitle("Archivum - Acceso");

        cargarIconoVentana(stage);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #12261A;"); // Fondo Verde Bosque Institucional
        root.setPadding(new Insets(40));

        VBox container = new VBox(24);
        container.setAlignment(Pos.CENTER);

        // --- SECCIÓN LOGO OFICIAL ---
        VBox logoBox = new VBox(12);
        logoBox.setAlignment(Pos.CENTER);

        ImageView logoView = new ImageView();
        Image img = cargarImagenLogo();
        if (img != null) {
            logoView.setImage(img);
            logoView.setFitWidth(80);
            logoView.setFitHeight(80);
            logoView.setPreserveRatio(true);
            logoBox.getChildren().add(logoView);
        } else {
            Label fallbackLogo = new Label("A");
            fallbackLogo.setFont(Font.font("Serif", FontWeight.BOLD, 54));
            fallbackLogo.setStyle("-fx-text-fill: #CFA144;");
            logoBox.getChildren().add(fallbackLogo);
        }

        Label tituloLabel = new Label("ARCHIVUM");
        tituloLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        tituloLabel.setStyle("-fx-text-fill: #F4F7F5; -fx-letter-spacing: 3px;");

        Label subLabel = new Label("Archivo Documental Organizado");
        subLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        subLabel.setStyle("-fx-text-fill: #CFA144;"); // Acento Oro

        logoBox.getChildren().addAll(tituloLabel, subLabel);

        // --- FORMULARIO DE ACCESO MODERNO ---
        VBox formBox = new VBox(16);
        formBox.setPadding(new Insets(20, 0, 10, 0));

        String fieldStyle = "-fx-background-color: #1A3324; -fx-text-fill: white; -fx-prompt-text-fill: #6B7A70; -fx-padding: 12px; -fx-background-radius: 6px; -fx-border-color: #1E3B2A; -fx-border-radius: 6px; -fx-font-size: 14px;";

        usernameField = new TextField();
        usernameField.setPromptText("Nombre de usuario");
        usernameField.setStyle(fieldStyle);

        passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña del sistema");
        passwordField.setStyle(fieldStyle);

        formBox.getChildren().addAll(usernameField, passwordField);

        // --- BOTONERA Y ALERTAS ---
        errorLabel = new Label("Credenciales incorrectas");
        errorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        errorLabel.setStyle("-fx-text-fill: #EF4444;");
        errorLabel.setVisible(false);

        btnLogin = new Button("INICIAR SESIÓN");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-background-color: #CFA144; -fx-text-fill: #12261A; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 14px; -fx-background-radius: 6px; -fx-cursor: hand;");

        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle("-fx-background-color: #B38E36; -fx-text-fill: #12261A; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 14px; -fx-background-radius: 6px; -fx-cursor: hand;"));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle("-fx-background-color: #CFA144; -fx-text-fill: #12261A; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 14px; -fx-background-radius: 6px; -fx-cursor: hand;"));

        btnLogin.setOnAction(e -> ejecutarAutenticacion());

        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) ejecutarAutenticacion(); });

        container.getChildren().addAll(logoBox, formBox, errorLabel, btnLogin);
        root.getChildren().add(container);

        Scene scene = new Scene(root, 450, 620);
        stage.setScene(scene);

        stage.setMinWidth(450);
        stage.setMaxWidth(450);
        stage.setMinHeight(620);
        stage.setMaxHeight(620);

        stage.centerOnScreen();
        stage.show();
    }

    private void ejecutarAutenticacion() {
        errorLabel.setVisible(false);
        boolean exito = authService.login(usernameField.getText().trim(), passwordField.getText());
        if (exito) {
            Platform.runLater(() -> {
                Stage mainStage = new Stage();
                MainWindow mainWindow = context.getBean(MainWindow.class);
                mainWindow.iniciar(mainStage);
                ((Stage) usernameField.getScene().getWindow()).close();
            });
        } else {
            errorLabel.setVisible(true);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    private void cargarIconoVentana(Stage stage) {
        try {
            var s = getClass().getResourceAsStream("/images/icono.png");
            if (s != null) stage.getIcons().add(new Image(s));
        } catch (Exception ignored) {}
    }

    private Image cargarImagenLogo() {
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
}