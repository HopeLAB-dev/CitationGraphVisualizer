package com.hopelab.graph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GraphLoader {

    private GraphLoader() {
    }

    //json seçme metordu
    public static CitationGraph loadFromFile(File file) throws IOException {
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            return loadFromReader(reader);
        }
    }

    //jsondan okuma metodu
    private static CitationGraph loadFromReader(Reader reader) throws IOException {
        String json = readAll(reader);

        //{}leri ayraç olarak ayır
        List<String> objectStrings = splitTopLevelObjects(json);

        //tüm makaleleri ayır
        List<ParsedArticle> parsed = new ArrayList<>();
        for (String obj : objectStrings) {
            ParsedArticle pa = parseOneArticle(obj);
            if (pa != null && pa.idShort != null) {
                parsed.add(pa);
            }
        }

        //Article ve kenarları CitationGraph'a çevir
        CitationGraph graph = new CitationGraph();
        Map<String, Article> idMap = new HashMap<>();

        //düğüm oluştur
        for (ParsedArticle pa : parsed) {
            Article a = graph.ensureArticle(pa.idShort);

            if (pa.title != null) {
                a.setTitle(pa.title);
            }
            if (pa.year != null) {
                a.setYear(pa.year);
            }
            if (pa.authors != null && !pa.authors.isEmpty()) {
                a.setAuthors(pa.authors);
            }

            idMap.put(pa.idShort, a);
        }


        //kenar ekle
        for (ParsedArticle pa : parsed) {
            Article from = idMap.get(pa.idShort);
            if (from == null) continue;

            for (String refShort : pa.referencedShortIds) {
                Article to = idMap.get(refShort);
                if (to == null) continue;
                graph.addEdge(from, to); // from -> to
            }
        }

        return graph;
    }

    //OKUMA İÇİN YARDIMCI FONKSIYONLAR

    //tüm metni oku
    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    //ayırma fonksiyonu {}
    private static List<String> splitTopLevelObjects(String json) {
        List<String> result = new ArrayList<>();

        if (json == null) return result;

        int n = json.length();
        int i = 0;

        // baştaki whitespaceleri at
        while (i < n && Character.isWhitespace(json.charAt(i))) i++;
        //[ karakterine kadar git
        while (i < n && json.charAt(i) != '[') i++;
        if (i >= n) return result;
        i++; //[ sonrası

        int braceDepth = 0;
        boolean inString = false;
        int objStart = -1;

        for (; i < n; i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) continue;

            if (c == '{') {
                if (braceDepth == 0) {
                    objStart = i;
                }
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0 && objStart != -1) {
                    result.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && braceDepth == 0) {
                break;
            }
        }

        return result;
    }

    //{} ile ayrılmış kısımları da ayır yıl title id vs oalark
    private static ParsedArticle parseOneArticle(String obj) {
        if (obj == null || obj.isEmpty()) return null;

        String fullId = extractStringField(obj, "\"id\"");
        String idShort = extractIdShort(fullId);

        String title = extractStringField(obj, "\"title\"");
        Integer year = extractIntField(obj, "\"year\"");

        List<String> authors = extractStringArrayField(obj, "\"authors\"");

        List<String> referenced = extractStringArrayField(obj, "\"referenced_works\"");
        List<String> refShorts = new ArrayList<>();
        for (String r : referenced) {
            String rs = extractIdShort(r);
            if (rs != null) {
                refShorts.add(rs);
            }
        }

        ParsedArticle pa = new ParsedArticle();
        pa.idShort = idShort;
        pa.title = title;
        pa.year = year;
        pa.authors = authors;        // YENİ
        pa.referencedShortIds = refShorts;
        return pa;
    }


    // alan adı
    private static String extractStringField(String obj, String fieldName) {
        int idx = obj.indexOf(fieldName);
        if (idx == -1) return null;

        idx = obj.indexOf(':', idx);
        if (idx == -1) return null;
        idx++;

        // whitespace atla
        int n = obj.length();
        while (idx < n && Character.isWhitespace(obj.charAt(idx))) idx++;
        if (idx >= n || obj.charAt(idx) != '"') return null;
        idx++;

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        while (idx < n) {
            char c = obj.charAt(idx);
            if (escape) {


                sb.append(c);
                escape = false;
            } else {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            idx++;
        }
        return sb.toString();
    }

    // alan adı
    private static Integer extractIntField(String obj, String fieldName) {
        int idx = obj.indexOf(fieldName);
        if (idx == -1) return null;

        idx = obj.indexOf(':', idx);
        if (idx == -1) return null;
        idx++;

        int n = obj.length();
        while (idx < n && Character.isWhitespace(obj.charAt(idx))) idx++;
        if (idx >= n) return null;

        int start = idx;
        while (idx < n && (obj.charAt(idx) == '-' || Character.isDigit(obj.charAt(idx)))) {
            idx++;
        }
        if (start == idx) return null;

        String numStr = obj.substring(start, idx);
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // string array cıkarma
    private static List<String> extractStringArrayField(String obj, String fieldName) {
        List<String> result = new ArrayList<>();

        int idx = obj.indexOf(fieldName);
        if (idx == -1) return result;

        idx = obj.indexOf(':', idx);
        if (idx == -1) return result;
        idx++;

        int n = obj.length();
        while (idx < n && Character.isWhitespace(obj.charAt(idx))) idx++;
        if (idx >= n || obj.charAt(idx) != '[') return result;
        idx++; // [ sonrası

        boolean inString = false;
        boolean escape = false;
        StringBuilder current = new StringBuilder();

        while (idx < n) {
            char c = obj.charAt(idx);

            if (inString) {
                if (escape) {
                    current.append(c);
                    escape = false;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        inString = false;
                        result.add(current.toString());
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == ']') {
                    break;
                }
            }
            idx++;
        }

        return result;
    }

    //id linkinde slashtan sonraki kısmı cıkar
    private static String extractIdShort(String full) {
        if (full == null) return null;
        int slash = full.lastIndexOf('/');
        if (slash >= 0 && slash < full.length() - 1) {
            return full.substring(slash + 1);
        }
        return full;
    }

    // veri icin sınıf
    private static class ParsedArticle {
        String idShort;
        String title;
        Integer year;

        List<String> authors = new ArrayList<>();

        List<String> referencedShortIds = new ArrayList<>();
    }

}
