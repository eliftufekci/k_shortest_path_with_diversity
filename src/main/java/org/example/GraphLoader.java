package org.example;

import java.io.*;
import java.util.*;

import org.example.core.Graph;
import org.example.core.Pair;

/**
 * Ortak graf yükleme ve pair yönetimi utility sınıfı.
 *
 * Tüm Comparison sınıfları bu sınıfı kullanarak:
 *  - Graf dosyasını yükler (SNAP veya DIMACS formatı)
 *  - Önceden üretilmiş pair listesini dosyadan okur
 *
 * Pair dosyaları graph-data/pairs/ klasöründe tutulur.
 * GeneratePairs.main() ile bir kez üretilir, tüm deneyler aynı çiftlerle çalışır.
 */
public class GraphLoader {

    private static final String PAIRS_DIR = "graph-data/pairs";

    // ─────────────────────────────────────────────────────────────────────────
    //  Graf yükleme
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Detects whether the file uses DIMACS Challenge 9 format (.gr).
     */
    public static boolean isDimacsFormat(String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            int checked = 0;
            while ((line = br.readLine()) != null && checked < 20) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("c ") || line.startsWith("p ") || line.startsWith("a ")) {
                    return true;
                }
                checked++;
            }
        } catch (IOException ignored) { }
        return false;
    }

    /**
     * Sadece grafı yükler, pair üretmez.
     */
    public static Graph loadGraph(String filepath) throws IOException {
        Graph graph = new Graph();
        boolean dimacs = isDimacsFormat(filepath);

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (dimacs) {
                    if (!line.startsWith("a ")) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length < 4) continue;
                    int u = Integer.parseInt(parts[1]);
                    int v = Integer.parseInt(parts[2]);
                    double weight = Double.parseDouble(parts[3]);
                    graph.addWeightedEdge(u, v, weight);
                } else {
                    if (line.startsWith("#")) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length == 3) {
                        int u = Integer.parseInt(parts[0]);
                        int v = Integer.parseInt(parts[1]);
                        double weight = Double.parseDouble(parts[2]);
                        graph.addWeightedEdge(u, v, weight);
                    } else if (parts.length == 2) {
                        int u = Integer.parseInt(parts[0]);
                        int v = Integer.parseInt(parts[1]);
                        graph.addWeightedEdge(u, v, 1.0);
                    }
                }
            }
        }
        return graph;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Pair üretme (sadece GeneratePairs tarafından çağrılır)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Graf üzerinde numPairs adet erişilebilir (src, dest) çifti üretir.
     */
    public static List<Pair<Integer, Integer>> generatePairs(Graph graph, int numPairs) {
        List<Integer> nodes = new ArrayList<>(graph.getNodes());
        List<Pair<Integer, Integer>> nodePairs = new ArrayList<>();
        Random random = new Random(42); // fixed seed for reproducibility

        int maxAttempts = numPairs * 100;
        int attempts = 0;
        while (nodePairs.size() < numPairs && attempts < maxAttempts) {
            attempts++;
            int src = nodes.get(random.nextInt(nodes.size()));
            Set<Integer> reachable = findReachableNodes(graph, src);
            if (!reachable.isEmpty()) {
                int dest = (Integer) reachable.toArray()[random.nextInt(reachable.size())];
                nodePairs.add(new Pair<>(src, dest));
            }
        }
        return nodePairs;
    }

    private static Set<Integer> findReachableNodes(Graph graph, int src) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(src);
        visited.add(src);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int neighbor : graph.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        visited.remove(src);
        return visited;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Pair dosyaya yazma / dosyadan okuma
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pair dosyasının yolu: graph-data/pairs/{graphLabel}.pairs
     */
    public static String pairsFilePath(String graphLabel) {
        return PAIRS_DIR + "/" + graphLabel + ".pairs";
    }

    /**
     * Pair listesini dosyaya yazar.
     * Format: her satır "src dest"
     */
    public static void savePairs(String graphLabel, List<Pair<Integer, Integer>> pairs) throws IOException {
        new File(PAIRS_DIR).mkdirs();
        String filePath = pairsFilePath(graphLabel);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("# Pairs for graph: " + graphLabel);
            writer.println("# Generated with seed=42, " + pairs.size() + " pairs");
            for (Pair<Integer, Integer> pair : pairs) {
                writer.println(pair.getFirst() + " " + pair.getSecond());
            }
        }
        System.out.println("  Saved " + pairs.size() + " pairs to " + filePath);
    }

    /**
     * Pair listesini dosyadan okur.
     */
    public static List<Pair<Integer, Integer>> loadPairs(String graphLabel) throws IOException {
        String filePath = pairsFilePath(graphLabel);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Pairs file not found: " + filePath
                    + "\n  → Önce GeneratePairs.main() çalıştırın!");
        }

        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    int src = Integer.parseInt(parts[0]);
                    int dest = Integer.parseInt(parts[1]);
                    pairs.add(new Pair<>(src, dest));
                }
            }
        }
        System.out.println("  Loaded " + pairs.size() + " pairs from " + filePath);
        return pairs;
    }

    /**
     * Pair dosyası mevcut mu kontrol eder.
     */
    public static boolean pairsExist(String graphLabel) {
        return new File(pairsFilePath(graphLabel)).exists();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Eksik graf dosyalarını kontrol et ve indir
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verilen graf tanımlarında eksik dosya varsa DownloadGraphs ile indirir.
     * @return true: tüm dosyalar hazır, false: indirme başarısız
     */
    public static boolean ensureGraphsExist(String[][] graphs) {
        boolean anyMissing = false;
        for (String[] entry : graphs) {
            if (!new File(entry[1]).exists()) {
                anyMissing = true;
                System.out.println("⚠  Missing: " + entry[1]);
            }
        }
        if (anyMissing) {
            System.out.println("\nSome graph files are missing. Starting download...\n");
            try {
                DownloadGraphs.downloadAndPrepareGraphs();
            } catch (IOException e) {
                System.err.println("❌ Download failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
