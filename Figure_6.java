import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithms.BasePathFindingAlgorithm;
import algorithms.FindKSPD;
import algorithms.FindKSPD_;
import algorithms.Iterbound;
import core.Graph;
import core.Pair;

/**
 * Figure 6: Efficiency on different graphs for KSP (k=30).
 *
 * Compares FindKSP (τ=1.0) vs IterBound on all graphs.
 * k = 30, τ = 1.0 (plain KSP, no diversity)
 * Graphs: web-Google, RoadCOL, RoadFLA, WikiTalk
 * Metrics: Average runtime (s), Average number of paths explored
 *
 * Pairs are read from graph-data/pairs/ directory.
 */
public class Figure_6 {

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

        @Override
        public String toString() {
            return String.format("%s - Avg Time: %.4f s, Avg Paths: %.2f%n  Times: %s%n  Paths: %s",
                    algorithmName, avgTime, avgNumPaths, times, numPaths);
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
                } else if (algorithm instanceof FindKSPD_) {
                    pathCount = ((FindKSPD_) algorithm).getNumberOfPathsExplored();
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
    // Per-graph comparison helper
    // -------------------------------------------------------------------------

    private static AlgorithmResult[] runGraphComparison(
            String graphLabel, String graphPath, int k) throws IOException {

        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.printf( "║  Graph: %-42s ║%n", graphLabel);
        System.out.println("╚════════════════════════════════════════════════════╝");

        System.out.println("  Loading graph: " + graphPath);
        Graph graph = GraphLoader.loadGraph(graphPath);

        List<Pair<Integer, Integer>> nodePairs = GraphLoader.loadPairs(graphLabel);
        System.out.printf("Nodes: %d  |  Pairs loaded: %d%n",
                graph.getNodes().size(), nodePairs.size());

        System.out.println("\n  -- FindKSPD (t=1.0) --");
        FindKSPD kspd = new FindKSPD(graph, 1.0);
        AlgorithmResult resKSPD = runAlgorithm(kspd, "FindKSPD(t=1)", nodePairs, k);

        System.out.println("\n  -- Iterbound --");
        Iterbound iterbound = new Iterbound(graph, 1.0);
        AlgorithmResult resIter = runAlgorithm(iterbound, "Iterbound", nodePairs, k);

        System.out.println("\n  ── Summary for " + graphLabel + " (k=" + k + ") ──");
        System.out.printf("  %-18s | %-14s | %-12s%n", "Algorithm", "Avg Time(s)", "Avg Paths");
        System.out.println("  " + "-".repeat(50));
        System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD(t=1)", resKSPD.avgTime, resKSPD.avgNumPaths);
        System.out.printf("  %-18s | %14.4f | %12.2f%n", "Iterbound", resIter.avgTime, resIter.avgNumPaths);

        // Per-list output
        System.out.println("\n  ── Per-pair lists for " + graphLabel + " ──");
        System.out.println("  " + graphLabel + " - FindKSPD runtime = " + resKSPD.times);
        System.out.println("  " + graphLabel + " - Iterbound runtime = " + resIter.times);
        System.out.println("  " + graphLabel + " - FindKSPD num of paths = " + resKSPD.numPaths);
        System.out.println("  " + graphLabel + " - Iterbound num of paths = " + resIter.numPaths);

        return new AlgorithmResult[]{resKSPD, resIter};
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Figure 6: KSP Comparison — FindKSPD (t=1.0) vs Iterbound ===");
        System.out.println("      4 Graphs  |  20 pairs each  |  k=30\n");

        int k = 30;

        String[][] graphs = {
                { "web-Google", "graph-data/web-Google.txt" },
                { "RoadCOL",    "graph-data/RoadCOL.gr"     },
                { "RoadFLA",    "graph-data/RoadFLA.gr"     },
                { "WikiTalk",   "graph-data/WikiTalk.txt"   },
        };

        if (!GraphLoader.ensureGraphsExist(graphs)) return;

        for (String[] entry : graphs) {
            if (!GraphLoader.pairsExist(entry[0])) {
                System.out.println("\n  [Setup] Pairs not found. Generating pairs...");
                GeneratePairs.main(new String[]{});
                break;
            }
        }

        // Accumulators for overall averages
        double totalKspdTime  = 0, totalKspdPaths  = 0;
        double totalIterTime  = 0, totalIterPaths  = 0;
        int    successfulRuns = 0;

        for (String[] entry : graphs) {
            String label = entry[0];
            String path  = entry[1];
            try {
                AlgorithmResult[] results = runGraphComparison(label, path, k);
                totalKspdTime  += results[0].avgTime;
                totalKspdPaths += results[0].avgNumPaths;
                totalIterTime  += results[1].avgTime;
                totalIterPaths += results[1].avgNumPaths;
                successfulRuns++;
            } catch (IOException e) {
                System.err.println("\n❌  Could not load graph [" + label + "]: " + e.getMessage());
            } catch (OutOfMemoryError oom) {
                System.err.println("\n❌  OutOfMemoryError while processing graph [" + label + "]: " + oom.getMessage());
                System.gc();
            }
        }

        // ── Overall summary ──
        System.out.println("\n\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        OVERALL AVERAGE  (across " + successfulRuns + " graphs, k=" + k + ")           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        if (successfulRuns > 0) {
            double n = successfulRuns;
            System.out.printf("  %-18s | %-14s | %-12s%n", "Algorithm", "Avg Runtime(s)", "Avg Paths");
            System.out.println("  " + "-".repeat(52));
            System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD(t=1)", totalKspdTime / n, totalKspdPaths / n);
            System.out.printf("  %-18s | %14.4f | %12.2f%n", "Iterbound", totalIterTime / n, totalIterPaths / n);
        } else {
            System.out.println("  No graphs were loaded successfully.");
        }
    }
}
