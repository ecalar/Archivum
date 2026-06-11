package com.archivum;

import com.archivum.ui.MainWindow;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArchivumApplication {

    public static void main(String[] args) {
        Application.launch(MainWindow.class, args);
    }
}