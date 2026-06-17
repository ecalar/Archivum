package com.archivum;

import com.archivum.ui.LoginWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ArchivumApplication extends Application {

    private static ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = SpringApplication.run(ArchivumApplication.class);
    }

    @Override
    public void start(Stage stage) {
        // Solución: Pedir el Bean a Spring y usar el método iniciar(stage)
        LoginWindow loginWindow = context.getBean(LoginWindow.class);
        loginWindow.iniciar(stage);
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}