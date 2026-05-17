package org.example.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.example.core.Graph.NodeDistance;

public class GraphUtils {
    public static Graph reverse(Graph graph) {
        Graph graphReverse = new Graph();

        for (int u : graph.getNodes()) {
            for (int v : graph.getNeighbors(u)) {
                Double weight = graph.getEdgeWeight(u, v);
                graphReverse.addWeightedEdge(v, u, weight);
            }
        }

        return graphReverse;
    }

    public static Path dijkstra(Graph graph, int src, int dest) {
        if (!graph.containsNode(src) || !graph.containsNode(dest)) {
            throw new IllegalArgumentException("Source ya da destination grafte yok");
        }

        if (src == dest) {
            Path path = new Path();
            path.setRoute(new ArrayList<>(Arrays.asList(src)));
            path.setLength(0.0);
            return path;
        }

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(
            Comparator.comparingDouble(NodeDistance::getDistance)
        );

        for (int node : graph.getNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
            previous.put(node, null);
        }
        distances.put(src, 0.0);
        pq.offer(new NodeDistance(src, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            int u = current.getNode();

            if (current.getDistance() > distances.get(u)) {
                continue;
            }

            if (u == dest) {
                break;
            }

            for (int v : graph.getNeighbors(u)) {
                Double weight = graph.getEdgeWeight(u, v);
                double alt = distances.get(u) + weight;

                if (alt < distances.get(v)) {
                    distances.put(v, alt);
                    previous.put(v, u);
                    pq.offer(new NodeDistance(v, alt));
                }
            }
        }

        Path path = new Path();
        if (distances.get(dest) == Double.POSITIVE_INFINITY) {
            return path; 
        }

        List<Integer> route = new ArrayList<>();
        Integer current = dest;
        while (current != null) {
            route.add(0, current);
            current = previous.get(current);
        }

        path.setRoute(route);
        for (int i = 0; i < route.size() - 1; i++) {
            int u = route.get(i);
            int v = route.get(i + 1);
            Double weight = graph.getEdgeWeight(u, v);
            if (weight != null) {
                path.addEdge(u, v, weight);
            }
        }
        path.setLb(path.getLength());

        return path;
    }

    public static double constructPartialSPT(GraphState graphState, int v) {
        if (graphState.isSettled(v)) {
            return graphState.getDistance(v);
        }

        Graph graphReverse = graphState.getGraphReverse();

        while (!graphState.getPq().isEmpty()) {
            NodeDistance current = graphState.pollPQ();
            int node = current.getNode();
            double cost = current.getDistance();

            if (cost > graphState.getDistance(node)) {
                continue;
            }

            if (!graphState.isSettled(node)) {
                graphState.setSettled(node, true);

                for (int neighbor : graphReverse.getNeighbors(node)) {
                    if (!graphState.isSettled(neighbor)) {
                        Double weight = graphReverse.getEdgeWeight(node, neighbor);
                        if (weight != null) {
                            double newCost = cost + weight;

                            if (newCost < graphState.getDistance(neighbor)) {
                                graphState.setDistance(neighbor, newCost);
                                graphState.setParent(neighbor, node);
                                graphState.addToPQ(neighbor, newCost);
                            }
                        }
                    }
                }

                if (node == v) {
                    return graphState.getDistance(v);
                }
            }
        }

        return Double.POSITIVE_INFINITY;
    }

}
