package com.archivum.service;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DocumentoService {

    private final DocumentoRepository repository;
    private final String rutaArchivos;
    private final OcrService ocrService;

    private final AjustesService ajustesService;

    // Formatos permitidos
    private static final Set<String> FORMATOS_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private static final long TAMANIO_MAXIMO = 50 * 1024 * 1024; // 50 MB en bytes

    // Constructor corregido: solo con las dependencias estrictamente necesarias
    public DocumentoService(DocumentoRepository repository,
                            @Value("${app.archivos.ruta}") String rutaArchivos,
                            OcrService ocrService, AjustesService ajustesService) {
        this.repository = repository;
        this.rutaArchivos = rutaArchivos;
        this.ocrService = ocrService;
        this.ajustesService = ajustesService;
    }

    // ============================================================
    // CREAR (Guarda el archivo maestro original e inalterado)
    // ============================================================

    public Documento crearDocumento(String titulo,
                                    TipoDocumento tipo,
                                    LocalDate fechaExacta,
                                    Integer anioAproximado,
                                    String descripcion,
                                    String ubicacionFisica,
                                    String nombreArchivoOriginal,
                                    byte[] contenidoArchivo,
                                    String tipoMime) throws IOException {

        // Validaciones previas
        validarTitulo(titulo);
        validarTipo(tipo);
        validarFechaYAnio(fechaExacta, anioAproximado);
        validarArchivo(contenidoArchivo, tipoMime);

        // Crear objeto de entidad
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

        // Guardar el archivo físico original e intacto en el disco duro
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivoPath = carpetaDocumento.resolve(nombreArchivoOriginal);
        Files.write(rutaArchivoPath, contenidoArchivo);
        doc.setRutaArchivo(rutaArchivoPath.toString());

        // Procesar OCR solo si está activo globalmente en la configuración del sistema
        if (ocrService.isOcrEnabled() && ajustesService.isOcrActivo()) {
            try {
                File archivoParaOcr = rutaArchivoPath.toFile();
                String textoOcr = ocrService.extraerTexto(archivoParaOcr);
                if (textoOcr != null && !textoOcr.isBlank()) {
                    doc.setTextoOcr(textoOcr.trim());
                }
            } catch (Exception ignored) { }
        }

        // Guardar definitivamente en la base de datos
        return repository.save(doc);
    }

    // ============================================================
    // LEER
    // ============================================================

    public Optional<Documento> obtenerPorId(UUID id) {
        return repository.findById(id);
    }

    public List<Documento> obtenerTodos() {
        return repository.findAllByEliminadoFalse();
    }

    public List<Documento> obtenerTodosOrdenadosPorFecha() {
        return repository.findTop10ByEliminadoFalseOrderByFechaSubidaDesc();
    }

    // ============================================================
    // BUSCAR
    // ============================================================

    public List<Documento> buscar(String texto, TipoDocumento tipo,
                                  LocalDate desde, LocalDate hasta,
                                  Integer anio) {

        if (texto != null && !texto.isBlank()) {
            return repository.buscarPorTexto(texto.trim());
        }

        if (tipo != null) {
            return repository.findByTipoAndEliminadoFalse(tipo);
        }

        if (desde != null && hasta != null) {
            return repository.findByFechaExactaBetweenAndEliminadoFalse(desde, hasta);
        }

        if (anio != null) {
            return repository.findByAnioAproximadoAndEliminadoFalse(anio);
        }

        return repository.findAllByEliminadoFalse();
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================

    public Documento actualizarDocumento(UUID id,
                                         String titulo,
                                         TipoDocumento tipo,
                                         LocalDate fechaExacta,
                                         Integer anioAproximado,
                                         String descripcion,
                                         String ubicacionFisica) {

        Documento doc = obtenerPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));

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

    public Documento reemplazarArchivo(UUID id,
                                       String nombreArchivo,
                                       byte[] contenido,
                                       String tipoMime) throws IOException {

        Documento doc = obtenerPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));

        validarArchivo(contenido, tipoMime);

        // Guardar nuevo archivo original en disco
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivoPath = carpetaDocumento.resolve(nombreArchivo);
        Files.write(rutaArchivoPath, contenido);

        doc.setNombreArchivo(nombreArchivo);
        doc.setRutaArchivo(rutaArchivoPath.toString());
        doc.setTipoMime(tipoMime);
        doc.setTamanioBytes((long) contenido.length);
        doc.setFechaModificacion(LocalDateTime.now());

        // Re-ejecutar OCR sobre el nuevo documento
        // Procesar OCR solo si está activo globalmente en la configuración del sistema
        if (ocrService.isOcrEnabled() && ajustesService.isOcrActivo()) {
            try {
                File archivoParaOcr = rutaArchivoPath.toFile();
                String textoOcr = ocrService.extraerTexto(archivoParaOcr);
                if (textoOcr != null && !textoOcr.isBlank()) {
                    doc.setTextoOcr(textoOcr.trim());
                }
            } catch (Exception ignored) { }
        }

        return repository.save(doc);
    }

    // ============================================================
    // ELIMINAR Y PAPELERA
    // ============================================================

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
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        try {
            if (Files.exists(carpetaDocumento)) {
                Files.walk(carpetaDocumento)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}
        repository.delete(doc);
    }

    public List<Documento> obtenerPapelera() {
        return repository.findAllByEliminadoTrue();
    }

    public long contarDocumentos() {
        return repository.findAllByEliminadoFalse().size();
    }

    public long contarDocumentosEnPapelera() {
        return repository.findAllByEliminadoTrue().size();
    }

    // ============================================================
    // VALIDACIONES PRIVADAS
    // ============================================================

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("El título es obligatorio");
        if (titulo.trim().length() < 3) throw new IllegalArgumentException("El título debe tener al menos 3 caracteres");
        if (titulo.trim().length() > 200) throw new IllegalArgumentException("El título debe tener como máximo 200 caracteres");
    }

    private void validarTipo(TipoDocumento tipo) {
        if (tipo == null) throw new IllegalArgumentException("El tipo de documento es obligatorio");
    }

    private void validarFechaYAnio(LocalDate fechaExacta, Integer anioAproximado) {
        if (fechaExacta != null && anioAproximado != null) throw new IllegalArgumentException("Use fecha exacta o año aproximado, no ambos");
        if (fechaExacta != null && fechaExacta.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha no puede ser futura");
        if (anioAproximado != null && (anioAproximado < 1500 || anioAproximado > 2026)) throw new IllegalArgumentException("El año debe estar entre 1500 y 2026");
    }

    private void validarArchivo(byte[] contenido, String tipoMime) {
        if (contenido == null || contenido.length == 0) throw new IllegalArgumentException("El archivo es obligatorio");
        if (!FORMATOS_PERMITIDOS.contains(tipoMime != null ? tipoMime.toLowerCase() : "")) throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG");
        if (contenido.length > TAMANIO_MAXIMO) throw new IllegalArgumentException("El archivo supera los 50 MB");
    }
}