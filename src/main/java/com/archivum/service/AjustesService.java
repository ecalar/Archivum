package com.archivum.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.Properties;

@Service
public class AjustesService {

    private static final String CONFIG_FILE = "./data/config.properties";
    private Properties properties;

    public AjustesService() {
        properties = new Properties();
        cargarAjustes();
    }

    private void cargarAjustes() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                try (InputStream input = new FileInputStream(file)) {
                    properties.load(input);
                }
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                // Inicialización estructurada por defecto
                properties.setProperty("institucion.nombre", "ARCHIVO DOCUMENTAL");
                properties.setProperty("marca.agua.activa", "true");
                properties.setProperty("marca.agua.tamano", "45");
                properties.setProperty("ocr.activo", "true");
                properties.setProperty("ocr.idioma", "spa");
                properties.setProperty("backup.ruta", "./backups");
                guardarAjustes();
            }
        } catch (IOException e) {
            System.err.println("Error al cargar ajustes: " + e.getMessage());
        }
    }

    public void guardarAjustes() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Configuración Avanzada de Archivum");
        } catch (IOException e) {
            System.err.println("Error al guardar ajustes: " + e.getMessage());
        }
    }

    // Getters y Setters Tipados
    public String getNombreInstitucion() { return properties.getProperty("institucion.nombre", "ARCHIVO DOCUMENTAL"); }
    public void setNombreInstitucion(String v) { properties.setProperty("institucion.nombre", v); }

    public boolean isMarcaAguaActiva() { return Boolean.parseBoolean(properties.getProperty("marca.agua.activa", "true")); }
    public void setMarcaAguaActiva(boolean v) { properties.setProperty("marca.agua.activa", String.valueOf(v)); }

    public float getMarcaAguaTamano() { return Float.parseFloat(properties.getProperty("marca.agua.tamano", "45")); }
    public void setMarcaAguaTamano(float v) { properties.setProperty("marca.agua.tamano", String.valueOf(v)); }

    public boolean isOcrActivo() { return Boolean.parseBoolean(properties.getProperty("ocr.activo", "true")); }
    public void setOcrActivo(boolean v) { properties.setProperty("ocr.activo", String.valueOf(v)); }

    public String getOcrIdioma() { return properties.getProperty("ocr.idioma", "spa"); }
    public void setOcrIdioma(String v) { properties.setProperty("ocr.idioma", v); }

    public String getRutaBackup() { return properties.getProperty("backup.ruta", "./backups"); }
    public void setRutaBackup(String v) { properties.setProperty("backup.ruta", v); }
}