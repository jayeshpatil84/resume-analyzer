package com.resumeai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts raw text from an uploaded PDF resume using Apache PDFBox 3.x.
 *
 * NOTE: PDFBox 3.x removed PDDocument.load() — use Loader.loadPDF() instead.
 */
@Service
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);

    public String extractText(MultipartFile file) throws IOException {
        // PDFBox 3.x requires Loader.loadPDF(byte[]) — the old PDDocument.load() was removed
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF ({} pages)",
                    text.length(), document.getNumberOfPages());
            return text;
        }
    }
}
