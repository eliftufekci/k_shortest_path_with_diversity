import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithms.BasePathFindingAlgorithm;
import algorithms.FindKSPD;
import algorithms.FindKSPD_;
import core.Graph;
import core.Pair;

/**
 * Figure 10: Vary k on different graphs for KSPD.
 *
 * Compares FindKSPD vs FindKSPD- with varying k values.
 * k ∈ {5, 10, 15, 20}, τ = 0.6 (fixed), Sim1
 * Graphs: RoadFLA
 *
 * Pairs are read from graph-data/pairs/ directory.
 */
public class Figure_10 {

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
            long memThreshold = rt.maxMemory() / 10;
            if (freeMemory < memThreshold) {
                System.err.println("   Low memory (" + (freeMemory / 1_048_576) + " MB free), skipping pair " 
                        + src + " → " + dest);
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
        System.out.println("=== Figure 10: Vary k on different graphs for KSPD ===");
        System.out.println("      FindKSPD vs FindKSPD-");
        System.out.println("      k ∈ {5, 10, 15, 20}, τ=0.6, Sim1\n");

        int[]  kValues   = {5, 10, 15, 20};
        double threshold = 0.6;

        String[][] graphs = {
                { "RoadFLA",    "graph-data/RoadFLA.gr"     },
        };

        if (!GraphLoader.ensureGraphsExist(graphs)) return;
        for (String[] entry : GeneratePairs.ALL_GRAPHS) {
            if (!GraphLoader.pairsExist(entry[0])) {
                System.out.println("\n  [Setup] Pairs not found. Generating pairs...");
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
                System.out.println("  Loading graph: " + path);
                graph = GraphLoader.loadGraph(path);
                nodePairs = GraphLoader.loadPairs(label);
                System.out.printf("Nodes: %d  |  Pairs loaded: %d%n", graph.getNodes().size(), nodePairs.size());
            } catch (IOException e) {
                System.err.println("❌ " + label + ": " + e.getMessage());
                continue;
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

            // ── Final Results Table ──
            System.out.println("\n\n  ╔══════════════════════════════════════════════════════════════════════╗");
            System.out.printf( "  ║  RESULTS: %s — Vary k (KSPD, τ=%.1f)                          ║%n", label, threshold);
            System.out.println("  ╚══════════════════════════════════════════════════════════════════════╝");
            System.out.printf("  %-5s | %-18s | %-14s | %-12s%n", "k", "Algorithm", "Avg Time(s)", "Avg Paths");
            System.out.println("  " + "-".repeat(59));

            for (int i = 0; i < kValues.length; i++) {
                System.out.printf("  %-5d | %-18s | %14.4f | %12.2f%n", kValues[i], "FindKSPD",  kspdResults.get(i).avgTime,    kspdResults.get(i).avgNumPaths);
                System.out.printf("  %-5d | %-18s | %14.4f | %12.2f%n", kValues[i], "FindKSPD-", kspd_Results.get(i).avgTime,   kspd_Results.get(i).avgNumPaths);
                System.out.println("  " + "-".repeat(59));
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
