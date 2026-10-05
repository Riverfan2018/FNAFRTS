package fnafrts.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import fnafrts.model.graph.MapGraph;
import fnafrts.model.graph.Node;
import fnafrts.model.graph.NodeType;
import fnafrts.model.graph.Sector;

public class MapLoader {

    private static final class PendingLink {
        final String from, to;
        final boolean bidirectional;
        final int line;
        PendingLink(String from, String to, boolean bidirectional, int line) {
            this.from = from; this.to = to;
            this.bidirectional = bidirectional; this.line = line;
        }
    }

    public static MapGraph loadFromResource(String resourcePath) throws IOException {
        try (InputStream is = MapLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Recurso no encontrado: " + resourcePath);
            }
            return parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        }
    }

    public static MapGraph parse(BufferedReader reader) throws IOException {
        MapGraph graph = new MapGraph();
        List<PendingLink> pendingLinks = new ArrayList<>();
        String leftEntry = null, rightEntry = null;

        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            String[] parts = trimmed.split("\\s+");
            String keyword = parts[0].toUpperCase();

            try {
                switch (keyword) {
                    case "SECTOR" -> {
                        requireArgs(parts, 5, lineNumber);
                        String id = parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        String name = stripQuotes(trimmed.substring(trimmed.indexOf('"')));
                        graph.addSector(new Sector(id, name, x, y));
                    }
                    case "NODE" -> {
                        requireArgs(parts, 6, lineNumber);
                        String id = parts[1];
                        String sectorId = parts[2];
                        int x = Integer.parseInt(parts[3]);
                        int y = Integer.parseInt(parts[4]);
                        NodeType type = NodeType.fromToken(parts[5]);
                        graph.addNode(new Node(id, sectorId, x, y, type));
                    }
                    case "LINK" -> {
                        // LINK A B   o   LINK A > B
                        if (parts.length == 3) {
                            pendingLinks.add(new PendingLink(parts[1], parts[2], true, lineNumber));
                        } else if (parts.length == 4 && parts[2].equals(">")) {
                            pendingLinks.add(new PendingLink(parts[1], parts[3], false, lineNumber));
                        } else {
                            throw new IllegalArgumentException(
                                    "LINK mal formado (usa: LINK A B  o  LINK A > B)");
                        }
                    }
                    case "OFFICE" -> {
                        requireArgs(parts, 3, lineNumber);
                        String side = parts[1].toLowerCase();
                        if (side.equals("left")) leftEntry = parts[2];
                        else if (side.equals("right")) rightEntry = parts[2];
                        else throw new IllegalArgumentException("OFFICE lado debe ser 'left' o 'right'");
                    }
                    default -> throw new IllegalArgumentException("Palabra clave desconocida: " + keyword);
                }
            } catch (RuntimeException ex) {
                throw new IOException(
                        "Error en línea " + lineNumber + ": " + ex.getMessage()
                                + "\n  -> " + trimmed, ex);
            }
        }

        // Aplicar links al final, cuando todos los nodos existan.
        for (PendingLink pl : pendingLinks) {
            try {
                graph.addLink(pl.from, pl.to, pl.bidirectional);
            } catch (RuntimeException ex) {
                throw new IOException("Error en LINK (línea " + pl.line + "): " + ex.getMessage(), ex);
            }
        }

        if (leftEntry != null && rightEntry != null) {
            graph.setOfficeEntries(leftEntry, rightEntry);
        }

        graph.validate();
        return graph;
    }

    private static void requireArgs(String[] parts, int expected, int line) {
        if (parts.length < expected) {
            throw new IllegalArgumentException(
                    "Se esperaban al menos " + expected + " tokens, hay " + parts.length);
        }
    }

    private static String stripQuotes(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}