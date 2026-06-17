package com.archivum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.archivos.ruta:./archivos}")
    private String rutaArchivos;

    private static final String CARPETA_BACKUPS = "./backups";

    public BackupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public File crearBackupCompleto() throws Exception {
        Path dirBackups = Paths.get(CARPETA_BACKUPS);
        if (!Files.exists(dirBackups)) {
            Files.createDirectories(dirBackups);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivoZip = "backup_" + timestamp + ".zip";
        Path rutaZipFinal = dirBackups.resolve(nombreArchivoZip);

        File tempSqlFile = File.createTempFile("archivum_dump_", ".sql");

        try {
            // SINTAXIS ESTÁNDAR: Volcado de datos puro y duro.
            // No necesitamos DROP aquí porque BackupDialog ya ejecuta "DROP ALL OBJECTS" al restaurar.
            jdbcTemplate.execute("SCRIPT TO '" + tempSqlFile.getAbsolutePath().replace("\\", "/") + "'");

            try (FileOutputStream fos = new FileOutputStream(rutaZipFinal.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                try (FileInputStream fis = new FileInputStream(tempSqlFile)) {
                    zos.putNextEntry(new ZipEntry("backup.sql"));
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }

                File origenDocumentos = new File(rutaArchivos);
                if (origenDocumentos.exists() && origenDocumentos.isDirectory()) {
                    comprimirDirectorio(origenDocumentos, "archivos/", zos);
                }
            }
        } finally {
            Files.deleteIfExists(tempSqlFile.toPath());
        }

        return rutaZipFinal.toFile();
    }

    private void comprimirDirectorio(File directorio, String rutaBase, ZipOutputStream zos) throws IOException {
        File[] listado = directorio.listFiles();
        if (listado == null) return;

        for (File elemento : listado) {
            if (elemento.isDirectory()) {
                comprimirDirectorio(elemento, rutaBase + elemento.getName() + "/", zos);
            } else {
                try (FileInputStream fis = new FileInputStream(elemento)) {
                    zos.putNextEntry(new ZipEntry(rutaBase + elemento.getName()));
                    byte[] buffer = new byte[8192];
                    int bytesLeidos;
                    while ((bytesLeidos = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesLeidos);
                    }
                    zos.closeEntry();
                }
            }
        }
    }
}