package org.example.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.example.core.Graph.NodeDistance;

/**
 * Holds the incremental state of the reverse shortest-path tree (SPT) rooted
 * at the destination, built lazily by {@link GraphUtils#constructPartialSPT}.
 */
public class GraphState {

    private final Graph  graphReverse;
    private final int    destination;

    private final Map<Integer, Double>  distances;
    private final Map<Integer, Boolean> isSettled;
    private final Map<Integer, Integer> parent;
    private final PriorityQueue<NodeDistance> pq;

    public GraphState(Graph graphReverse, int destination) {
        this.graphReverse = graphReverse;
        this.destination  = destination;
        this.distances    = new HashMap<>();
        this.isSettled    = new HashMap<>();
        this.parent       = new HashMap<>();
        this.pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::getDistance));

        // Seed the SPT at the destination with distance 0.
        this.distances.put(destination, 0.0);
        this.pq.offer(new NodeDistance(destination, 0.0));
    }
    public void       addToPQ(int node, double dist) { pq.offer(new NodeDistance(node, dist)); }
    public NodeDistance pollPQ()                     { return pq.poll(); }
    public PriorityQueue<NodeDistance> getPq()       { return pq; }

    public double  getDistance(int v)            { return distances.getOrDefault(v, Double.POSITIVE_INFINITY); }
    public void    setDistance(int v, double d)  { distances.put(v, d); }

    public boolean isSettled(int v)              { return isSettled.getOrDefault(v, false); }
    public void    setSettled(int v, boolean s)  { isSettled.put(v, s); }

    public Integer getParent(int v)              { return parent.get(v); }          
    public void    setParent(int v, Integer p)   { parent.put(v, p); }

    public Graph getGraphReverse() { return graphReverse; }
    public int   getDestination()  { return destination; }

}
