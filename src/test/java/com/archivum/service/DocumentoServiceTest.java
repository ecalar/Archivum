package com.archivum.service;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.repository.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository repository;

    private DocumentoService service;

    private static final String RUTA_TEST = "./target/test-archivos";

    @BeforeEach
    void setUp() {
        service = new DocumentoService(repository, RUTA_TEST);
    }

    // ============================================================
    // CREAR DOCUMENTO - Casos válidos
    // ============================================================

    @Test
    void crearDocumento_valido() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = service.crearDocumento(
                "Acta fundacional",
                TipoDocumento.ACTA,
                LocalDate.of(1814, 3, 15),
                null,
                "Descripcion de prueba",
                "Estanteria 1",
                "acta.pdf",
                "contenido de prueba".getBytes(),
                "application/pdf"
        );

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals("Acta fundacional", doc.getTitulo());
        assertEquals(TipoDocumento.ACTA, doc.getTipo());
        assertEquals(LocalDate.of(1814, 3, 15), doc.getFechaExacta());
        assertNull(doc.getAnioAproximado());
        assertEquals("Descripcion de prueba", doc.getDescripcion());
        assertEquals("Estanteria 1", doc.getUbicacionFisica());
        assertEquals("acta.pdf", doc.getNombreArchivo());
        assertEquals("application/pdf", doc.getTipoMime());
        assertFalse(doc.isEliminado());
        assertNotNull(doc.getFechaSubida());

        verify(repository).save(any(Documento.class));
    }

    @Test
    void crearDocumento_soloConAnioAproximado() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = service.crearDocumento(
                "Documento antiguo",
                TipoDocumento.CARTA,
                null,
                1814,
                null,
                null,
                "carta.pdf",
                "contenido".getBytes(),
                "application/pdf"
        );

        assertNull(doc.getFechaExacta());
        assertEquals(1814, doc.getAnioAproximado());
    }

    @Test
    void crearDocumento_formatoImagen() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = service.crearDocumento(
                "Foto procesion",
                TipoDocumento.FOTOGRAFIA,
                null,
                1955,
                null,
                null,
                "foto.jpg",
                "imagen".getBytes(),
                "image/jpeg"
        );

        assertEquals("image/jpeg", doc.getTipoMime());
    }

    // ============================================================
    // CREAR DOCUMENTO - Validaciones de título
    // ============================================================

    @Test
    void crearDocumento_tituloVacio_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El título es obligatorio", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void crearDocumento_tituloNulo_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        null,
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El título es obligatorio", e.getMessage());
    }

    @Test
    void crearDocumento_tituloSoloEspacios_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "   ",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El título es obligatorio", e.getMessage());
    }

    @Test
    void crearDocumento_tituloMuyCorto_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "AB",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El título debe tener al menos 3 caracteres", e.getMessage());
    }

    @Test
    void crearDocumento_tituloMuyLargo_lanzaExcepcion() {
        String tituloLargo = "A".repeat(201);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        tituloLargo,
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El título debe tener como máximo 200 caracteres", e.getMessage());
    }

    // ============================================================
    // CREAR DOCUMENTO - Validaciones de tipo
    // ============================================================

    @Test
    void crearDocumento_tipoNulo_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El tipo de documento es obligatorio", e.getMessage());
    }

    // ============================================================
    // CREAR DOCUMENTO - Validaciones de fecha
    // ============================================================

    @Test
    void crearDocumento_fechaYAño_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        LocalDate.of(1814, 3, 15),
                        1814,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("Use fecha exacta o año aproximado, no ambos", e.getMessage());
    }

    @Test
    void crearDocumento_fechaFutura_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        LocalDate.now().plusDays(1),
                        null,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("La fecha no puede ser futura", e.getMessage());
    }

    @Test
    void crearDocumento_fechaHoy_esValida() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = service.crearDocumento(
                "Acta reciente",
                TipoDocumento.ACTA,
                LocalDate.now(),
                null,
                null,
                null,
                "acta.pdf",
                "contenido".getBytes(),
                "application/pdf"
        );

        assertEquals(LocalDate.now(), doc.getFechaExacta());
    }

    @Test
    void crearDocumento_anioMuyAntiguo_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        1200,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El año debe estar entre 1500 y 2026", e.getMessage());
    }

    @Test
    void crearDocumento_anioMuyFuturo_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        2050,
                        null,
                        null,
                        "acta.pdf",
                        "contenido".getBytes(),
                        "application/pdf"
                )
        );
        assertEquals("El año debe estar entre 1500 y 2026", e.getMessage());
    }

    // ============================================================
    // CREAR DOCUMENTO - Validaciones de archivo
    // ============================================================

    @Test
    void crearDocumento_archivoVacio_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        new byte[0],
                        "application/pdf"
                )
        );
        assertEquals("El archivo es obligatorio", e.getMessage());
    }

    @Test
    void crearDocumento_archivoNulo_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "acta.pdf",
                        null,
                        "application/pdf"
                )
        );
        assertEquals("El archivo es obligatorio", e.getMessage());
    }

    @Test
    void crearDocumento_formatoExe_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "archivo.exe",
                        "contenido".getBytes(),
                        "application/x-msdownload"
                )
        );
        assertEquals("Formato no permitido. Use PDF, JPG o PNG", e.getMessage());
    }

    @Test
    void crearDocumento_formatoWord_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(
                        "Acta",
                        TipoDocumento.ACTA,
                        null,
                        null,
                        null,
                        null,
                        "documento.doc",
                        "contenido".getBytes(),
                        "application/msword"
                )
        );
        assertEquals("Formato no permitido. Use PDF, JPG o PNG", e.getMessage());
    }

    // ============================================================
    // BUSCAR
    // ============================================================

    @Test
    void buscar_porTexto() {
        Documento doc1 = crearDocumentoPrueba("Acta fundacional", TipoDocumento.ACTA);
        when(repository.buscarPorTexto("acta")).thenReturn(List.of(doc1));

        List<Documento> resultados = service.buscar("acta", null, null, null, null);
        assertEquals(1, resultados.size());
        assertEquals("Acta fundacional", resultados.get(0).getTitulo());
    }

    @Test
    void buscar_porTipo() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findByTipoAndEliminadoFalse(TipoDocumento.ACTA)).thenReturn(List.of(doc));

        List<Documento> resultados = service.buscar(null, TipoDocumento.ACTA, null, null, null);
        assertEquals(1, resultados.size());
        assertEquals(TipoDocumento.ACTA, resultados.get(0).getTipo());
    }

    @Test
    void buscar_porRangoFechas() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        LocalDate desde = LocalDate.of(1800, 1, 1);
        LocalDate hasta = LocalDate.of(1900, 12, 31);
        when(repository.findByFechaExactaBetweenAndEliminadoFalse(desde, hasta))
                .thenReturn(List.of(doc));

        List<Documento> resultados = service.buscar(null, null, desde, hasta, null);
        assertEquals(1, resultados.size());
    }

    @Test
    void buscar_porAnio() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findByAnioAproximadoAndEliminadoFalse(1814)).thenReturn(List.of(doc));

        List<Documento> resultados = service.buscar(null, null, null, null, 1814);
        assertEquals(1, resultados.size());
    }

    @Test
    void buscar_sinFiltros_devuelveTodos() {
        Documento doc1 = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        Documento doc2 = crearDocumentoPrueba("Carta", TipoDocumento.CARTA);
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(doc1, doc2));

        List<Documento> resultados = service.buscar(null, null, null, null, null);
        assertEquals(2, resultados.size());
    }

    @Test
    void buscar_textoVacio_devuelveTodos() {
        Documento doc1 = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(doc1));

        List<Documento> resultados = service.buscar("   ", null, null, null, null);
        assertEquals(1, resultados.size());
    }

    @Test
    void buscar_textoNulo_devuelveTodos() {
        Documento doc1 = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(doc1));

        List<Documento> resultados = service.buscar(null, null, null, null, null);
        assertEquals(1, resultados.size());
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    @Test
    void obtenerPorId_existente() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        Optional<Documento> resultado = service.obtenerPorId(doc.getId());
        assertTrue(resultado.isPresent());
        assertEquals("Acta", resultado.get().getTitulo());
    }

    @Test
    void obtenerPorId_inexistente() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Documento> resultado = service.obtenerPorId(id);
        assertTrue(resultado.isEmpty());
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================

    @Test
    void actualizarDocumento_ok() {
        Documento doc = crearDocumentoPrueba("Titulo original", TipoDocumento.ACTA);
        doc.setDescripcion("Desc original");
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repository.save(any(Documento.class))).thenReturn(doc);

        Documento actualizado = service.actualizarDocumento(
                doc.getId(),
                "Nuevo titulo",
                TipoDocumento.CARTA,
                LocalDate.of(1900, 1, 1),
                null,
                "Nueva descripcion",
                "Nueva ubicacion"
        );

        assertEquals("Nuevo titulo", actualizado.getTitulo());
        assertEquals(TipoDocumento.CARTA, actualizado.getTipo());
        assertEquals(LocalDate.of(1900, 1, 1), actualizado.getFechaExacta());
        assertNull(actualizado.getAnioAproximado());
        assertEquals("Nueva descripcion", actualizado.getDescripcion());
        assertEquals("Nueva ubicacion", actualizado.getUbicacionFisica());
        assertNotNull(actualizado.getFechaModificacion());
        verify(repository).save(any(Documento.class));
    }

    @Test
    void actualizarDocumento_noExiste_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                service.actualizarDocumento(id, "Titulo", TipoDocumento.ACTA,
                        null, null, null, null)
        );
        verify(repository, never()).save(any());
    }

    // ============================================================
    // ELIMINAR LÓGICO
    // ============================================================

    @Test
    void eliminarDocumento_ok() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        service.eliminarDocumento(doc.getId());

        assertTrue(doc.isEliminado());
        assertNotNull(doc.getFechaEliminacion());
        verify(repository).save(doc);
    }

    @Test
    void eliminarDocumento_noEncontrado_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                service.eliminarDocumento(id)
        );
    }

    // ============================================================
    // RESTAURAR
    // ============================================================

    @Test
    void restaurarDocumento_ok() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        doc.setEliminado(true);
        doc.setFechaEliminacion(LocalDateTime.now());
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        service.restaurarDocumento(doc.getId());

        assertFalse(doc.isEliminado());
        assertNull(doc.getFechaEliminacion());
        verify(repository).save(doc);
    }

    @Test
    void restaurarDocumento_noEstaEnPapelera_lanzaExcepcion() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        doc.setEliminado(false);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(IllegalStateException.class, () ->
                service.restaurarDocumento(doc.getId())
        );
    }

    // ============================================================
    // ELIMINAR DEFINITIVO
    // ============================================================

    @Test
    void eliminarDefinitivamente_ok() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        doc.setEliminado(true);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        service.eliminarDefinitivamente(doc.getId());

        verify(repository).delete(doc);
    }

    @Test
    void eliminarDefinitivamente_noEstaEnPapelera_lanzaExcepcion() {
        Documento doc = crearDocumentoPrueba("Acta", TipoDocumento.ACTA);
        doc.setEliminado(false);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(IllegalStateException.class, () ->
                service.eliminarDefinitivamente(doc.getId())
        );
        verify(repository, never()).delete(any());
    }

    // ============================================================
    // CONTADORES
    // ============================================================

    @Test
    void contarDocumentos_devuelveCantidadCorrecta() {
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(
                crearDocumentoPrueba("1", TipoDocumento.ACTA),
                crearDocumentoPrueba("2", TipoDocumento.CARTA),
                crearDocumentoPrueba("3", TipoDocumento.FACTURA)
        ));

        assertEquals(3, service.contarDocumentos());
    }

    @Test
    void contarDocumentos_vacio() {
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of());

        assertEquals(0, service.contarDocumentos());
    }

    @Test
    void contarPapelera_conDocumentos() {
        Documento doc = crearDocumentoPrueba("Eliminado", TipoDocumento.ACTA);
        doc.setEliminado(true);
        when(repository.findAllByEliminadoTrue()).thenReturn(List.of(doc));

        assertEquals(1, service.contarDocumentosEnPapelera());
    }

    @Test
    void contarPapelera_vacia() {
        when(repository.findAllByEliminadoTrue()).thenReturn(List.of());

        assertEquals(0, service.contarDocumentosEnPapelera());
    }

    // ============================================================
    // HELPER
    // ============================================================

    private Documento crearDocumentoPrueba(String titulo, TipoDocumento tipo) {
        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setTitulo(titulo);
        doc.setTipo(tipo);
        doc.setEliminado(false);
        doc.setFechaSubida(LocalDateTime.now());
        return doc;
    }
}