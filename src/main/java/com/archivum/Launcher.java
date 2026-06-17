package com.archivum;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("glass.gtk.uiInput", "false");

        ArchivumApplication.main(args);
    }
}