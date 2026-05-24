import java.io.*;
import java.util.*;

import core.Graph;
import core.Pair;

/**
 * Common graph loading and pair management utility class.
 *
 * All Comparison classes use this class to:
 *  - Load graph files (SNAP or DIMACS format)
 *  - Read pre-generated pair lists from files
 *
 * Pair files are stored in the graph-data/pairs/ directory.
 * Generated once with GeneratePairs.main(), all experiments run with the same pairs.
 */
public class GraphLoader {

    private static final String PAIRS_DIR = "graph-data/pairs";

    // ─────────────────────────────────────────────────────────────────────────
    //  Graph Loading
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
     * Loads only the graph, does not generate pairs.
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
    //  Pair generation (called only by GeneratePairs)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates numPairs reachable (src, dest) pairs on the graph.
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
    //  Writing/reading pairs to/from file
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Path of the pair file: graph-data/pairs/{graphLabel}.pairs
     */
    public static String pairsFilePath(String graphLabel) {
        return PAIRS_DIR + "/" + graphLabel + ".pairs";
    }

    /**
     * Writes the pair list to a file.
     * Format: each line "src dest"
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
     * Reads the pair list from a file.
     */
    public static List<Pair<Integer, Integer>> loadPairs(String graphLabel) throws IOException {
        String filePath = pairsFilePath(graphLabel);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Pairs file not found: " + filePath
                    + "\n  → Run GeneratePairs.main() first!");
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
     * Checks if the pair file exists.
     */
    public static boolean pairsExist(String graphLabel) {
        return new File(pairsFilePath(graphLabel)).exists();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Check and download missing graph files
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Downloads with DownloadGraphs if there are missing files in the given graph definitions.
     * @return true if all files are ready, false if download fails
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
