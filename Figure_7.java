import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithms.BasePathFindingAlgorithm;
import algorithms.FindKSPD;
import algorithms.Iterbound;
import core.Graph;
import core.Pair;

/**
 * Figure 7: Vary k on different graphs for KSP.
 *
 * Compares FindKSP (τ=1.0) vs IterBound with varying k values.
 * k ∈ {10, 20, 30, 40, 50}
 * Graphs: RoadFLA
 * Metrics: Average runtime (s), Average number of paths explored
 *
 * Pairs are read from graph-data/pairs/ directory.
 */
public class Figure_7 {

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

    // -------------------------------------------------------------------------
    // Algorithm runner
    // -------------------------------------------------------------------------

    public static AlgorithmResult runAlgorithm(
            BasePathFindingAlgorithm algorithm,
            String algorithmName,
            List<Pair<Integer, Integer>> nodePairs,
            int k) {

        List<Double> times = new ArrayList<>();
        List<Integer> numPaths = new ArrayList<>();

        for (Pair<Integer, Integer> pair : nodePairs) {
            int src = pair.getFirst();
            int dest = pair.getSecond();

            System.out.println("  [Executing] " + algorithmName + " | Source: " + src + " -> Destination: " + dest);

            Runtime rt = Runtime.getRuntime();
            long freeMemory = rt.freeMemory() + (rt.maxMemory() - rt.totalMemory());
            long threshold = rt.maxMemory() / 10;
            if (freeMemory < threshold) {
                System.err.println("   Low memory (" + (freeMemory / 1_048_576) + " MB free), skipping pair "
                        + src + " → " + dest);
                continue;
            }

            try {
                long startTime = System.nanoTime();
                algorithm.findPaths(src, dest, k);
                long endTime = System.nanoTime();

                double executionTime = (endTime - startTime) / 1_000_000_000.0;
                times.add(executionTime);

                int pathCount = 0;
                if (algorithm instanceof FindKSPD) {
                    pathCount = ((FindKSPD) algorithm).getNumberOfPathsExplored();
                } else if (algorithm instanceof Iterbound) {
                    pathCount = ((Iterbound) algorithm).getNumberOfPathsExplored();
                }
                numPaths.add(pathCount);
            } catch (OutOfMemoryError oom) {
                System.err.println("  ❌ OutOfMemoryError for pair " + src + " → " + dest
                        + " [" + algorithmName + "]: " + oom.getMessage());
                System.gc();
            } catch (Exception ex) {
                System.err.println("  ❌ Exception for pair " + src + " → " + dest
                        + " [" + algorithmName + "]: " + ex.getMessage());
            }
        }
        return new AlgorithmResult(algorithmName, times, numPaths);
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Figure 7: Vary k on different graphs for KSP ===");
        System.out.println("      FindKSP (τ=1.0) vs IterBound");
        System.out.println("      k ∈ {10, 20, 30, 40, 50}\n");

        int[] kValues = {10, 20, 30, 40, 50};

        String[][] graphs = {
                { "RoadFLA",    "graph-data/RoadFLA.gr"     },
        };

        if (!GraphLoader.ensureGraphsExist(graphs)) return;

        for (String[] entry : GeneratePairs.ALL_GRAPHS) {
            if (!GraphLoader.pairsExist(entry[0])) {
                System.out.println("\n  [Setup] Pairs not found. Generating pairs...");
                GeneratePairs.main(new String[]{});
                break;
            }
        }

        for (String[] entry : graphs) {
            String label = entry[0];
            String path  = entry[1];

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.printf( "║  Graph: %-42s ║%n", label);
            System.out.println("╚════════════════════════════════════════════════════╝");

            Graph graph;
            List<Pair<Integer, Integer>> nodePairs;
            try {
                System.out.println("  Loading graph: " + path);
                graph = GraphLoader.loadGraph(path);
                nodePairs = GraphLoader.loadPairs(label);
                System.out.printf("Nodes: %d  |  Pairs loaded: %d%n",
                        graph.getNodes().size(), nodePairs.size());
            } catch (IOException e) {
                System.err.println("❌ Could not load graph [" + label + "]: " + e.getMessage());
                continue;
            }

            List<AlgorithmResult> kspdResults = new ArrayList<>();
            List<AlgorithmResult> iterResults = new ArrayList<>();

            for (int k : kValues) {
                System.out.println("\n  ── k = " + k + " ──");

                System.out.println("\n  -- FindKSP (τ=1.0, k=" + k + ") --");
                FindKSPD kspd = new FindKSPD(graph, 1.0);
                AlgorithmResult resKSPD = runAlgorithm(kspd, "FindKSP(k=" + k + ")", nodePairs, k);
                kspdResults.add(resKSPD);

                System.out.println("\n  -- IterBound (k=" + k + ") --");
                Iterbound iterbound = new Iterbound(graph, 1.0);
                AlgorithmResult resIter = runAlgorithm(iterbound, "IterBound(k=" + k + ")", nodePairs, k);
                iterResults.add(resIter);
            }

            // ── Final Results Table ──
            System.out.println("\n\n  ╔══════════════════════════════════════════════════════════════╗");
            System.out.printf( "  ║  RESULTS: %s — Vary k (KSP)                            ║%n", label);
            System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
            System.out.printf("  %-5s | %-18s | %-14s | %-12s%n", "k", "Algorithm", "Avg Time(s)", "Avg Paths");
            System.out.println("  " + "-".repeat(59));

            for (int i = 0; i < kValues.length; i++) {
                System.out.printf("  %-5d | %-18s | %14.4f | %12.2f%n", kValues[i], "FindKSP(τ=1)", kspdResults.get(i).avgTime, kspdResults.get(i).avgNumPaths);
                System.out.printf("  %-5d | %-18s | %14.4f | %12.2f%n", kValues[i], "IterBound", iterResults.get(i).avgTime, iterResults.get(i).avgNumPaths);
                System.out.println("  " + "-".repeat(59));
            }

            // Per-k detaylı çıktı
            System.out.println("\n  ── Per-k summary for " + label + " ──");
            for (int i = 0; i < kValues.length; i++) {
                System.out.println("  k=" + kValues[i] + " FindKSP  runtime = " + kspdResults.get(i).times);
                System.out.println("  k=" + kValues[i] + " IterBound runtime = " + iterResults.get(i).times);
                System.out.println("  k=" + kValues[i] + " FindKSP  paths   = " + kspdResults.get(i).numPaths);
                System.out.println("  k=" + kValues[i] + " IterBound paths   = " + iterResults.get(i).numPaths);
            }
        }
    }
}
