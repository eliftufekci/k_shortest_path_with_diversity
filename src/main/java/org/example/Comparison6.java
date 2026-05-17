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
 * Figure 11: Vary τ (similarity threshold) on different graphs for KSPD.
 *
 * Compares FindKSPD vs FindKSPD- with varying τ values.
 * τ ∈ {0.8, 0.6, 0.4, 0.2}, k = 10 (fixed), Sim1
 *
 * Pair dosyaları graph-data/pairs/ klasöründen okunur.
 */
public class Comparison6 {

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
        System.out.println("=== Figure 11: Vary τ (threshold) on different graphs for KSPD ===");
        System.out.println("      FindKSPD vs FindKSPD-");
        System.out.println("      τ ∈ {0.8, 0.6, 0.4, 0.2}, k=10, Sim1\n");

        double[] tauValues = {0.8, 0.6, 0.4, 0.2};
        int      k         = 10;

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

            // Her bir τ değeri için döngü içinde hesaplama yapar ve hemen çıktı verir
            for (double tau : tauValues) {
                System.out.println("\n  ── τ = " + tau + ", k = " + k + " ──");

                FindKSPD kspd = new FindKSPD(graph, tau);
                AlgorithmResult kspdResult = runAlgorithm(kspd, "FindKSPD(τ=" + tau + ")", nodePairs, k);

                FindKSPD_ kspd_ = new FindKSPD_(graph, tau);
                AlgorithmResult kspd_Result = runAlgorithm(kspd_, "FindKSPD-(τ=" + tau + ")", nodePairs, k);

                // ── Bu τ değerine ait anlık sonuç tablosu ──
                System.out.println("\n  ╔══════════════════════════════════════════════════════════════════════╗");
                System.out.printf( "  ║  INTERMEDIATE RESULTS: %s for τ = %.1f                              ║%n", label, tau);
                System.out.println("  ╚══════════════════════════════════════════════════════════════════════╝");
                System.out.println(String.format("  %-6s | %-18s | %-14s | %-12s", "τ", "Algorithm", "Avg Time(s)", "Avg Paths"));
                System.out.println("  " + "-".repeat(60));

                System.out.println(String.format("  %-6.1f | %-18s | %14.4f | %12.2f", tau, "FindKSPD",  kspdResult.avgTime,    kspdResult.avgNumPaths));
                System.out.println(String.format("  %-6.1f | %-18s | %14.4f | %12.2f", tau, "FindKSPD-", kspd_Result.avgTime,   kspd_Result.avgNumPaths));
                System.out.println("  " + "-".repeat(60));

                // ── Bu τ değerine ait detaylı özet listeler ──
                System.out.println("\n  ── Summary for τ=" + tau + " (" + label + ") ──");
                System.out.println("  τ=" + tau + " FindKSPD  runtime = " + kspdResult.times);
                System.out.println("  τ=" + tau + " FindKSPD- runtime = " + kspd_Result.times);
                System.out.println("  τ=" + tau + " FindKSPD  paths   = " + kspdResult.numPaths);
                System.out.println("  τ=" + tau + " FindKSPD- paths   = " + kspd_Result.numPaths);
                System.out.println("\n" + "=".repeat(72));
            }
        }
    }
}