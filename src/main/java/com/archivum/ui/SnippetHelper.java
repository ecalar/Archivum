package com.archivum.ui;

import com.archivum.model.Documento;

public final class SnippetHelper {

    private SnippetHelper() {}

    /**
     * Devuelve un fragmento del texto OCR alrededor de la coincidencia.
     * Si no hay coincidencia o no hay texto OCR, devuelve null.
     */
    public static String snippetOcr(Documento d, String busqueda) {
        if (d == null) return null;
        String ocr = d.getTextoOcr();
        if (ocr == null || ocr.isBlank()) return null;
        if (busqueda == null || busqueda.isBlank()) {
            return recortar(ocr, 0, 180);
        }
        int idx = ocr.toLowerCase().indexOf(busqueda.toLowerCase());
        if (idx < 0) return null; // sin coincidencia en OCR
        int ini = Math.max(0, idx - 60);
        int fin = Math.min(ocr.length(), idx + busqueda.length() + 60);
        return "…" + recortar(ocr.substring(ini, fin), 0, 260) + "…";
    }

    /** true si el documento tiene texto OCR útil. */
    public static boolean tieneOcr(Documento d) {
        return d != null && d.getTextoOcr() != null && !d.getTextoOcr().isBlank();
    }

    private static String recortar(String s, int desde, int max) {
        if (s == null) return "";
        int fin = Math.min(s.length(), desde + max);
        return s.substring(desde, fin).replaceAll("\\s+", " ").trim();
    }
}