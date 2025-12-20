package com.prolab3.parsers;

import com.prolab3.models.Makale;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ManualJsonParser {

    public List<Makale> parse(String filePath) {
        List<Makale> makaleler = new ArrayList<>();
        StringBuilder jsonContent = new StringBuilder();

        // 1. Dosyayı oku
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                jsonContent.append(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return makaleler;
        }

        String content = jsonContent.toString();
        // Dış köşeli parantezleri at
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }

        // 2. Karakter bazlı obje ayırma (Bracket Counting)
        int braceCount = 0;
        int start = 0;
        boolean inQuote = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            // Tırnak kontrolü (escape edilmiş tırnakları \" atla)
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }

            if (!inQuote) {
                if (c == '{') {
                    if (braceCount == 0) start = i;
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // Bir obje bitti
                        String objStr = content.substring(start, i + 1);
                        parseSingleObject(objStr, makaleler);
                    }
                }
            }
        }

        return makaleler;
    }

    private void parseSingleObject(String json, List<Makale> list) {
        try {
            String id = extractValue(json, "\"id\"");
            String title = extractValue(json, "\"title\"");
            String yearStr = extractValue(json, "\"year\"");
            int year = 0;
            if (yearStr != null && !yearStr.equals("null")) {
                try {
                    year = Integer.parseInt(yearStr);
                } catch (Exception e) {}
            }

            List<String> authors = extractList(json, "\"authors\"");
            List<String> refs = extractList(json, "\"referenced_works\"");

            if (id != null) {
                list.add(new Makale(id, title, year, authors, refs));
            }
        } catch (Exception e) {
            System.err.println("Objeyi parse ederken hata: " + e.getMessage());
        }
    }

    // Basit string arama fonksiyonu
    private String extractValue(String source, String key) {
        int keyIndex = source.indexOf(key);
        if (keyIndex == -1) return null;

        int separatorIndex = source.indexOf(":", keyIndex);
        if (separatorIndex == -1) return null;

        // Değerin başladığı yer
        int valueStart = separatorIndex + 1;
        while (valueStart < source.length() && Character.isWhitespace(source.charAt(valueStart))) {
            valueStart++;
        }

        char startChar = source.charAt(valueStart);
        
        // Eğer string ise ("...")
        if (startChar == '"') {
            int valueEnd = source.indexOf("", valueStart + 1);
            // String içinde escape tırnak varsa, basit parser patlayabilir ama 
            // şimdilik en yakın kapanış tırnağını buluyoruz. 
            // Daha sağlam olması için escape kontrolü eklenebilir.
            while (valueEnd != -1 && source.charAt(valueEnd - 1) == '\\') {
                 valueEnd = source.indexOf("", valueEnd + 1);
            }
            
            if (valueEnd != -1) {
                return source.substring(valueStart + 1, valueEnd);
            }
        } 
        // Eğer sayı veya boolean veya null ise
        else {
            int valueEnd = valueStart;
            while (valueEnd < source.length() && (Character.isLetterOrDigit(source.charAt(valueEnd)) || source.charAt(valueEnd) == '.')) {
                valueEnd++;
            }
            return source.substring(valueStart, valueEnd);
        }
        return null;
    }

    // Basit liste ayıklayıcı
    private List<String> extractList(String source, String key) {
        List<String> list = new ArrayList<>();
        int keyIndex = source.indexOf(key);
        if (keyIndex == -1) return list;

        int listStart = source.indexOf("[", keyIndex);
        if (listStart == -1) return list;

        // Listenin bitişini bul (Bracket counting)
        int listEnd = listStart;
        int count = 0;
        for (int i = listStart; i < source.length(); i++) {
            if (source.charAt(i) == '[') count++;
            else if (source.charAt(i) == ']') {
                count--;
                if (count == 0) {
                    listEnd = i;
                    break;
                }
            }
        }

        String listContent = source.substring(listStart + 1, listEnd);
        
        boolean inQ = false;
        StringBuilder currentItem = new StringBuilder();
        
        for (int i = 0; i < listContent.length(); i++) {
            char c = listContent.charAt(i);
            if (c == '"' && (i == 0 || listContent.charAt(i-1) != '\\')) inQ = !inQ;
            
            if (c == ',' && !inQ) {
                addCleanItem(list, currentItem.toString());
                currentItem = new StringBuilder();
            } else {
                currentItem.append(c);
            }
        }
        addCleanItem(list, currentItem.toString());
        
        return list;
    }
    
    private void addCleanItem(List<String> list, String item) {
        String clean = item.trim();
        if (clean.startsWith("\"") && clean.endsWith("\"")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        if (!clean.isEmpty()) {
            list.add(clean);
        }
    }
}