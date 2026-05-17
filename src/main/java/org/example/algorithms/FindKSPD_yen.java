package org.example.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.example.core.Graph;
import org.example.core.Path;
import org.example.core.SimilarityMetric;

public class FindKSPD_yen extends BasePathFindingAlgorithm {

    private int numberOfPathsExplored;

    public FindKSPD_yen(Graph graph, double threshold, SimilarityMetric metric) {
        super(graph, threshold, metric);
    }

    public FindKSPD_yen(Graph graph, double threshold) {
        super(graph, threshold);
    }

    private Path dijkstraSimple(int src, int dest, Set<Integer> excludedNodes, Set<Long> excludedEdges) {
        if (src == dest) {
            Path path = new Path();
            path.appendVertex(src);
            return path;
        }

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> previousNodes = new HashMap<>();

        distances.put(src, 0.0);
        PriorityQueue<NodeCost> pq = new PriorityQueue<>();
        pq.add(new NodeCost(src, 0.0));

        while (!pq.isEmpty()) {
            NodeCost current = pq.poll();
            int u = current.node;
            double cost = current.cost;

            if (u == dest) break;
            if (cost > distances.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;
            if (excludedNodes.contains(u)) continue;


            for (int v : graph.getNeighbors(u)) {
                if (excludedNodes.contains(v)) continue;
                long edgeKey = Path.edgeKey(u, v);
                if (excludedEdges.contains(edgeKey)) continue;

                double weight = graph.getEdgeWeight(u, v);
                double newCost = cost + weight;

                if (newCost < distances.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    distances.put(v, newCost);
                    previousNodes.put(v, u);
                    pq.add(new NodeCost(v, newCost));
                }
            }
        }

        if (distances.getOrDefault(dest, Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY) return null;

        List<Integer> route = new ArrayList<>();
        Integer curr = dest;
        while (curr != null) {
            route.add(0, curr);
            curr = previousNodes.get(curr);
        }

        if (route.get(0) != src) return null;

        Path shortestPath = new Path();
        shortestPath.setRoute(route);
        for (int i = 0; i < route.size() - 1; i++) {
            int u = route.get(i), v = route.get(i + 1);
            double w = graph.getEdgeWeight(u, v);
            shortestPath.addEdge(u, v, w);
        }
        shortestPath.setLb(shortestPath.getLength());
        return shortestPath;
    }

    @Override
    public List<Path> findPaths(int src, int dest, int k) {
        validateParameters(src, dest, k);
        this.numberOfPathsExplored = 0;

        Path p1 = dijkstraSimple(src, dest, new HashSet<>(), new HashSet<>());
        if (p1 == null) return new ArrayList<>();

        List<Path> resultSet = new ArrayList<>();
        resultSet.add(p1);

        List<Path> acceptedPaths = new ArrayList<>();
        acceptedPaths.add(p1);

        Set<List<Integer>> seenRoutes = new HashSet<>();
        seenRoutes.add(new ArrayList<>(p1.getRoute()));

        PriorityQueue<Path> candidates = new PriorityQueue<>();

        generateSpurs(p1, dest, acceptedPaths, seenRoutes, candidates);

        while (resultSet.size() < k && !candidates.isEmpty()) {
            Path currentPath = candidates.poll();
            acceptedPaths.add(currentPath);
            generateSpurs(currentPath, dest, acceptedPaths, seenRoutes, candidates);

            if (currentPath.isFeasible(metric, threshold, resultSet)) {
                resultSet.add(currentPath);
            }
        }

        return resultSet;
    }

    private void generateSpurs(Path basePath, int dest, List<Path> acceptedPaths,
                               Set<List<Integer>> seenRoutes, PriorityQueue<Path> candidates) {
        List<Integer> baseRoute = basePath.getRoute();

        // Hangi accepted path'lerin hangi prefix'te basePath ile örtüştüğünü
        // tek seferde hesapla — her spur node için ayrı ayrı değil
        int maxSpurIdx = baseRoute.size() - 1;

        // Her accepted path için kaçıncı pozisyona kadar baseRoute ile aynı?
        int[] matchDepth = new int[acceptedPaths.size()];
        for (int p = 0; p < acceptedPaths.size(); p++) {
            List<Integer> route = acceptedPaths.get(p).getRoute();
            int depth = 0;
            while (depth < Math.min(maxSpurIdx, route.size() - 1)
                    && route.get(depth).equals(baseRoute.get(depth))) {
                depth++;
            }
            matchDepth[p] = depth; // bu path baseRoute ile depth-1'e kadar örtüşüyor
        }

        Set<Integer> excludedNodes = new HashSet<>();

        for (int i = 0; i < maxSpurIdx; i++) {
            int spurNode = baseRoute.get(i);

            Set<Long> excludedEdges = new HashSet<>();
            for (int p = 0; p < acceptedPaths.size(); p++) {
                // O(1) kontrol — O(i) karşılaştırma yerine
                if (matchDepth[p] > i) {
                    List<Integer> route = acceptedPaths.get(p).getRoute();
                    if (route.size() > i + 1) {
                        excludedEdges.add(Path.edgeKey(route.get(i), route.get(i + 1)));
                    }
                }
            }

            if (i > 0) excludedNodes.add(baseRoute.get(i - 1));

            Path spurPath = dijkstraSimple(spurNode, dest, excludedNodes, excludedEdges);
            if (spurPath == null) continue;

            this.numberOfPathsExplored++;

            List<Integer> totalRoute = new ArrayList<>(baseRoute.subList(0, i));
            totalRoute.addAll(spurPath.getRoute());

            if (seenRoutes.contains(totalRoute)) continue;
            seenRoutes.add(totalRoute);

            Path totalPath = new Path();
            totalPath.setRoute(totalRoute);
            for (int j = 0; j < totalRoute.size() - 1; j++) {
                int u = totalRoute.get(j), v = totalRoute.get(j + 1);
                totalPath.addEdge(u, v, graph.getEdgeWeight(u, v));
            }
            totalPath.setLb(totalPath.getLength());
            candidates.add(totalPath);
        }
    }

    private static class NodeCost implements Comparable<NodeCost> {
        int node;
        double cost;
        NodeCost(int node, double cost) { this.node = node; this.cost = cost; }
        @Override
        public int compareTo(NodeCost other) { return Double.compare(this.cost, other.cost); }
    }

    public int getNumberOfPathsExplored() {
        return numberOfPathsExplored;
    }
}
