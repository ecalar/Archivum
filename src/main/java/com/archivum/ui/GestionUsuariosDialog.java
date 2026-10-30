package com.archivum.ui;

import com.archivum.model.RolUsuario;
import com.archivum.model.Usuario;
import com.archivum.service.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class GestionUsuariosDialog {

    private final AuthService authService;
    private final Stage parentStage;
    private Stage stage;
    private TableView<Usuario> tabla;
    private Label lblMensaje;

    public GestionUsuariosDialog(AuthService authService, Stage parentStage) {
        this.authService = authService;
        this.parentStage = parentStage;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Archivum - Gestión de Usuarios");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        // --- TÍTULO CON ICONO ---
        Label titulo = new Label("Administración de Usuarios");
        titulo.setGraphic(crearIcono("fas-users", "#12261A", 18));
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #12261A;");

        // --- ETIQUETA DE ESTADO ---
        lblMensaje = new Label();
        lblMensaje.setVisible(false);
        lblMensaje.setContentDisplay(ContentDisplay.LEFT);

        // --- TABLA ---
        tabla = new TableView<>();
        configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // --- ACCIONES SOBRE LA SELECCIÓN ---
        Button btnCambiarPass = new Button("Cambiar contraseña");
        btnCambiarPass.setGraphic(crearIcono("fas-key", "#12261A", 14));
        btnCambiarPass.setStyle("-fx-background-color: #CFA144; -fx-text-fill: #12261A; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6px;");
        btnCambiarPass.setOnAction(e -> cambiarPasswordSeleccionado());

        Button btnEliminar = new Button("Eliminar seleccionado");
        btnEliminar.setGraphic(crearIcono("fas-trash-alt", "white", 14));
        btnEliminar.setStyle("-fx-background-color: #B91C1C; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6px;");
        btnEliminar.setOnAction(e -> eliminarSeleccionado());

        HBox acciones = new HBox(10, btnCambiarPass, btnEliminar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        // --- FORMULARIO NUEVO USUARIO ---
        Label lblNuevo = new Label("Crear nuevo usuario");
        lblNuevo.setGraphic(crearIcono("fas-user-plus", "#2A6B44", 14));
        lblNuevo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2A6B44;");

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Nombre de usuario");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña");

        ComboBox<RolUsuario> cmbRol = new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        cmbRol.getSelectionModel().select(RolUsuario.CONSULTOR);
        cmbRol.setPrefWidth(150);

        Button btnCrear = new Button("Crear Usuario");
        btnCrear.setGraphic(crearIcono("fas-check", "white", 14));
        btnCrear.setStyle("-fx-background-color: #2A6B44; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6px;");
        btnCrear.setOnAction(e -> {
            try {
                authService.crearUsuario(
                        txtUsername.getText().trim(),
                        txtPassword.getText(),
                        cmbRol.getValue()
                );
                txtUsername.clear();
                txtPassword.clear();
                cmbRol.getSelectionModel().select(RolUsuario.CONSULTOR);
                mostrarExito("Usuario creado correctamente.");
                refrescarTabla();
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });

        HBox formNuevo = new HBox(10, txtUsername, txtPassword, cmbRol, btnCrear);
        formNuevo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtUsername, Priority.ALWAYS);
        HBox.setHgrow(txtPassword, Priority.ALWAYS);

        root.getChildren().addAll(
                titulo, lblMensaje, tabla, acciones,
                new Separator(), lblNuevo, formNuevo
        );

        Scene scene = new Scene(root, 780, 540);
        stage.setScene(scene);
        stage.setMinWidth(780);
        stage.setMinHeight(540);

        refrescarTabla();
        stage.showAndWait();
    }

    private FontIcon crearIcono(String literal, String colorHex, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(colorHex));
        return icon;
    }

    private void configurarTabla() {
        TableColumn<Usuario, String> colUser = new TableColumn<>("Usuario");
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colUser.setPrefWidth(260);

        TableColumn<Usuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRol() == RolUsuario.ADMINISTRADOR ? "Administrador" : "Consultor"));
        colRol.setPrefWidth(160);

        tabla.getColumns().addAll(colUser, colRol);
        tabla.setPlaceholder(new Label("No hay usuarios registrados."));
    }

    private void refrescarTabla() {
        tabla.setItems(FXCollections.observableArrayList(authService.listarUsuarios()));
        tabla.refresh();
    }

    private void eliminarSeleccionado() {
        Usuario u = tabla.getSelectionModel().getSelectedItem();
        if (u == null) { mostrarError("Seleccione un usuario."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al usuario \"" + u.getUsername() + "\"?",
                ButtonType.YES, ButtonType.NO);
        conf.initOwner(stage);
        conf.setHeaderText(null);
        if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            authService.eliminarUsuario(u.getId());
            mostrarExito("Usuario eliminado.");
            refrescarTabla();
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void cambiarPasswordSeleccionado() {
        Usuario u = tabla.getSelectionModel().getSelectedItem();
        if (u == null) { mostrarError("Seleccione un usuario."); return; }

        Dialog<String> dlg = new Dialog<>();
        dlg.initOwner(stage);
        dlg.setTitle("Cambiar contraseña");
        dlg.setHeaderText("Nueva contraseña para: " + u.getUsername());

        PasswordField pf = new PasswordField();
        pf.setPromptText("Nueva contraseña");
        VBox box = new VBox(8,
                new Label("Contraseña (mínimo 4 caracteres):"),
                pf);
        box.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(box);

        ButtonType btnOk = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);
        dlg.setResultConverter(bt -> bt == btnOk ? pf.getText() : null);

        dlg.showAndWait().ifPresent(nueva -> {
            try {
                authService.cambiarPassword(u.getId(), nueva);
                mostrarExito("Contraseña actualizada.");
            } catch (Exception ex) {
                mostrarError(ex.getMessage());
            }
        });
    }

    private void mostrarError(String msg) {
        FontIcon icon = crearIcono("fas-times-circle", "#B91C1C", 14);
        lblMensaje.setGraphic(icon);
        lblMensaje.setText(msg);
        lblMensaje.setTextFill(Color.web("#B91C1C"));
        lblMensaje.setStyle("-fx-font-size: 12px;");
        lblMensaje.setVisible(true);
    }

    private void mostrarExito(String msg) {
        FontIcon icon = crearIcono("fas-check-circle", "#2A6B44", 14);
        lblMensaje.setGraphic(icon);
        lblMensaje.setText(msg);
        lblMensaje.setTextFill(Color.web("#2A6B44"));
        lblMensaje.setStyle("-fx-font-size: 12px;");
        lblMensaje.setVisible(true);
    }
}