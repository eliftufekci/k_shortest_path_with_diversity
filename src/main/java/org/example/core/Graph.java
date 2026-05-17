package org.example.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Weighted directed graph backed by an adjacency list.
 */
public class Graph {

    private final Map<Integer, Map<Integer, Double>> adjacencyList;
    private final Set<Integer>                       nodes;
    private int                                      edgeCount;

    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.nodes         = new HashSet<>();
        this.edgeCount     = 0;
    }

    public void addNode(int vertex) {
        if (!nodes.contains(vertex)) {
            adjacencyList.put(vertex, new HashMap<>());
            nodes.add(vertex);
        }
    }

    public void addWeightedEdge(int u, int v, double weight) {
        addNode(u);
        addNode(v);
        if (!adjacencyList.get(u).containsKey(v)) {
            edgeCount++;
        }
        adjacencyList.get(u).put(v, weight);
    }

    public double getEdgeWeight(int u, int v) {
        Map<Integer, Double> nbrs = adjacencyList.get(u);
        if (nbrs != null) {
            Double w = nbrs.get(v);
            if (w != null) return w;
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the neighbours of {@code vertex}.
     */
    public List<Integer> getNeighbors(int vertex) {
        Map<Integer, Double> nbrs = adjacencyList.get(vertex);
        return nbrs != null ? new ArrayList<>(nbrs.keySet()) : new ArrayList<>();
    }

    public boolean containsNode(int vertex) {
        return nodes.contains(vertex);
    }

    public boolean containsEdge(int u, int v) {
        Map<Integer, Double> nbrs = adjacencyList.get(u);
        return nbrs != null && nbrs.containsKey(v);
    }

    public Set<Integer>                       getNodes()         { return nodes; }
    public Map<Integer, Map<Integer, Double>> getAdjacencyList() { return adjacencyList; }
    public int                                getNodeCount()     { return nodes.size(); }
    public int                                getEdgeCount()     { return edgeCount; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Graph {\n  Vertices: ")
                .append(nodes).append("\n  Edges:\n");
        for (int u : adjacencyList.keySet()) {
            for (Map.Entry<Integer, Double> e : adjacencyList.get(u).entrySet()) {
                sb.append(String.format("    (%d→%d) %.2f%n", u, e.getKey(), e.getValue()));
            }
        }
        return sb.append("}").toString();
    }

    static class NodeDistance {
        final int    node;
        final double distance;

        NodeDistance(int node, double distance) {
            this.node     = node;
            this.distance = distance;
        }

        int    getNode()     { return node; }
        double getDistance() { return distance; }
    }
}
