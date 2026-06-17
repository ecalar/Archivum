package com.archivum.ui;

import com.archivum.service.BackupService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BackupDialog {

    private final BackupService backupService;
    private final ConfigurableApplicationContext context;
    private final Stage parentStage;
    private final Runnable onComplete;
    private Label estadoLabel;
    private Stage stage;

    public BackupDialog(BackupService backupService, ConfigurableApplicationContext context, Stage parentStage, Runnable onComplete) {
        this.backupService = backupService;
        this.context = context;
        this.parentStage = parentStage;
        this.onComplete = onComplete;
    }

    public void mostrar() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Gestión de Copias de Seguridad");
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #F4F7F5;");

        Label titulo = new Label("Copias de Seguridad");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titulo.setStyle("-fx-text-fill: #12261A;");
        Label subtitulo = new Label("Exporte o restaure el estado de la base de datos y los documentos físicos.");
        subtitulo.setStyle("-fx-text-fill: #6B7A70;");
        VBox header = new VBox(5, titulo, subtitulo);
        header.setAlignment(Pos.CENTER);

        HBox tarjetas = new HBox(20);
        tarjetas.setAlignment(Pos.CENTER);

        VBox cardCrear = crearTarjetaAccion("Crear Copia", "fas-file-export", "Genera un archivo comprimido (.zip) con todos los documentos y la base de datos actual.", "Generar Backup", "#2A6B44");
        ((Button) cardCrear.getChildren().get(3)).setOnAction(e -> generarBackup());

        VBox cardRestaurar = crearTarjetaAccion("Restaurar Copia", "fas-file-import", "Sustituye los datos actuales por una copia de seguridad anterior. Requiere reiniciar.", "Cargar Archivo", "#8A3636");
        ((Button) cardRestaurar.getChildren().get(3)).setOnAction(e -> restaurarBackup());

        tarjetas.getChildren().addAll(cardCrear, cardRestaurar);

        estadoLabel = new Label("");
        estadoLabel.setStyle("-fx-text-fill: #CFA144; -fx-font-weight: bold;");

        Button btnCerrar = new Button("Cerrar panel");
        btnCerrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7A70; -fx-border-color: #D1DED5; -fx-border-radius: 6px; -fx-padding: 8px 20px; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> stage.close());

        HBox footer = new HBox(estadoLabel, new Region(), btnCerrar);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, new Separator(), tarjetas, new Separator(), footer);

        Scene scene = new Scene(root, 650, 450);
        stage.setScene(scene);
        stage.show();
    }

    private VBox crearTarjetaAccion(String tit, String iconoStr, String desc, String textBtn, String color) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(18, 38, 26, 0.08), 15, 0, 0, 5);");

        FontIcon icon = new FontIcon(iconoStr);
        icon.setIconSize(40);
        icon.setIconColor(Color.web(color));

        Label lTit = new Label(tit);
        lTit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lTit.setStyle("-fx-text-fill: #12261A;");

        Label lDesc = new Label(desc);
        lDesc.setWrapText(true);
        lDesc.setStyle("-fx-text-fill: #6B7A70; -fx-text-alignment: center;");
        lDesc.setPrefHeight(60);

        Button btn = new Button(textBtn);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-background-radius: 6px; -fx-cursor: hand;");

        card.getChildren().addAll(icon, lTit, lDesc, btn);
        return card;
    }

    private void generarBackup() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar destino de la copia");
        File carpetaDestino = dc.showDialog(stage);

        if (carpetaDestino == null) return;
        estadoLabel.setText("Empaquetando documentos... Por favor espere.");

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
            estadoLabel.setText("¡Copia guardada con éxito!");
            if (onComplete != null) onComplete.run();
        });
        task.setOnFailed(e -> {
            estadoLabel.setText("Error crítico al empaquetar el backup.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void restaurarBackup() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo de Backup (.zip)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos ZIP", "*.zip"));
        File archivoZip = fc.showOpenDialog(stage);

        if (archivoZip == null) return;

        Alert confirm = new Alert(Alert.AlertType.WARNING,
                "La restauración reemplazará por completo la base de datos y archivos actuales.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("¡Atención: Restauración inminente!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                estadoLabel.setText("Procesando script de restauración...");

                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        // 1. Cerrar Spring y desconectar HikariCP
                        context.close();
                        Thread.sleep(1500);

                        // 2. Destruir DB antigua
                        File oldDb = new File("./data/archivum.mv.db");
                        if (oldDb.exists()) oldDb.delete();
                        File oldTrace = new File("./data/archivum.trace.db");
                        if (oldTrace.exists()) oldTrace.delete();

                        // 3. Extraer contenido
                        File scriptExtraido = null;
                        try (ZipFile zipFile = new ZipFile(archivoZip)) {
                            Enumeration<? extends ZipEntry> entries = zipFile.entries();
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                String nombreEntrada = entry.getName();

                                if (nombreEntrada.endsWith(".mv.db") || nombreEntrada.contains("database/")) continue;
                                if (nombreEntrada.startsWith("documentos/")) nombreEntrada = nombreEntrada.replace("documentos/", "archivos/");

                                File archivoDestino = new File("./", nombreEntrada);

                                if (entry.isDirectory()) {
                                    archivoDestino.mkdirs();
                                } else {
                                    File padre = archivoDestino.getParentFile();
                                    if (padre != null && !padre.exists()) padre.mkdirs();
                                    try (InputStream in = zipFile.getInputStream(entry);
                                         FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                                        byte[] buffer = new byte[8192];
                                        int length;
                                        while ((length = in.read(buffer)) != -1) fos.write(buffer, 0, length);
                                        fos.flush();
                                        fos.getFD().sync();
                                    }

                                    if (nombreEntrada.equals("backup.sql")) {
                                        scriptExtraido = archivoDestino;
                                    }
                                }
                            }
                        }

                        // 4. INYECCIÓN SEGURA (CORREGIDA)
                        if (scriptExtraido != null && scriptExtraido.exists()) {
                            Class.forName("org.h2.Driver");
                            try (Connection conn = DriverManager.getConnection("jdbc:h2:file:./data/archivum", "SA", "")) {
                                try (Statement stmt = conn.createStatement()) {
                                    // 4.1. Limpiamos cualquier esquema
                                    stmt.execute("DROP ALL OBJECTS");
                                    // 4.2. Usamos la ruta absoluta del script para que H2 no se pierda
                                    stmt.execute("RUNSCRIPT FROM '" + scriptExtraido.getAbsolutePath().replace("\\", "/") + "'");
                                    // 4.3. FORZAMOS A H2 A GUARDAR LA RAM EN EL DISCO DURO
                                    stmt.execute("SHUTDOWN COMPACT");
                                }
                            }
                            scriptExtraido.delete();
                        }

                        Thread.sleep(1000);
                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    Alert aviso = new Alert(Alert.AlertType.INFORMATION);
                    aviso.setTitle("Restauración de Copia");
                    aviso.setHeaderText("Proceso completado con éxito");
                    aviso.setContentText("Los scripts de texto y el repositorio físico se han restaurado de forma inmaculada.\n\nAl hacer clic en Aceptar, la aplicación se reiniciará de forma automática.");
                    aviso.showAndWait();

                    try {
                        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                        String classpath = System.getProperty("java.class.path");
                        String mainClass = "com.archivum.Launcher";

                        new ProcessBuilder(javaBin, "-cp", classpath, mainClass).start();

                        // CIERRE SEGURO (CORREGIDO): Permite que Linux Mint procese los archivos pendientes
                        Platform.exit();
                        System.exit(0);
                    } catch (Exception ex) {
                        estadoLabel.setText("Restaurado. Por favor, abra desde el Launcher.");
                    }
                });

                task.setOnFailed(e -> {
                    Platform.runLater(() -> {
                        Throwable ex = task.getException();
                        if (ex != null) ex.printStackTrace();
                        estadoLabel.setText("Error al aplicar el volcado SQL.");
                    });
                });

                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();
            }
        });
    }
}