package org.example.algorithms;

import java.util.*;

import org.example.core.Graph;
import org.example.core.GraphState;
import org.example.core.GraphUtils;
import org.example.core.Path;
import org.example.core.SimilarityMetric;

/**
 * IterBound algorithm for the K Shortest Paths problem.
 * "Efficient Top-k Shortest-Path Distance Queries on Large Networks
 *  by Pruning Candidate Paths"
 *
 * IterBound is a variant of Yen's algorithm that computes candidate
 * shortest paths in a best-first manner based on their lower bounds,
 * using an iteratively bounding approach (bounded Dijkstra with tau).
 */
public class Iterbound extends BasePathFindingAlgorithm {

    private static final double PATH_LEN_FACTOR = 3.0;   // upperBound = p0 * factor
    private static final double ALPHA           = 1.1;    // tau büyütme katsayısı (paper default)
    private static final int    MAX_RETRIES     = 50;
    private static final int    MAX_ITERATIONS  = 500_000;
    private int numberOfPathsExplored;

    public Iterbound(Graph graph, double threshold, SimilarityMetric metric) {
        super(graph, threshold, metric);
    }
    public Iterbound(Graph graph, double threshold) {
        super(graph, threshold);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner types
    // ─────────────────────────────────────────────────────────────────────────

    private static class Subspace {
        final Path      prefix;           // (v1, ..., v_devNode) kısmı
        final Set<Long> excludedEdges;    // prefix.tail()'den çıkan yasaklı kenarlar
        Path            computedPath;     // bounded Dijkstra sonucu (null = henüz hesaplanmadı)
        double          tau;              // bu subspace için son kullanılan tau bound
        int             retries;          // kaç kez path bulunamadı

        Subspace(Path prefix, Set<Long> excludedEdges) {
            this.prefix        = (prefix        != null) ? prefix        : new Path();
            this.excludedEdges = (excludedEdges != null) ? excludedEdges : new HashSet<>();
        }
    }

    private static class Entry implements Comparable<Entry> {
        final double   lb;
        final long     seq;   // tie-break
        final Subspace sub;

        Entry(double lb, long seq, Subspace sub) {
            this.lb  = lb;
            this.seq = seq;
            this.sub = sub;
        }

        @Override
        public int compareTo(Entry o) {
            int c = Double.compare(lb, o.lb);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }

    private static class NodeEntry implements Comparable<NodeEntry> {
        final int    node;
        final double g, f;   // g = gerçek mesafe, f = g + h (heuristic)

        NodeEntry(int node, double g, double f) {
            this.node = node; this.g = g; this.f = f;
        }

        @Override
        public int compareTo(NodeEntry o) { return Double.compare(f, o.f); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LB1: prefix tail'inden hedefe en kısa yol tahmini
    // ─────────────────────────────────────────────────────────────────────────
    private double computeLB1(Subspace sub, GraphState gs) {
        int u = sub.prefix.tail();
        if (u == -1) return Double.POSITIVE_INFINITY;

        // tail'den dest'e heuristic (reverse SPT üzerinden)
        if (!gs.isSettled(u)) GraphUtils.constructPartialSPT(gs, u);
        double directH = gs.getDistance(u);

        // Eğer tail doğrudan ulaşılabilirse, prefix uzunluğu + h
        // Ama excluded edge'ler yüzünden bazı yollar kapalı olabilir,
        // o yüzden komşuları kontrol ediyoruz
        double best = Double.POSITIVE_INFINITY;
        for (int v : graph.getNeighbors(u)) {
            if (sub.prefix.containsVertex(v)) continue;        // cycle önleme
            if (sub.excludedEdges.contains(Path.edgeKey(u, v))) continue; // excluded
            if (!gs.isSettled(v)) GraphUtils.constructPartialSPT(gs, v);
            double h = gs.getDistance(v);
            if (h == Double.POSITIVE_INFINITY) continue;
            double est = sub.prefix.getLength() + graph.getEdgeWeight(u, v) + h;
            if (est < best) best = est;
        }
        return best;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Bounded Dijkstra: tau sınırı içinde dest'e path bul
    // ─────────────────────────────────────────────────────────────────────────

    private Path findWithinTau(Subspace sub, GraphState gs, double tau, int dest) {
        int    startNode = sub.prefix.tail();
        double startG    = sub.prefix.getLength();

        if (!gs.isSettled(startNode)) GraphUtils.constructPartialSPT(gs, startNode);
        if (startG + gs.getDistance(startNode) > tau) return null; // erken çıkış

        // prefix'teki vertex'lerin set'i (cycle önleme için)
        // tail hariç — aramaya oradan başlıyoruz
        Set<Integer> prefixVertices = new HashSet<>(sub.prefix.getRoute());
        prefixVertices.remove(startNode); // startNode'dan arama başlıyor

        Map<Integer, Double>  dist   = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        dist.put(startNode, startG);
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>();
        pq.add(new NodeEntry(startNode, startG, startG + gs.getDistance(startNode)));
        Set<Integer> closed = new HashSet<>();

        while (!pq.isEmpty()) {
            NodeEntry cur = pq.poll();
            if (!closed.add(cur.node)) continue;

            // Hedefe ulaştık — path'i oluştur
            if (cur.node == dest) {
                return buildFullPath(sub.prefix, parent, dest);
            }

            for (int nb : graph.getNeighbors(cur.node)) {
                // Prefix'teki vertex'lere gitme — cycle oluşur
                if (prefixVertices.contains(nb)) continue;

                // Excluded edge kontrolü
                if (sub.excludedEdges.contains(Path.edgeKey(cur.node, nb))) continue;

                double w    = graph.getEdgeWeight(cur.node, nb);
                double newG = cur.g + w;

                // Heuristic hesapla
                if (!gs.isSettled(nb)) GraphUtils.constructPartialSPT(gs, nb);
                double h    = gs.getDistance(nb);
                double newF = newG + h;

                // Tau sınırı kontrolü
                if (newF <= tau) {
                    Double prev = dist.get(nb);
                    if (prev == null || newG < prev) {
                        dist.put(nb, newG);
                        parent.put(nb, cur.node);
                        pq.add(new NodeEntry(nb, newG, newF));
                    }
                }
            }
        }
        return null; // tau içinde path bulunamadı
    }

    /**
     * Prefix + bounded Dijkstra parent map'inden tam path oluşturur.
     */
    private Path buildFullPath(Path prefix, Map<Integer, Integer> parent, int dest) {
        // Suffix: parent map'ten dest → prefix.tail() geriye iz sür
        List<Integer> suffix = new ArrayList<>();
        for (Integer cur = dest; cur != null; cur = parent.get(cur)) {
            suffix.add(cur);
        }
        Collections.reverse(suffix);

        // Tam route: prefix + suffix (prefix.tail() ortak, tekrarlamayalım)
        List<Integer> fullRoute = new ArrayList<>(prefix.getRoute());
        // suffix'in ilk elemanı = prefix.tail(), atla
        for (int i = 1; i < suffix.size(); i++) {
            fullRoute.add(suffix.get(i));
        }

        Path p = new Path();
        p.setRoute(fullRoute);
        for (int i = 0; i < fullRoute.size() - 1; i++) {
            int u = fullRoute.get(i), v = fullRoute.get(i + 1);
            p.addEdge(u, v, graph.getEdgeWeight(u, v));
        }
        p.setLb(p.getLength());
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Subspace bölme — Yen's decomposition
    // ─────────────────────────────────────────────────────────────────────────

    private List<Subspace> divideSubspace(Subspace parentSub, Path fullPath,
                                           double ub, GraphState gs) {
        List<Integer>  route  = fullPath.getRoute();
        List<Subspace> result = new ArrayList<>();

        // parentSub'un prefix uzunluğu — ondan önceki deviation point'ler
        // zaten parent tarafından handle ediliyor
        int prefixStart = parentSub.prefix.getRoute().size() - 1;
        // prefixStart: parent prefix'in son düğümünün full path'deki indeksi
        // Bu düğümden itibaren yeni deviation point'ler oluşturuyoruz

        // Inherited excluded edges (parent'tan miras)
        Set<Long> inheritedExcluded = new HashSet<>(parentSub.excludedEdges);

        for (int i = Math.max(0, prefixStart); i < route.size() - 1; i++) {
            int devNode  = route.get(i);
            int nextNode = route.get(i + 1);

            // Prefix: (v0, v1, ..., v_i)
            Path prefix = buildPrefix(route, i);

            // Bu deviation point'ten devam eden en az bir geçerli komşu var mı kontrol et
            boolean hasValidNeighbor = false;
            Set<Long> excludedForThis = new HashSet<>(inheritedExcluded);
            excludedForThis.add(Path.edgeKey(devNode, nextNode));

            for (int nb : graph.getNeighbors(devNode)) {
                if (prefix.containsVertex(nb)) continue;
                if (excludedForThis.contains(Path.edgeKey(devNode, nb))) continue;
                hasValidNeighbor = true;
                break;
            }

            if (hasValidNeighbor) {
                Subspace ns = new Subspace(prefix, excludedForThis);
                // LB hesapla
                double lb = computeLB1(ns, gs);
                if (lb != Double.POSITIVE_INFINITY && lb <= ub) {
                    ns.prefix.setLb(lb);
                    ns.tau = lb;
                    result.add(ns);
                }
            }

            // Bu düğüm için excluded edge'i miras listesine ekle
            // (sonraki deviation point'ler de bu edge'i exclude etmeli)
            inheritedExcluded.add(Path.edgeKey(devNode, nextNode));
        }
        return result;
    }

    private Path buildPrefix(List<Integer> route, int upToIndex) {
        Path           prefix = new Path();
        List<Integer>  verts  = new ArrayList<>(route.subList(0, upToIndex + 1));
        prefix.setRoute(verts);
        for (int j = 0; j < verts.size() - 1; j++) {
            int a = verts.get(j), b = verts.get(j + 1);
            prefix.addEdge(a, b, graph.getEdgeWeight(a, b));
        }
        return prefix;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Ana algoritma — IterBound
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<Path> findPaths(int src, int dest, int k) {
        validateParameters(src, dest, k);
        numberOfPathsExplored = 0;

        // Reverse graph üzerinde SPT (heuristic için)
        Graph      revGraph = GraphUtils.reverse(graph);
        GraphState gs       = new GraphState(revGraph, dest);

        // İlk en kısa yol
        Path p0 = GraphUtils.dijkstra(graph, src, dest);
        if (p0 == null || p0.getRoute().isEmpty()) return Collections.emptyList();

        double upperBound = p0.getLength() * PATH_LEN_FACTOR;

        PriorityQueue<Entry> queue = new PriorityQueue<>();
        long seq = 0;

        // İlk subspace: p0 zaten hesaplı
        Subspace init = new Subspace(null, null);
        init.prefix.appendVertex(src);
        init.computedPath = p0;
        queue.add(new Entry(p0.getLength(), seq++, init));

        List<Path> results = new ArrayList<>(k);
        int        iters   = 0;

        while (results.size() < k && !queue.isEmpty() && iters++ < MAX_ITERATIONS) {

            Entry    e   = queue.poll();
            Subspace sub = e.sub;

            numberOfPathsExplored++;

            if (e.lb > upperBound) continue;

            // ── Path zaten hesaplanmış: feasibility kontrolü + bölme ────────
            if (sub.computedPath != null) {
                Path path = sub.computedPath;

                if (path.isFeasible(metric, threshold, results)) {
                    results.add(path);

                    // Yen's decomposition: yeni subspace'ler üret
                    for (Subspace ns : divideSubspace(sub, path, upperBound, gs)) {
                        queue.add(new Entry(ns.prefix.getLb(), seq++, ns));
                    }
                }
                continue;
            }

            // ── Path henüz yok: tau ile bounded Dijkstra ────────────────────

            // Tau hesabı (iterative bounding)
            double base = queue.isEmpty() ? e.lb : Math.max(e.lb, queue.peek().lb);
            double tau  = Math.min(ALPHA * base, upperBound);
            tau = Math.max(tau, sub.tau); // tau asla geriye gitmesin

            Path found = findWithinTau(sub, gs, tau, dest);

            if (found != null) {
                sub.computedPath = found;
                // Gerçek path uzunluğu ile tekrar kuyruğa al
                queue.add(new Entry(found.getLength(), seq++, sub));
            } else {
                sub.tau = tau;
                sub.retries++;
                // Hala headroom varsa tekrar kuyruğa; yoksa subspace'i bırak
                if (tau < upperBound && sub.retries < MAX_RETRIES) {
                    queue.add(new Entry(tau, seq++, sub));
                }
            }
        }

        return results;
    }

    public int getNumberOfPathsExplored() { return numberOfPathsExplored; }
}