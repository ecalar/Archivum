package com.archivum.service;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class OcrService {

    private final Tesseract tesseract;
    private final boolean ocrEnabled;

    // Carpeta donde el programa guardará el idioma en el ordenador del usuario
    private static final String TESSDATA_LOCAL_DIR = "./data/tessdata";
    private static final String IDIOMA_ARCHIVO = "spa.traineddata";

    public OcrService(@Value("${app.ocr.enabled:true}") boolean ocrEnabled,
                      @Value("${app.ocr.idioma:spa}") String idioma) {
        this.ocrEnabled = ocrEnabled;
        this.tesseract = new Tesseract();

        if (ocrEnabled) {
            prepararDirectorioTessdata();

            // Le decimos a Tesseract que busque en la carpeta local que acabamos de preparar
            tesseract.setDatapath(new File(TESSDATA_LOCAL_DIR).getAbsolutePath());
            tesseract.setLanguage(idioma);
            tesseract.setTessVariable("tessedit_create_hocr", "0");
        }
    }

    /**
     * Extrae el archivo spa.traineddata desde dentro del .jar al disco duro local.
     * Así funciona en Windows, Mac y Linux sin que el usuario instale nada.
     */
    private void prepararDirectorioTessdata() {
        try {
            Path directorioPath = Paths.get(TESSDATA_LOCAL_DIR);
            if (!Files.exists(directorioPath)) {
                Files.createDirectories(directorioPath);
            }

            Path archivoDestino = directorioPath.resolve(IDIOMA_ARCHIVO);

            // Si el archivo no existe en el disco duro, lo copiamos desde resources
            if (!Files.exists(archivoDestino)) {
                System.out.println("Extrayendo archivos de idioma OCR para el primer uso...");

                // Lee el archivo empaquetado en src/main/resources/tessdata/spa.traineddata
                try (InputStream in = getClass().getResourceAsStream("/tessdata/" + IDIOMA_ARCHIVO)) {
                    if (in == null) {
                        System.err.println("CRÍTICO: No se encontró " + IDIOMA_ARCHIVO + " en los recursos del programa.");
                        return;
                    }
                    Files.copy(in, archivoDestino, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al preparar los archivos de OCR: " + e.getMessage());
        }
    }

    public String extraerTexto(File archivo) {
        if (!ocrEnabled || archivo == null || !archivo.exists()) {
            return null;
        }
        try {
            return tesseract.doOCR(archivo);
        } catch (Exception | Error e) {
            System.err.println("Advertencia OCR: No se pudo extraer el texto. " + e.getMessage());
            return null;
        }
    }

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }
}