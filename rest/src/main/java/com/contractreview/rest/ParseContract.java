package com.contractreview.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.ejb.Stateless;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.DetokenizationDictionary;
import opennlp.tools.tokenize.DetokenizationDictionary.Operation;
import opennlp.tools.tokenize.Detokenizer;
import opennlp.tools.tokenize.DictionaryDetokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

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
            return normalizar(removerLinhasSimilares(result.toString()).trim());
        } else {
            return result.toString().trim();
        }
    }

    public String normalizar(String texto) {
        StringBuilder result = new StringBuilder();
        try (InputStream modelIn = new FileInputStream("/usr/local/tomee/webapps/rest/WEB-INF/classes/pt-sent.bin")) {
            SentenceModel model = new SentenceModel(modelIn);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            String sentences[] = sentenceDetector.sentDetect(texto);
            for (String sentence : sentences) {
                String line = tokenizar(sentence);
                if (line != null)
                    result.append(line).append("\n");
            }
        } catch(Throwable error) {
            throw new RuntimeException(error);
        }
        return result.toString();
    }

    public String tokenizar(String texto) {
        try (InputStream modelIn = new FileInputStream("/usr/local/tomee/webapps/rest/WEB-INF/classes/pt-token.bin")) {
            TokenizerModel model = new TokenizerModel(modelIn);
            Tokenizer tokenizer = new TokenizerME(model);
            String tokens[] = tokenizer.tokenize(texto);
            if (tokens.length > 5)
                return destokenizar(tokens);
            else
                return null;
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

    public String destokenizar(String[] texto) {
        String[] tokens = new String[]{".", "!", "(", ")", "\"", "-", ","};
        Operation[] operations = new Operation[]{
            Operation.MOVE_LEFT,
            Operation.MOVE_LEFT,
            Operation.MOVE_RIGHT,
            Operation.MOVE_LEFT,
            Operation.RIGHT_LEFT_MATCHING,
            Operation.MOVE_BOTH,
            Operation.MOVE_LEFT};
        DetokenizationDictionary dict = new DetokenizationDictionary(tokens, operations);
        Detokenizer detokenizer = new DictionaryDetokenizer(dict);
        return detokenizer.detokenize(texto, null);
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

    public String toJSONL(String texto) {
        List<String> lines = Arrays.asList(texto.split("\n"));
        StringBuilder jsonl = new StringBuilder();
        for (String line : lines) {
            JsonObject json = new JsonObject();
            json.addProperty("text", line);
            json.addProperty("category", "contract");
            jsonl.append(new Gson().toJson(json)).append("\n");
        }
        return jsonl.toString();
    }

    public static void main(String[] args) throws Throwable {
        byte[] content = Files.readAllBytes(Paths.get("/home/rene/dev/workspace-contract-review/contract-review/saida.txt"));
        ParseContract c = new ParseContract();
        String texto = c.normalizar(new String(content));
        System.out.println(texto);
    }

}
