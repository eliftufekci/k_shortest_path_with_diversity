package org.example.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A (possibly partial) path in the graph.
 *
 * <p><b>İki mod vardır:</b>
 * <ol>
 * <li><b>Mutable mod</b> – {@code new Path()} ile oluşturulur.
 * {@code appendVertex} / {@code addEdge} / {@code setRoute} çağrılabilir.</li>
 * <li><b>Linked mod</b> – {@code path.branch(v, w)} ile oluşturulur.
 * Hiçbir veri yapısı kopyalanmaz (O(1)).
 * Herhangi bir mutator çağrıldığında (appendVertex/addEdge) otomatik olarak mutable moda geçer (promote).</li>
 * </ol>
 */
public class Path implements Comparable<Path> {

    // ── Mutable-mod alanları ──────────────────────────────────────────────────
    private List<Integer>     route;     // null ise linked-mod (lazy)
    private Set<Integer>      routeSet;  // null ise linked-mod (lazy)
    private Map<Long, Double> edges;     // null ise linked-mod (lazy)
    private double            length;

    // ── Linked-mod alanları (branch() ile set edilir) ─────────────────────────
    private Path   linkedParent;       // null → mutable mod
    private int    linkedVertex;       // bu node'da eklenen vertex
    private double linkedEdgeWeight;   // linkedParent.tail() → linkedVertex kenar ağırlığı

    // ── Algoritma metadata (her iki modda da mutable) ─────────────────────────
    private Pair<Integer, Integer> cls;
    private double  lb;
    private boolean isActive;

    private static long pathIdCounter = 0;
    private final  long pathId;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────────

    public Path() {
        this.route    = new ArrayList<>();
        this.routeSet = new HashSet<>();
        this.edges    = new HashMap<>();
        this.length   = 0.0;
        this.lb       = Double.POSITIVE_INFINITY;
        this.isActive = true;
        this.pathId   = pathIdCounter++;
    }

