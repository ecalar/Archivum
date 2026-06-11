package com.archivum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private final String rutaArchivos;
    private final String rutaBackups;
    private final String rutaData;

    public BackupService(@Value("${app.archivos.ruta}") String rutaArchivos,
                         @Value("${app.backup.ruta}") String rutaBackups) {
        this.rutaArchivos = rutaArchivos;
        this.rutaBackups = rutaBackups;
        this.rutaData = "./data";
    }

    public File crearBackup() throws IOException {
        Files.createDirectories(Paths.get(rutaBackups));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "archivum_backup_" + timestamp + ".zip";
        Path rutaZip = Paths.get(rutaBackups, nombreArchivo);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(rutaZip.toFile()))) {

            // Añadir base de datos
            Path dataPath = Paths.get(rutaData);
            if (Files.exists(dataPath)) {
                Files.walk(dataPath)
                        .filter(Files::isRegularFile)
                        .forEach(archivo -> {
                            try {
                                String entryName = "data/" + dataPath.relativize(archivo).toString();
                                zos.putNextEntry(new ZipEntry(entryName));
                                Files.copy(archivo, zos);
                                zos.closeEntry();
                            } catch (IOException ignored) {}
                        });
            }

            // Añadir archivos documentales
            Path archivosPath = Paths.get(rutaArchivos);
            if (Files.exists(archivosPath)) {
                Files.walk(archivosPath)
                        .filter(Files::isRegularFile)
                        .forEach(archivo -> {
                            try {
                                String entryName = "archivos/" + archivosPath.relativize(archivo).toString();
                                zos.putNextEntry(new ZipEntry(entryName));
                                Files.copy(archivo, zos);
                                zos.closeEntry();
                            } catch (IOException ignored) {}
                        });
            }
        }

        return rutaZip.toFile();
    }

    public void restaurarBackup(File archivoZip) throws IOException {
        // Validar que el ZIP tiene pinta de backup de Archivum
        boolean tieneData = false;
        boolean tieneArchivos = false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("data/")) tieneData = true;
                if (entry.getName().startsWith("archivos/")) tieneArchivos = true;
                zis.closeEntry();
            }
        }

        if (!tieneData || !tieneArchivos) {
            throw new IllegalArgumentException("El archivo seleccionado no es un backup válido de Archivum");
        }

        // Borrar datos actuales
        Path dataPath = Paths.get(rutaData);
        if (Files.exists(dataPath)) {
            Files.walk(dataPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }

        Path archivosPath = Paths.get(rutaArchivos);
        if (Files.exists(archivosPath)) {
            Files.walk(archivosPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }

        // Extraer backup
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path destino = Paths.get(".", entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(destino);
                } else {
                    Files.createDirectories(destino.getParent());
                    Files.copy(zis, destino, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}