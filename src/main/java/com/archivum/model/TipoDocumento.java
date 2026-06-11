package com.archivum.model;

public enum TipoDocumento {
    ACTA("Acta"),
    CARTA("Carta"),
    FACTURA("Factura"),
    PRESUPUESTO("Presupuesto"),
    PLANO("Plano"),
    PARTITURA("Partitura"),
    FOTOGRAFIA("Fotografía"),
    CONTRATO("Contrato"),
    ESCRITURA("Escritura"),
    BOLETIN("Boletín"),
    CIRCULAR("Circular"),
    INFORME("Informe"),
    OTRO("Otro");

    private final String etiqueta;

    TipoDocumento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}