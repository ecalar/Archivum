package com.archivum.config;

import com.archivum.model.TipoDocumento;
import com.archivum.service.DocumentoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner cargarDatosPrueba(DocumentoService service) {
        return args -> {
            if (service.contarDocumentos() == 0) {
                System.out.println("ARCHIVUM - Cargando datos de prueba...");

                service.crearDocumento(
                        "Acta fundacional de la hermandad",
                        TipoDocumento.ACTA,
                        LocalDate.of(1814, 3, 15),
                        null,
                        "Acta fundacional donde se recoge la primera reunión de hermanos para constituir oficialmente la hermandad.",
                        "Estantería 1, Archivador A, Carpeta 1",
                        "acta_fundacional_1814.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );

                service.crearDocumento(
                        "Carta a Palacio solicitando permiso",
                        TipoDocumento.CARTA,
                        LocalDate.of(1901, 2, 1),
                        null,
                        "Carta enviada a la Casa Real solicitando permiso para procesión.",
                        "Estantería 1, Archivador A, Carpeta 3",
                        "carta_palacio_1901.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );

                service.crearDocumento(
                        "Factura restauración del manto",
                        TipoDocumento.FACTURA,
                        LocalDate.of(1955, 6, 20),
                        null,
                        "Factura del taller de bordados por restauración del manto procesional.",
                        "Estantería 2, Archivador C, Carpeta 1",
                        "factura_manto_1955.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );

                service.crearDocumento(
                        "Plano del paso procesional",
                        TipoDocumento.PLANO,
                        LocalDate.of(1898, 9, 10),
                        null,
                        "Plano detallado del proyecto de construcción del nuevo paso.",
                        "Planero 1, Cajón 3",
                        "plano_paso_1898.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );

                service.crearDocumento(
                        "Fotografía de la procesión 1955",
                        TipoDocumento.FOTOGRAFIA,
                        null,
                        1955,
                        "Fotografía en blanco y negro de la salida procesional.",
                        "Archivo fotográfico, Álbum 2",
                        "foto_procesion_1955.jpg",
                        "documento de prueba".getBytes(),
                        "image/jpeg"
                );

                service.crearDocumento(
                        "Partitura marcha Reina de los Cielos",
                        TipoDocumento.PARTITURA,
                        null,
                        1962,
                        "Marcha procesional dedicada a la titular mariana.",
                        "Archivo musical, Carpeta 5",
                        "marcha_1962.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );

                // Uno en papelera para probar
                var doc = service.crearDocumento(
                        "Antiguo reglamento (derogado)",
                        TipoDocumento.INFORME,
                        LocalDate.of(1950, 1, 1),
                        null,
                        "Reglamento antiguo, conservado por valor histórico.",
                        "Estantería 4, Archivador Z",
                        "reglamento_1950.pdf",
                        "documento de prueba".getBytes(),
                        "application/pdf"
                );
                service.eliminarDocumento(doc.getId());

                System.out.println("ARCHIVUM - " + service.contarDocumentos() + " documentos cargados");
                System.out.println("ARCHIVUM - 1 documento en papelera");
            }
        };
    }
}