package com.contractreview.rest;

import java.io.IOException;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

import jakarta.ejb.Stateless;

@Stateless
public class ParseContract {

    public String parse(byte[] contract, boolean similaridades) throws IOException {
        StringBuffer result = new StringBuffer();
        try (PDDocument document = PDDocument.load(contract)) {
            AccessPermission ap = document.getCurrentAccessPermission();
            if (!ap.canExtractContent()) {
                throw new IOException("You do not have permission to extract text");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setAddMoreFormatting(true);
            stripper.setSortByPosition(true);
            stripper.setShouldSeparateByBeads(true);
            
            stripper.setDropThreshold(5);
            stripper.setIndentThreshold(5);
            stripper.setSpacingTolerance(5);

            stripper.setSuppressDuplicateOverlappingText(true);

            for (int p = 1; p <= document.getNumberOfPages(); ++p) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String texto = stripper.getText(document).trim();
                result.append(texto).append("\n");
            }
        }
        if (similaridades) {
            return removerLinhasSimilares(result.toString()).trim();
        } else {
            return result.toString().trim();
        }
    }

    public String removerLinhasSimilares(String texto) {
        String[] linhas = texto.split("\n");
        String result = "";
        for (int i = 0; i < linhas.length; i++) {
            boolean similar = false;
            int total = 0;
            if (linhas[i].equals("") || linhas[i].equals("\n") || linhas[i].trim().equals("")) {
                result = result + linhas[i] + "\n";
            } else {
                for (int j = 0; j < linhas.length; j++) {
                    if (i != j) {
                        int distance = LevenshteinDistance.getDefaultInstance().apply(linhas[i], linhas[j]);
                        long pct = 0;
                        if (linhas[i].length() > 0) {
                            pct = (distance * 100) / linhas[i].length();
                        }
                        if (distance < 3 && pct < 15) {
                            similar = true;
                            total = total + 1;
                        }
                    }
                }
                if ((!similar) || (similar && total < 3)) {
                    result = result + linhas[i] + "\n";
                }    
            }
        }
        return result;
    }

}
