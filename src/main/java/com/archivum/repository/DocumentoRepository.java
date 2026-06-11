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

    // Buscar no eliminados por texto en título o descripción
    @Query("SELECT d FROM Documento d WHERE d.eliminado = false AND " +
            "(LOWER(d.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
            "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')))")
    List<Documento> buscarPorTexto(@Param("texto") String texto);

    // Buscar no eliminados por tipo
    List<Documento> findByTipoAndEliminadoFalse(TipoDocumento tipo);

    // Buscar no eliminados por rango de fechas
    List<Documento> findByFechaExactaBetweenAndEliminadoFalse(LocalDate desde, LocalDate hasta);

    // Buscar no eliminados por año aproximado
    List<Documento> findByAnioAproximadoAndEliminadoFalse(Integer anio);

    // Todos los no eliminados
    List<Documento> findAllByEliminadoFalse();

    // Todos los eliminados (papelera)
    List<Documento> findAllByEliminadoTrue();

    // Últimos documentos subidos (no eliminados)
    List<Documento> findTop10ByEliminadoFalseOrderByFechaSubidaDesc();
}