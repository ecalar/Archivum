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
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DocumentoService {

    private final DocumentoRepository repository;
    private final String rutaArchivos;

    // Formatos permitidos
    private static final Set<String> FORMATOS_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private static final long TAMANIO_MAXIMO = 50 * 1024 * 1024; // 50 MB en bytes

    public DocumentoService(DocumentoRepository repository,
                            @Value("${app.archivos.ruta}") String rutaArchivos) {
        this.repository = repository;
        this.rutaArchivos = rutaArchivos;
    }

    // ============================================================
    // CREAR
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

        // Validaciones
        validarTitulo(titulo);
        validarTipo(tipo);
        validarFechaYAnio(fechaExacta, anioAproximado);
        validarArchivo(contenidoArchivo, tipoMime);

        // Crear entidad
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

        // Guardar archivo en disco
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivo = carpetaDocumento.resolve(nombreArchivoOriginal);
        Files.write(rutaArchivo, contenidoArchivo);
        doc.setRutaArchivo(rutaArchivo.toString());

        // Guardar en BD
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

        // Si hay texto, búsqueda por texto
        if (texto != null && !texto.isBlank()) {
            return repository.buscarPorTexto(texto.trim());
        }

        // Si hay tipo, búsqueda por tipo
        if (tipo != null) {
            return repository.findByTipoAndEliminadoFalse(tipo);
        }

        // Si hay rango de fechas
        if (desde != null && hasta != null) {
            return repository.findByFechaExactaBetweenAndEliminadoFalse(desde, hasta);
        }

        // Si hay año aproximado
        if (anio != null) {
            return repository.findByAnioAproximadoAndEliminadoFalse(anio);
        }

        // Sin filtros: todos
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

        // Guardar nuevo archivo
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        Files.createDirectories(carpetaDocumento);
        Path rutaArchivo = carpetaDocumento.resolve(nombreArchivo);
        Files.write(rutaArchivo, contenido);

        doc.setNombreArchivo(nombreArchivo);
        doc.setRutaArchivo(rutaArchivo.toString());
        doc.setTipoMime(tipoMime);
        doc.setTamanioBytes((long) contenido.length);
        doc.setFechaModificacion(LocalDateTime.now());

        return repository.save(doc);
    }

    // ============================================================
    // ELIMINAR (LÓGICO)
    // ============================================================

    public void eliminarDocumento(UUID id) {
        Documento doc = obtenerPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));

        doc.setEliminado(true);
        doc.setFechaEliminacion(LocalDateTime.now());
        repository.save(doc);
    }

    public void restaurarDocumento(UUID id) {
        Documento doc = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));

        if (!doc.isEliminado()) {
            throw new IllegalStateException("El documento no está en la papelera");
        }

        doc.setEliminado(false);
        doc.setFechaEliminacion(null);
        repository.save(doc);
    }

    // ============================================================
    // ELIMINAR (DEFINITIVO)
    // ============================================================

    public void eliminarDefinitivamente(UUID id) {
        Documento doc = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado: " + id));

        if (!doc.isEliminado()) {
            throw new IllegalStateException("El documento debe estar en la papelera para eliminarlo definitivamente");
        }

        // Borrar archivo físico
        Path carpetaDocumento = Paths.get(rutaArchivos, doc.getId().toString());
        try {
            if (Files.exists(carpetaDocumento)) {
                Files.walk(carpetaDocumento)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}

        // Borrar de BD
        repository.delete(doc);
    }

    // ============================================================
    // PAPELERA
    // ============================================================

    public List<Documento> obtenerPapelera() {
        return repository.findAllByEliminadoTrue();
    }

    public List<Documento> obtenerDocumentosEnPapeleraMasDe90Dias() {
        LocalDateTime hace90Dias = LocalDateTime.now().minusDays(90);
        return repository.findAllByEliminadoTrue().stream()
                .filter(d -> d.getFechaEliminacion() != null && d.getFechaEliminacion().isBefore(hace90Dias))
                .toList();
    }

    // ============================================================
    // CONTADORES
    // ============================================================

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
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (titulo.trim().length() < 3) {
            throw new IllegalArgumentException("El título debe tener al menos 3 caracteres");
        }
        if (titulo.trim().length() > 200) {
            throw new IllegalArgumentException("El título debe tener como máximo 200 caracteres");
        }
    }

    private void validarTipo(TipoDocumento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
    }

    private void validarFechaYAnio(LocalDate fechaExacta, Integer anioAproximado) {
        if (fechaExacta != null && anioAproximado != null) {
            throw new IllegalArgumentException("Use fecha exacta o año aproximado, no ambos");
        }
        if (fechaExacta != null && fechaExacta.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }
        if (anioAproximado != null && (anioAproximado < 1500 || anioAproximado > 2026)) {
            throw new IllegalArgumentException("El año debe estar entre 1500 y 2026");
        }
    }

    private void validarArchivo(byte[] contenido, String tipoMime) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        if (!FORMATOS_PERMITIDOS.contains(tipoMime != null ? tipoMime.toLowerCase() : "")) {
            throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG");
        }
        if (contenido.length > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("El archivo supera los 50 MB");
        }
    }
}