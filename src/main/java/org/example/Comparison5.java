package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.example.algorithms.BasePathFindingAlgorithm;
import org.example.algorithms.FindKSPD;
import org.example.algorithms.FindKSPD_;
import org.example.core.Graph;
import org.example.core.Pair;

/**
 * Figure 10: Vary k on different graphs for KSPD.
 *
 * Compares FindKSPD vs FindKSPD- with varying k values.
 * k ∈ {5, 10, 15, 20}, τ = 0.6 (fixed), Sim1
 *
 * Pair dosyaları graph-data/pairs/ klasöründen okunur.
 */
public class Comparison5 {

    public static class AlgorithmResult {
        public String algorithmName;
        public List<Double> times;
        public List<Integer> numPaths;
        public double avgTime;
        public double avgNumPaths;

        public AlgorithmResult(String name, List<Double> times, List<Integer> numPaths) {
            this.algorithmName = name;
            this.times = times;
            this.numPaths = numPaths;
            this.avgTime = calculateAverage(times);
            this.avgNumPaths = calculateAverage(numPaths);
        }

        private double calculateAverage(List<? extends Number> list) {
            if (list.isEmpty()) return 0;
            double sum = 0;
            for (Number n : list) { sum += n.doubleValue(); }
            return sum / list.size();
        }
    }

    public static AlgorithmResult runAlgorithm(
            BasePathFindingAlgorithm algorithm, String algorithmName,
            List<Pair<Integer, Integer>> nodePairs, int k) {

        List<Double> times = new ArrayList<>();
        List<Integer> numPaths = new ArrayList<>();

        for (Pair<Integer, Integer> pair : nodePairs) {
            int src = pair.getFirst(), dest = pair.getSecond();
            System.out.println("  Running " + algorithmName + " for SRC: " + src + ", DEST: " + dest);

            Runtime rt = Runtime.getRuntime();
            long freeMemory = rt.freeMemory() + (rt.maxMemory() - rt.totalMemory());
            if (freeMemory < rt.maxMemory() / 10) {
                System.err.println("   Low memory, skipping pair " + src + " → " + dest);
                continue;
            }

            try {
                long startTime = System.nanoTime();
                algorithm.findPaths(src, dest, k);
                double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
                times.add(executionTime);

                int pathCount = 0;
                if (algorithm instanceof FindKSPD) pathCount = ((FindKSPD) algorithm).getNumberOfPathsExplored();
                else if (algorithm instanceof FindKSPD_) pathCount = ((FindKSPD_) algorithm).getNumberOfPathsExplored();
                numPaths.add(pathCount);
            } catch (OutOfMemoryError oom) {
                System.err.println("  ❌ OOM: " + oom.getMessage()); System.gc();
            } catch (Exception ex) {
                System.err.println("  ❌ Exception: " + ex.getMessage());
            }
        }
        return new AlgorithmResult(algorithmName, times, numPaths);
    }

    public static void main(String[] args) {
        System.out.println("=== Figure 10: Vary k on different graphs for KSPD ===");
        System.out.println("      FindKSPD vs FindKSPD-");
        System.out.println("      k ∈ {5, 10, 15, 20}, τ=0.6, Sim1\n");

        int[]  kValues   = {5, 10, 15, 20};
        double threshold = 0.6;

        String[][] graphs = {
                { "web-Google", "graph-data/web-Google.txt" },
        };

        if (!GraphLoader.ensureGraphsExist(graphs)) return;
        for (String[] entry : GeneratePairs.ALL_GRAPHS) {
            if (!GraphLoader.pairsExist(entry[0])) {
                GeneratePairs.main(new String[]{}); break;
            }
        }

        for (String[] entry : graphs) {
            String label = entry[0], path = entry[1];

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.printf( "║  Graph: %-42s ║%n", label);
            System.out.println("╚════════════════════════════════════════════════════╝");

            Graph graph;
            List<Pair<Integer, Integer>> nodePairs;
            try {
                graph = GraphLoader.loadGraph(path);
                nodePairs = GraphLoader.loadPairs(label);
                System.out.printf("Nodes: %d  |  Pairs loaded: %d%n", graph.getNodes().size(), nodePairs.size());
            } catch (IOException e) {
                System.err.println("❌ " + label + ": " + e.getMessage()); continue;
            }

            List<AlgorithmResult> kspdResults    = new ArrayList<>();
            List<AlgorithmResult> kspd_Results   = new ArrayList<>();

            for (int k : kValues) {
                System.out.println("\n  ── k = " + k + ", τ = " + threshold + " ──");

                FindKSPD kspd = new FindKSPD(graph, threshold);
                kspdResults.add(runAlgorithm(kspd, "FindKSPD(k=" + k + ")", nodePairs, k));

                FindKSPD_ kspd_ = new FindKSPD_(graph, threshold);
                kspd_Results.add(runAlgorithm(kspd_, "FindKSPD-(k=" + k + ")", nodePairs, k));

            }

            // ── Sonuç tablosu ──
            System.out.println("\n\n  ╔══════════════════════════════════════════════════════════════════════╗");
            System.out.printf( "  ║  RESULTS: %s — Vary k (KSPD, τ=%.1f)                          ║%n", label, threshold);
            System.out.println("  ╚══════════════════════════════════════════════════════════════════════╝");
            System.out.println(String.format("  %-5s | %-18s | %-14s | %-12s", "k", "Algorithm", "Avg Time(s)", "Avg Paths"));
            System.out.println("  " + "-".repeat(58));

            for (int i = 0; i < kValues.length; i++) {
                System.out.println(String.format("  %-5d | %-18s | %14.4f | %12.2f", kValues[i], "FindKSPD",  kspdResults.get(i).avgTime,    kspdResults.get(i).avgNumPaths));
                System.out.println(String.format("  %-5d | %-18s | %14.4f | %12.2f", kValues[i], "FindKSPD-", kspd_Results.get(i).avgTime,   kspd_Results.get(i).avgNumPaths));
                System.out.println("  " + "-".repeat(58));
            }

            System.out.println("\n  ── Per-k summary for " + label + " ──");
            for (int i = 0; i < kValues.length; i++) {
                System.out.println("  k=" + kValues[i] + " FindKSPD  runtime = " + kspdResults.get(i).times);
                System.out.println("  k=" + kValues[i] + " FindKSPD- runtime = " + kspd_Results.get(i).times);
                System.out.println("  k=" + kValues[i] + " FindKSPD  paths   = " + kspdResults.get(i).numPaths);
                System.out.println("  k=" + kValues[i] + " FindKSPD- paths   = " + kspd_Results.get(i).numPaths);
            }
        }
    }
}
