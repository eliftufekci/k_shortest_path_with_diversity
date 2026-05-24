package algorithms;

import core.Path;
import core.SimilarityMetric;
import core.Graph;

import java.util.List;

public abstract class BasePathFindingAlgorithm {

    protected final Graph            graph;
    protected final double           threshold;
    protected final SimilarityMetric metric;

    public BasePathFindingAlgorithm(Graph graph, double threshold, SimilarityMetric metric) {
        this.graph     = graph;
        this.threshold = threshold;
        this.metric    = metric;
    }

    /** defaults to Sim1 */
    public BasePathFindingAlgorithm(Graph graph, double threshold) {
        this(graph, threshold, SimilarityMetric.SIM1);
    }

    public abstract List<Path> findPaths(int src, int dest, int k);

    protected void validateParameters(int src, int dest, int k) {
        if (!graph.containsNode(src) || !graph.containsNode(dest)) {
            throw new IllegalArgumentException(
                    "Source or destination node does not exist in the graph");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be a positive integer");
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "Threshold must be in [0, 1]; use τ=1 to run plain KSP");
        }
    }

    public Graph            getGraph()     { return graph; }
    public double           getThreshold() { return threshold; }
    public SimilarityMetric getMetric()    { return metric; }
}
