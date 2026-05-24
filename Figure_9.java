import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithms.BasePathFindingAlgorithm;
import algorithms.FindKSPD;
import algorithms.FindKSPD_;
import algorithms.FindKSPD_yen;
import core.Graph;
import core.Pair;

/**
 * Figure 9: Efficiency on different graphs for KSPD.
 *
 * Compares FindKSPD vs FindKSPD- vs KSPD-Yen on all graphs.
 * k = 10, τ = 0.6, Similarity Metric = Sim1
 * Graphs: web-Google, RoadCOL, RoadFLA, WikiTalk
 * Metrics: Average runtime (s), Average number of paths explored
 *
 * Pairs are read from graph-data/pairs/ directory.
 */
public class Figure_9 {

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
            long threshold = rt.maxMemory() / 10;
            if (freeMemory < threshold) {
                System.err.println("   Low memory (" + (freeMemory / 1_048_576) + " MB free), skipping pair " + src + " → " + dest);
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
                else if (algorithm instanceof FindKSPD_yen) pathCount = ((FindKSPD_yen) algorithm).getNumberOfPathsExplored();
                numPaths.add(pathCount);
            } catch (OutOfMemoryError oom) {
                System.err.println("  ❌ OutOfMemoryError for pair " + src + " → " + dest
                        + " [" + algorithmName + "]: " + oom.getMessage());
                System.gc();
            } catch (Exception ex) {
                System.err.println("  ❌ Exception: " + ex.getMessage());
            }
        }
        return new AlgorithmResult(algorithmName, times, numPaths);
    }

    private static AlgorithmResult[] runGraphComparison(
            String graphLabel, String graphPath, double threshold, int k) throws IOException {

        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.printf( "║  Graph: %-42s ║%n", graphLabel);
        System.out.println("╚════════════════════════════════════════════════════╝");

        System.out.println("  Loading graph: " + graphPath);
        Graph graph = GraphLoader.loadGraph(graphPath);
        List<Pair<Integer, Integer>> nodePairs = GraphLoader.loadPairs(graphLabel);
        System.out.printf("Nodes: %d  |  Pairs loaded: %d%n", graph.getNodes().size(), nodePairs.size());

        FindKSPD kspd = new FindKSPD(graph, threshold);
        AlgorithmResult resKSPD = runAlgorithm(kspd, "FindKSPD", nodePairs, k);

        FindKSPD_ kspd_ = new FindKSPD_(graph, threshold);
        AlgorithmResult resKSPD_ = runAlgorithm(kspd_, "FindKSPD-", nodePairs, k);

        FindKSPD_yen kspdYen = new FindKSPD_yen(graph, threshold);
        AlgorithmResult resKSPDYen = runAlgorithm(kspdYen, "KSPD-Yen", nodePairs, k);

        System.out.println("\n  ── Summary for " + graphLabel + " (k=" + k + ", τ=" + threshold + ") ──");
        System.out.printf("  %-18s | %-14s | %-12s%n", "Algorithm", "Avg Time(s)", "Avg Paths");
        System.out.println("  " + "-".repeat(50));
        System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD",  resKSPD.avgTime,    resKSPD.avgNumPaths);
        System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD-", resKSPD_.avgTime,   resKSPD_.avgNumPaths);
        System.out.printf("  %-18s | %14.4f | %12.2f%n", "KSPD-Yen",  resKSPDYen.avgTime, resKSPDYen.avgNumPaths);

        System.out.println("\n  ── Per-pair lists for " + graphLabel + " ──");
        System.out.println("  FindKSPD runtime = "  + resKSPD.times);
        System.out.println("  FindKSPD- runtime = " + resKSPD_.times);
        System.out.println("  KSPD-Yen runtime = "  + resKSPDYen.times);
        System.out.println("  FindKSPD paths = "    + resKSPD.numPaths);
        System.out.println("  FindKSPD- paths = "   + resKSPD_.numPaths);
        System.out.println("  KSPD-Yen paths = "    + resKSPDYen.numPaths);

        return new AlgorithmResult[]{resKSPD, resKSPD_};
    }

    public static void main(String[] args) {
        System.out.println("=== Figure 9: Efficiency on different graphs for KSPD ===");
        System.out.println("      FindKSPD vs FindKSPD- vs KSPD-Yen | k=10, τ=0.6, Sim1\n");

        int k = 10; double threshold = 0.6;
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

        double totalKspdT = 0, totalKspdP = 0, totalKspd_T = 0, totalKspd_P = 0, totalYenT = 0, totalYenP = 0;
        int ok = 0;

        for (String[] entry : graphs) {
            try {
                AlgorithmResult[] r = runGraphComparison(entry[0], entry[1], threshold, k);
                totalKspdT += r[0].avgTime; totalKspdP += r[0].avgNumPaths;
                totalKspd_T += r[1].avgTime; totalKspd_P += r[1].avgNumPaths;
                totalYenT += r[2].avgTime; totalYenP += r[2].avgNumPaths;
                ok++;
            } catch (IOException e) {
                System.err.println("❌ " + entry[0] + ": " + e.getMessage());
            } catch (OutOfMemoryError oom) {
                System.err.println("❌ OutOfMemoryError while processing graph [" + entry[0] + "]: " + oom.getMessage());
                System.gc();
            }
        }

        System.out.println("\n\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  OVERALL AVERAGE  (" + ok + " graphs, k=" + k + ", τ=" + threshold + ")              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        if (ok > 0) {
            double n = ok;
            System.out.printf("  %-18s | %-14s | %-12s%n", "Algorithm", "Avg Runtime(s)", "Avg Paths");
            System.out.println("  " + "-".repeat(52));
            System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD",  totalKspdT/n,  totalKspdP/n);
            System.out.printf("  %-18s | %14.4f | %12.2f%n", "FindKSPD-", totalKspd_T/n, totalKspd_P/n);
            System.out.printf("  %-18s | %14.4f | %12.2f%n", "KSPD-Yen",  totalYenT/n,   totalYenP/n);
        }
    }
}
