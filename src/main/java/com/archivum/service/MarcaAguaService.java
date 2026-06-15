package com.archivum.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class MarcaAguaService {

    public byte[] aplicarMarcaAguaPDF(byte[] pdfOriginal, String textoMarca, float fontSize) {
        try (PDDocument document = Loader.loadPDF(pdfOriginal)) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (PDPage page : document.getPages()) {
                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(0.15f);
                    cs.setGraphicsStateParameters(gs);

                    cs.setNonStrokingColor(new Color(18, 38, 26));

                    float pageWidth = page.getMediaBox().getWidth();
                    float pageHeight = page.getMediaBox().getHeight();

                    float stringWidth = font.getStringWidth(textoMarca) * fontSize / 1000f;

                    cs.beginText();
                    cs.setFont(font, fontSize);

                    // Centrado y rotación matemática perfecta
                    Matrix matrix = Matrix.getTranslateInstance(pageWidth / 2, pageHeight / 2);
                    matrix.concatenate(Matrix.getRotateInstance(Math.toRadians(45), 0, 0));
                    matrix.concatenate(Matrix.getTranslateInstance(-stringWidth / 2, -fontSize / 4));

                    cs.setTextMatrix(matrix);
                    cs.showText(textoMarca);
                    cs.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            System.err.println("No se pudo aplicar la marca de agua: " + e.getMessage());
            return pdfOriginal;
        }
    }
}