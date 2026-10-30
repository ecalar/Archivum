package com.archivum.service;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DocumentoService {

    private final DocumentoRepository repository;
    private final String rutaArchivos;
    private final OcrService ocrService;
    private final AjustesService ajustesService;

    private static final Set<String> FORMATOS_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png"
    );
    private static final long TAMANIO_MAXIMO = 50 * 1024 * 1024;

    public DocumentoService(DocumentoRepository repository,
                            @Value("${app.archivos.ruta}") String rutaArchivos,
                            OcrService ocrService,
                            AjustesService ajustesService) {
        this.repository = repository;
        this.rutaArchivos = rutaArchivos;
        this.ocrService = ocrService;
        this.ajustesService = ajustesService;
    }

    // ============================================================
    // CREAR
    // ============================================================
    public Documento crearDocumento(String titulo, TipoDocumento tipo, LocalDate fechaExacta,
                                    Integer anioAproximado, String descripcion, String ubicacionFisica,
                                    String nombreArchivoOriginal, byte[] contenidoArchivo, String tipoMime) throws IOException {
        validarTitulo(titulo);
        validarTipo(tipo);
        validarFechaYAnio(fechaExacta, anioAproximado);
        validarArchivo(contenidoArchivo, tipoMime);

        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setTitulo(titulo.trim());
        doc.setTipo(tipo);
        doc.setFechaExacta(fechaExacta);
        doc.setAnioAproximado(anioAproximado);
        doc.setDescripcion(descripcion != null ? descripcion.trim() : null);
        doc.setUbicacionFisica(ubicacionFisica != null ? ubicacionFisica.trim() : null);
        doc.setNombreArchivo(nombreArchivoOriginal);
        doc.setTipoMime(tipoMime);
        doc.setTamanioBytes((long) contenidoArchivo.length);
        doc.setEliminado(false);
        doc.setFechaSubida(LocalDateTime.now());

        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivoPath = carpetaDocumento.resolve(nombreArchivoOriginal);
        Files.write(rutaArchivoPath, contenidoArchivo);
        doc.setRutaArchivo(rutaArchivoPath.toString());

        // 1. Guardamos el documento primero para liberar la interfaz gráfica instantáneamente
        Documento docGuardado = repository.save(doc);

        // 2. Disparamos el OCR en la sombra sin bloquear al usuario
        if (ocrService.isOcrEnabled() && ajustesService.isOcrActivo()) {
            procesarOcrEnSegundoPlano(docGuardado.getId(), rutaArchivoPath);
        }

        return docGuardado;
    }

    // ============================================================
    // ACTUALIZAR Y REEMPLAZAR
    // ============================================================
    public Documento actualizarDocumento(UUID id, String titulo, TipoDocumento tipo,
                                         LocalDate fechaExacta, Integer anioAproximado,
                                         String descripcion, String ubicacionFisica) {
        Documento doc = obtenerPorId(id).orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));
        validarTitulo(titulo);
        validarTipo(tipo);
        validarFechaYAnio(fechaExacta, anioAproximado);

        doc.setTitulo(titulo.trim());
        doc.setTipo(tipo);
        doc.setFechaExacta(fechaExacta);
        doc.setAnioAproximado(anioAproximado);
        doc.setDescripcion(descripcion != null ? descripcion.trim() : null);
        doc.setUbicacionFisica(ubicacionFisica != null ? ubicacionFisica.trim() : null);
        doc.setFechaModificacion(LocalDateTime.now());
        return repository.save(doc);
    }

    public Documento reemplazarArchivo(UUID id, String nombreArchivo, byte[] contenido, String tipoMime) throws IOException {
        Documento doc = obtenerPorId(id).orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));
        validarArchivo(contenido, tipoMime);

        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivoPath = carpetaDocumento.resolve(nombreArchivo);
        Files.write(rutaArchivoPath, contenido);

        doc.setNombreArchivo(nombreArchivo);
        doc.setRutaArchivo(rutaArchivoPath.toString());
        doc.setTipoMime(tipoMime);
        doc.setTamanioBytes((long) contenido.length);
        doc.setFechaModificacion(LocalDateTime.now());

        Documento docActualizado = repository.save(doc);

        if (ocrService.isOcrEnabled() && ajustesService.isOcrActivo()) {
            procesarOcrEnSegundoPlano(docActualizado.getId(), rutaArchivoPath);
        }

        return docActualizado;
    }

    // ============================================================
    // MOTOR ASÍNCRONO DE OCR
    // ============================================================
    private void procesarOcrEnSegundoPlano(UUID documentoId, Path rutaArchivo) {
        CompletableFuture.runAsync(() -> {
            try {
                String textoOcr = ocrService.extraerTexto(rutaArchivo.toFile());
                if (textoOcr != null && !textoOcr.isBlank()) {
                    // Abrimos una mini-transacción nueva solo para inyectar el texto
                    repository.findById(documentoId).ifPresent(d -> {
                        d.setTextoOcr(textoOcr.trim());
                        repository.save(d);
                    });
                }
            } catch (Exception e) {
                System.err.println("Fallo silencioso en OCR de fondo: " + e.getMessage());
            }
        });
    }

    // ============================================================
    // LECTURA, BÚSQUEDA Y PAPELERA (Sin cambios)
    // ============================================================
    public Optional<Documento> obtenerPorId(UUID id) { return repository.findById(id); }
    public List<Documento> obtenerTodos() { return repository.findAllByEliminadoFalse(); }
    public List<Documento> obtenerTodosOrdenadosPorFecha() { return repository.findTop10ByEliminadoFalseOrderByFechaSubidaDesc(); }

    public List<Documento> buscar(String texto, TipoDocumento tipo,
                                  LocalDate desde, LocalDate hasta, Integer anio) {
        String t = (texto != null && !texto.isBlank()) ? texto.trim() : null;
        return repository.buscarCombinado(t, tipo, desde, hasta, anio);
    }

    public void eliminarDocumento(UUID id) {
        Documento doc = obtenerPorId(id).orElseThrow();
        doc.setEliminado(true);
        doc.setFechaEliminacion(LocalDateTime.now());
        repository.save(doc);
    }

    public void restaurarDocumento(UUID id) {
        Documento doc = repository.findById(id).orElseThrow();
        doc.setEliminado(false);
        doc.setFechaEliminacion(null);
        repository.save(doc);
    }

    public void eliminarDefinitivamente(UUID id) {
        Documento doc = repository.findById(id).orElseThrow();
        try { Files.deleteIfExists(Paths.get(doc.getRutaArchivo())); } catch (IOException ignored) {}
        repository.delete(doc);
    }

    public List<Documento> obtenerPapelera() { return repository.findAllByEliminadoTrue(); }
    public long contarDocumentos() { return repository.findAllByEliminadoFalse().size(); }
    public long contarDocumentosEnPapelera() { return repository.findAllByEliminadoTrue().size(); }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("El título es obligatorio");
        if (titulo.trim().length() < 3 || titulo.trim().length() > 200) throw new IllegalArgumentException("El título debe tener entre 3 y 200 caracteres");
    }
    private void validarTipo(TipoDocumento tipo) { if (tipo == null) throw new IllegalArgumentException("El tipo de documento es obligatorio"); }
    private void validarFechaYAnio(LocalDate fechaExacta, Integer anioAproximado) {
        if (fechaExacta != null && anioAproximado != null) throw new IllegalArgumentException("Use fecha exacta o año aproximado, no ambos");
    }
    private void validarArchivo(byte[] contenido, String tipoMime) {
        if (contenido == null || contenido.length == 0) throw new IllegalArgumentException("El archivo es obligatorio");
        if (!FORMATOS_PERMITIDOS.contains(tipoMime != null ? tipoMime.toLowerCase() : "")) throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG");
        if (contenido.length > TAMANIO_MAXIMO) throw new IllegalArgumentException("El archivo supera los 50 MB");
    }

    public int reindexarOcrFaltante(java.util.function.Consumer<Integer> onProgress) {
        var docs = repository.findAllByEliminadoFalse().stream()
                .filter(d -> d.getTextoOcr() == null || d.getTextoOcr().isBlank())
                .filter(d -> d.getRutaArchivo() != null && !d.getRutaArchivo().isBlank())
                .toList();

        int total = docs.size();
        int hechos = 0;
        for (Documento d : docs) {
            try {
                String txt = ocrService.extraerTexto(new java.io.File(d.getRutaArchivo()));
                if (txt != null && !txt.isBlank()) {
                    d.setTextoOcr(txt.trim());
                    repository.save(d);
                }
                hechos++;
                if (onProgress != null) onProgress.accept(hechos);
            } catch (Exception e) {
                System.err.println("Fallo re-indexando " + d.getId() + ": " + e.getMessage());
            }
        }
        return hechos;
    }
}