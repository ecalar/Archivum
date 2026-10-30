package com.archivum.repository;

import com.archivum.model.Documento;
import com.archivum.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /**
     * Búsqueda combinada: todos los parámetros son opcionales (null = sin filtro).
     * El texto se busca en título, descripción y contenido OCR.
     */
    @Query("SELECT d FROM Documento d WHERE d.eliminado = false " +
            "AND (:texto IS NULL OR " +
            "     LOWER(d.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "  OR LOWER(COALESCE(d.descripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "  OR LOWER(COALESCE(d.textoOcr, '')) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
            "AND (:tipo IS NULL OR d.tipo = :tipo) " +
            "AND (:desde IS NULL OR d.fechaExacta >= :desde) " +
            "AND (:hasta IS NULL OR d.fechaExacta <= :hasta) " +
            "AND (:anio IS NULL OR d.anioAproximado = :anio) " +
            "ORDER BY d.fechaSubida DESC")
    List<Documento> buscarCombinado(
            @Param("texto") String texto,
            @Param("tipo") TipoDocumento tipo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("anio") Integer anio);

    // --- Métodos antiguos, se mantienen por compatibilidad ---
    @Query("SELECT d FROM Documento d WHERE d.eliminado = false AND " +
            "(LOWER(d.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(COALESCE(d.textoOcr, '')) LIKE LOWER(CONCAT('%', :texto, '%')))")
    List<Documento> buscarPorTexto(@Param("texto") String texto);

    List<Documento> findByTipoAndEliminadoFalse(TipoDocumento tipo);
    List<Documento> findByFechaExactaBetweenAndEliminadoFalse(LocalDate desde, LocalDate hasta);
    List<Documento> findByAnioAproximadoAndEliminadoFalse(Integer anio);
    List<Documento> findAllByEliminadoFalse();
    List<Documento> findAllByEliminadoTrue();
    List<Documento> findTop10ByEliminadoFalseOrderByFechaSubidaDesc();
}