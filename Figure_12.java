import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithms.BasePathFindingAlgorithm;
import algorithms.FindKSPD;
import algorithms.FindKSPD_;
import core.Graph;
import core.Pair;
import core.SimilarityMetric;

/**
 * Figure 12: Vary similarity metric for KSPD.
 *
 * Compares FindKSPD vs FindKSPD- using different similarity functions.
 * Sim1, Sim2, Sim3, Sim4, Sim5
 * k = 10 (fixed), τ = 0.6 (fixed)
 * Graphs: RoadFLA
 *
 * Pairs are read from graph-data/pairs/ directory.
 */
public class Figure_12 {

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
            for (Number n : list) {
                sum += n.doubleValue();
            }
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
            System.out.println("  [Executing] " + algorithmName + " | Source: " + src + " -> Destination: " + dest);

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
                System.err.println("  ❌ OOM: " + oom.getMessage());
                System.gc();
            } catch (Exception ex) {
                System.err.println("  ❌ Exception: " + ex.getMessage());
            }
        }
        return new AlgorithmResult(algorithmName, times, numPaths);
    }

    public static void main(String[] args) {
        System.out.println("=== Figure 12: Vary similarity metric for KSPD ===");
        System.out.println("      FindKSPD vs FindKSPD-");
        System.out.println("      Metrics: Sim1–Sim5 | k=10, τ=0.6\n");

        final SimilarityMetric[] metrics = SimilarityMetric.values();
        int k = 10;
        double threshold = 0.6;

        String[][] graphs = {
                { "RoadFLA",    "graph-data/RoadFLA.gr"     },
        };

        if (!GraphLoader.ensureGraphsExist(graphs)) return;
        for (String[] entry : GeneratePairs.ALL_GRAPHS) {
            if (!GraphLoader.pairsExist(entry[0])) {
                GeneratePairs.main(new String[]{});
                break;
            }
        }

        for (String[] entry : graphs) {
            String label = entry[0], path = entry[1];

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.printf("║  Graph: %-42s ║%n", label);
            System.out.println("╚════════════════════════════════════════════════════╝");

            Graph graph;
            List<Pair<Integer, Integer>> nodePairs;
            try {
                graph = GraphLoader.loadGraph(path);
                nodePairs = GraphLoader.loadPairs(label);
                System.out.printf("Nodes: %d  |  Pairs loaded: %d%n", graph.getNodes().size(), nodePairs.size());
            } catch (IOException e) {
                System.err.println("❌ " + label + ": " + e.getMessage());
                continue;
            }

            List<AlgorithmResult> kspdResults = new ArrayList<>();
            List<AlgorithmResult> kspd_Results = new ArrayList<>();

            for (SimilarityMetric metric : metrics) {
                System.out.println("\n  ── Metric = " + metric + ", k = " + k + ", τ = " + threshold + " ──");

                kspdResults.add(runAlgorithm(new FindKSPD(graph, threshold, metric), "FindKSPD(" + metric + ")", nodePairs, k));

                kspd_Results.add(runAlgorithm(new FindKSPD_(graph, threshold, metric), "FindKSPD-(" + metric + ")", nodePairs, k));

            }

            // ── Final Results Table ──
            // All calculations are completed, now printing the summary table.
            System.out.println("\n\n  ╔══════════════════════════════════════════════════════════════════════╗");
            System.out.printf("  ║  RESULTS: %s — Vary Similarity Metric (k=%d, τ=%.1f)            ║%n", label, k, threshold);
            System.out.println("  ╚══════════════════════════════════════════════════════════════════════╝");
            System.out.println(String.format("  %-8s | %-18s | %-14s | %-12s", "Metric", "Algorithm", "Avg Time(s)", "Avg Paths"));
            System.out.println("  " + "-".repeat(62));

            for (int i = 0; i < metrics.length; i++) {
                System.out.println(String.format("  %-8s | %-18s | %14.4f | %12.2f", metrics[i], "FindKSPD", kspdResults.get(i).avgTime, kspdResults.get(i).avgNumPaths));
                System.out.println(String.format("  %-8s | %-18s | %14.4f | %12.2f", metrics[i], "FindKSPD-", kspd_Results.get(i).avgTime, kspd_Results.get(i).avgNumPaths));
                System.out.println("  " + "-".repeat(62));
            }

            System.out.println("\n  ── Detailed per-metric summary for " + label + " ──");
            for (int i = 0; i < metrics.length; i++) {
                System.out.println("  " + metrics[i] + " FindKSPD  runtime = " + kspdResults.get(i).times);
                System.out.println("  " + metrics[i] + " FindKSPD- runtime = " + kspd_Results.get(i).times);
                System.out.println("  " + metrics[i] + " FindKSPD  paths   = " + kspdResults.get(i).numPaths);
                System.out.println("  " + metrics[i] + " FindKSPD- paths   = " + kspd_Results.get(i).numPaths);
            }
        }
    }
}