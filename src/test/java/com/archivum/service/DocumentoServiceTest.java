package com.archivum.service;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import com.archivum.repository.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)   // <- Permite stubs no usados sin error
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository repository;

    @Mock
    private OcrService ocrService;

    @Mock
    private AjustesService ajustesService;

    private DocumentoService service;
    private static final String RUTA_TEST = "./target/test-archivos";

    @BeforeEach
    void setUp() {
        // Estos stubs pueden no usarse en todos los tests, pero con LENIENT no dará error
        lenient().when(ocrService.isOcrEnabled()).thenReturn(false);
        lenient().when(ajustesService.isOcrActivo()).thenReturn(false);

        service = new DocumentoService(repository, RUTA_TEST, ocrService, ajustesService);
    }

    // ----------------------------------------------------------------
    // CREAR DOCUMENTO - Casos válidos
    // ----------------------------------------------------------------
    @Test
    void crearDocumento_valido() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = service.crearDocumento(
                "Acta fundacional", TipoDocumento.ACTA, LocalDate.of(1814, 3, 15),
                null, "Descripcion de prueba", "Estanteria 1",
                "acta.pdf", "contenido de prueba".getBytes(), "application/pdf");

        assertNotNull(doc);
        assertEquals("Acta fundacional", doc.getTitulo());
        assertEquals(TipoDocumento.ACTA, doc.getTipo());
        assertEquals(LocalDate.of(1814, 3, 15), doc.getFechaExacta());
        assertFalse(doc.isEliminado());
        verify(repository).save(any(Documento.class));
    }

    @Test
    void crearDocumento_soloConAnioAproximado() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        Documento doc = service.crearDocumento(
                "Documento antiguo", TipoDocumento.CARTA, null, 1814,
                null, null, "carta.pdf", "contenido".getBytes(), "application/pdf");
        assertNull(doc.getFechaExacta());
        assertEquals(1814, doc.getAnioAproximado());
    }

    @Test
    void crearDocumento_formatoImagen() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        Documento doc = service.crearDocumento(
                "Foto procesion", TipoDocumento.FOTOGRAFIA, null, 1955,
                null, null, "foto.jpg", "imagen".getBytes(), "image/jpeg");
        assertEquals("image/jpeg", doc.getTipoMime());
    }

    // ----------------------------------------------------------------
    // VALIDACIONES TÍTULO
    // ----------------------------------------------------------------
    @Test
    void crearDocumento_tituloVacio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("", TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
        verify(repository, never()).save(any());
    }

    @Test
    void crearDocumento_tituloNulo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(null, TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    @Test
    void crearDocumento_tituloSoloEspacios_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("   ", TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    @Test
    void crearDocumento_tituloMuyCorto_lanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("AB", TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
        assertEquals("El título debe tener al menos 3 caracteres", e.getMessage());
    }

    @Test
    void crearDocumento_tituloMuyLargo_lanzaExcepcion() {
        String largo = "A".repeat(201);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento(largo, TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
        assertEquals("El título debe tener como máximo 200 caracteres", e.getMessage());
    }

    // ----------------------------------------------------------------
    // VALIDACIONES TIPO
    // ----------------------------------------------------------------
    @Test
    void crearDocumento_tipoNulo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", null, null, null, null, null,
                        "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    // ----------------------------------------------------------------
    // VALIDACIONES FECHA
    // ----------------------------------------------------------------
    @Test
    void crearDocumento_fechaYAño_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA,
                        LocalDate.of(1814, 3, 15), 1814,
                        null, null, "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    @Test
    void crearDocumento_fechaFutura_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA,
                        LocalDate.now().plusDays(1), null,
                        null, null, "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    @Test
    void crearDocumento_fechaHoy_esValida() throws IOException {
        when(repository.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        Documento doc = service.crearDocumento("Acta reciente", TipoDocumento.ACTA,
                LocalDate.now(), null, null, null,
                "acta.pdf", "contenido".getBytes(), "application/pdf");
        assertEquals(LocalDate.now(), doc.getFechaExacta());
    }

    @Test
    void crearDocumento_anioMuyAntiguo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, 1200,
                        null, null, "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    @Test
    void crearDocumento_anioMuyFuturo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, 2050,
                        null, null, "acta.pdf", "contenido".getBytes(), "application/pdf"));
    }

    // ----------------------------------------------------------------
    // VALIDACIONES ARCHIVO
    // ----------------------------------------------------------------
    @Test
    void crearDocumento_archivoVacio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", new byte[0], "application/pdf"));
    }

    @Test
    void crearDocumento_archivoNulo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, null, null, null,
                        "acta.pdf", null, "application/pdf"));
    }

    @Test
    void crearDocumento_formatoExe_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, null, null, null,
                        "archivo.exe", "contenido".getBytes(), "application/x-msdownload"));
    }

    @Test
    void crearDocumento_formatoWord_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearDocumento("Acta", TipoDocumento.ACTA, null, null, null, null,
                        "documento.doc", "contenido".getBytes(), "application/msword"));
    }

    // ----------------------------------------------------------------
    // BUSCAR
    // ----------------------------------------------------------------
    @Test
    void buscar_porTexto() {
        Documento doc = crearDoc("Acta fundacional", TipoDocumento.ACTA);
        when(repository.buscarPorTexto("acta")).thenReturn(List.of(doc));
        assertEquals(1, service.buscar("acta", null, null, null, null).size());
    }

    @Test
    void buscar_porTipo() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        when(repository.findByTipoAndEliminadoFalse(TipoDocumento.ACTA)).thenReturn(List.of(doc));
        assertEquals(1, service.buscar(null, TipoDocumento.ACTA, null, null, null).size());
    }

    @Test
    void buscar_porRangoFechas() {
        LocalDate desde = LocalDate.of(1800, 1, 1), hasta = LocalDate.of(1900, 12, 31);
        when(repository.findByFechaExactaBetweenAndEliminadoFalse(desde, hasta)).thenReturn(List.of(crearDoc("", TipoDocumento.ACTA)));
        assertEquals(1, service.buscar(null, null, desde, hasta, null).size());
    }

    @Test
    void buscar_porAnio() {
        when(repository.findByAnioAproximadoAndEliminadoFalse(1814)).thenReturn(List.of(crearDoc("", TipoDocumento.ACTA)));
        assertEquals(1, service.buscar(null, null, null, null, 1814).size());
    }

    @Test
    void buscar_sinFiltros_devuelveTodos() {
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(crearDoc("A", TipoDocumento.ACTA), crearDoc("B", TipoDocumento.CARTA)));
        assertEquals(2, service.buscar(null, null, null, null, null).size());
    }

    // ----------------------------------------------------------------
    // ACTUALIZAR
    // ----------------------------------------------------------------
    @Test
    void actualizarDocumento_ok() {
        Documento doc = crearDoc("Titulo original", TipoDocumento.ACTA);
        doc.setDescripcion("Desc original");
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repository.save(any())).thenReturn(doc);

        Documento actualizado = service.actualizarDocumento(doc.getId(),
                "Nuevo titulo", TipoDocumento.CARTA, LocalDate.of(1900, 1, 1),
                null, "Nueva descripcion", "Nueva ubicacion");

        assertEquals("Nuevo titulo", actualizado.getTitulo());
        assertEquals(TipoDocumento.CARTA, actualizado.getTipo());
        assertNotNull(actualizado.getFechaModificacion());
    }

    @Test
    void actualizarDocumento_noExiste_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () ->
                service.actualizarDocumento(id, "T", TipoDocumento.ACTA, null, null, null, null));
    }

    // ----------------------------------------------------------------
    // ELIMINAR / RESTAURAR
    // ----------------------------------------------------------------
    @Test
    void eliminarDocumento_ok() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        service.eliminarDocumento(doc.getId());
        assertTrue(doc.isEliminado());
        verify(repository).save(doc);
    }

    @Test
    void restaurarDocumento_ok() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        doc.setEliminado(true);
        doc.setFechaEliminacion(LocalDateTime.now());
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        service.restaurarDocumento(doc.getId());
        assertFalse(doc.isEliminado());
    }

    @Test
    void restaurarDocumento_noEstaEnPapelera_lanzaExcepcion() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        doc.setEliminado(false);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThrows(IllegalStateException.class, () -> service.restaurarDocumento(doc.getId()));
    }

    @Test
    void eliminarDefinitivamente_ok() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        doc.setEliminado(true);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        service.eliminarDefinitivamente(doc.getId());
        verify(repository).delete(doc);
    }

    @Test
    void eliminarDefinitivamente_noEstaEnPapelera_lanzaExcepcion() {
        Documento doc = crearDoc("Acta", TipoDocumento.ACTA);
        doc.setEliminado(false);
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThrows(IllegalStateException.class, () -> service.eliminarDefinitivamente(doc.getId()));
    }

    // ----------------------------------------------------------------
    // CONTADORES
    // ----------------------------------------------------------------
    @Test
    void contarDocumentos_devuelveCantidadCorrecta() {
        when(repository.findAllByEliminadoFalse()).thenReturn(List.of(crearDoc("1", TipoDocumento.ACTA), crearDoc("2", TipoDocumento.CARTA)));
        assertEquals(2, service.contarDocumentos());
    }

    @Test
    void contarPapelera_conDocumentos() {
        Documento doc = crearDoc("Eliminado", TipoDocumento.ACTA);
        doc.setEliminado(true);
        when(repository.findAllByEliminadoTrue()).thenReturn(List.of(doc));
        assertEquals(1, service.contarDocumentosEnPapelera());
    }

    // ----------------------------------------------------------------
    // HELPER
    // ----------------------------------------------------------------
    private Documento crearDoc(String titulo, TipoDocumento tipo) {
        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setTitulo(titulo);
        doc.setTipo(tipo);
        doc.setEliminado(false);
        doc.setFechaSubida(LocalDateTime.now());
        return doc;
    }
}