    private Path(Path parent, int vertex, double edgeWeight) {
        this.linkedParent     = parent;
        this.linkedVertex     = vertex;
        this.linkedEdgeWeight = edgeWeight;
        this.length           = parent.length + edgeWeight;
        this.lb       = Double.POSITIVE_INFINITY;
        this.isActive = true;
        this.pathId   = pathIdCounter++;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Promotion Logic (Linked -> Mutable)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Eğer path linked moddaysa, onu tam bir mutable path'e dönüştürür (promote).
     * Mutation işlemleri (appendVertex/addEdge) öncesi çağrılmalıdır.
     */
    private void ensureMutable() {
        if (linkedParent != null) {
            // Lazy materialization metotlarını çağırarak route ve edges'i doldur
            getRoute();
            getEdges();
            // routeSet linked modda hiç materialize edilmez, burada oluşturuyoruz
            if (this.routeSet == null) {
                this.routeSet = new HashSet<>(this.route);
            }
            // Zinciri kopar, artık tamamen bağımsız/mutable bir path
            this.linkedParent = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core Methods
    // ─────────────────────────────────────────────────────────────────────────

    public Path branch(int v, double w) {
        return new Path(this, v, w);
    }

    public int tail() {
        if (linkedParent != null) return linkedVertex;
        return (route == null || route.isEmpty()) ? -1 : route.get(route.size() - 1);
    }

    public int head() {
        if (linkedParent != null) {
            Path cur = this;
            while (cur.linkedParent != null) cur = cur.linkedParent;
            return (cur.route == null || cur.route.isEmpty()) ? -1 : cur.route.get(0);
        }
        return (route == null || route.isEmpty()) ? -1 : route.get(0);
    }

    public boolean containsVertex(int v) {
        if (linkedParent != null) {
            Path cur = this;
            while (cur.linkedParent != null) {
                if (cur.linkedVertex == v) return true;
                cur = cur.linkedParent;
            }
            return cur.routeSet != null && cur.routeSet.contains(v);
        }
        return routeSet != null && routeSet.contains(v);
    }

    public List<Integer> getRoute() {
        if (linkedParent == null) {
            return (route != null) ? route : Collections.emptyList();
        }
        if (route == null) {
            route = new ArrayList<>(linkedParent.getRoute());
            route.add(linkedVertex);
        }
        return route;
    }

    public Map<Long, Double> getEdges() {
        if (linkedParent == null) {
            return (edges != null) ? edges : Collections.emptyMap();
        }
        if (edges == null) {
            Map<Long, Double> parentEdges = linkedParent.getEdges();
            edges = new HashMap<>(parentEdges.size() + 1, 1.0f);
            edges.putAll(parentEdges);
            edges.put(edgeKey(linkedParent.tail(), linkedVertex), linkedEdgeWeight);
        }
        return edges;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mutators (Artık hem Linked hem Mutable modda güvenli)
    // ─────────────────────────────────────────────────────────────────────────

    public void appendVertex(int v) {
        ensureMutable(); // NullPointerException'ı önleyen kritik çağrı
        route.add(v);
        routeSet.add(v);
    }

    public void addEdge(int u, int v, double weight) {
        ensureMutable();
        edges.put(edgeKey(u, v), weight);
        length += weight;
    }

    public void setRoute(List<Integer> newRoute) {
        this.route    = newRoute;
        this.routeSet = new HashSet<>(newRoute);
        this.linkedParent = null; // Manuel set yapıldıysa bağımsızdır
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Utils & Algorithm Logic
    // ─────────────────────────────────────────────────────────────────────────

    public static long edgeKey(int u, int v) {
        return ((long) (u & 0xFFFFFFFFL)) << 32 | (v & 0xFFFFFFFFL);
    }

    private double intersectionLength(Path other) {
        Map<Long, Double> myEdges    = this.getEdges();
        Map<Long, Double> otherEdges = other.getEdges();
        Map<Long, Double> smaller = myEdges.size() <= otherEdges.size() ? myEdges : otherEdges;
        Map<Long, Double> larger  = (smaller == myEdges) ? otherEdges : myEdges;
        double inter = 0.0;
        for (Map.Entry<Long, Double> e : smaller.entrySet()) {
            if (larger.containsKey(e.getKey())) {
                inter += e.getValue();
            }
        }
        return inter;
    }

    private double similarity(SimilarityMetric metric, double inter, double otherLen) {
        switch (metric) {
            case SIM1: {
                double union = this.length + otherLen - inter;
                return union > 0.0 ? inter / union : 0.0;
            }
            case SIM2:
                if (this.length == 0.0 || otherLen == 0.0) return 0.0;
                return inter / (2.0 * this.length) + inter / (2.0 * otherLen);
            case SIM3:
                if (this.length == 0.0 || otherLen == 0.0) return 0.0;
                return Math.sqrt((inter * inter) / (this.length * otherLen));
            case SIM4:
                double maxLen = Math.max(this.length, otherLen);
                return maxLen > 0.0 ? inter / maxLen : 0.0;
            case SIM5:
                double minLen = Math.min(this.length, otherLen);
                return minLen > 0.0 ? inter / minLen : 0.0;
            default:
                return 0.0;
        }
    }

    public boolean isFeasible(SimilarityMetric metric, double threshold, List<Path> resultSet) {
        for (Path p : resultSet) {
            double inter = intersectionLength(p);
            if (similarity(metric, inter, p.length) > threshold) {
                return false;
            }
        }
        return true;
    }

    public double calculateLB1(GraphState graphState) {
        int tailVertex = tail();
        if (!graphState.isSettled(tailVertex)) {
            GraphUtils.constructPartialSPT(graphState, tailVertex);
        }
        return length + graphState.getDistance(tailVertex);
    }

    public double calculateLB2(SimilarityMetric metric, double threshold, List<Path> resultSet) {
        if (resultSet.isEmpty()) return 0.0;
        double lb2 = 0.0;
        for (Path p : resultSet) {
            double inter = intersectionLength(p);
            double val   = lb2ForOnePath(metric, threshold, inter, p.length);
            if (val == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
            lb2 = Math.max(lb2, val);
        }
        return lb2;
    }

    private double lb2ForOnePath(SimilarityMetric metric, double tau, double inter, double otherLen) {
        switch (metric) {
            case SIM1: return inter * (1.0 + 1.0 / tau) - otherLen;
            case SIM2: {
                double denom = 2.0 * tau * otherLen - inter;
                if (denom <= 0.0) return Double.POSITIVE_INFINITY;
                return (inter * otherLen) / denom;
            }
            case SIM3:
                if (otherLen == 0.0) return 0.0;
                return (inter * inter) / (tau * tau * otherLen);
            case SIM4: return (this.length >= otherLen) ? inter / tau : this.length;
            case SIM5: return (inter >= tau * otherLen) ? Double.POSITIVE_INFINITY : this.length;
            default: return 0.0;
        }
    }

    public Path copy() {
        Path c = new Path();
        c.route    = new ArrayList<>(getRoute());
        c.routeSet = new HashSet<>(c.route);
        c.edges    = new HashMap<>(getEdges());
        c.length   = length;
        c.lb       = lb;
        c.isActive = isActive;
        if (cls != null) {
            c.cls = new Pair<>(cls.getFirst(), cls.getSecond());
        }
        return c;
    }

    @Override
    public int compareTo(Path other) {
        if (this.isActive != other.isActive) return this.isActive ? -1 : 1;
        int c = Double.compare(this.lb, other.lb);
        if (c != 0) return c;
        return Long.compare(this.pathId, other.pathId);
    }

    @Override
    public String toString() {
        return String.format("Path(route=%s, len=%.2f, lb=%.2f, active=%s, cls=%s)",
                getRoute(), length, lb, isActive, cls);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public double  getLength()                  { return length; }
    public void    setLength(double l)          { this.length = l; }
    public double  getLb()                      { return lb; }
    public void    setLb(double lb)             { this.lb = lb; }
    public boolean isActive()                   { return isActive; }
    public void    setActive(boolean a)         { this.isActive = a; }
    public Pair<Integer, Integer> getCls()      { return cls; }
    public void setCls(int pathNum, int vertex) { this.cls = new Pair<>(pathNum, vertex); }
    public void setCls(Pair<Integer, Integer> cls) { this.cls = cls; }
    public long getPathId()                     { return pathId; }
    public void setEdges(Map<Long, Double> e)   { this.edges = e; }
    public static long getPathIdCounter()       { return pathIdCounter; }
    public static void setPathIdCounter(long c) { pathIdCounter = c; }
}