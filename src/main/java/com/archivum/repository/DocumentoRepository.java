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

    // AHORA BUSCA TAMBIÉN EN EL OCR AUTOMÁTICAMENTE
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