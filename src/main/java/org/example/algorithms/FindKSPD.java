package org.example.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.example.core.Graph;
import org.example.core.GraphState;
import org.example.core.GraphUtils;
import org.example.core.Pair;
import org.example.core.Path;
import org.example.core.PrefixMap;
import org.example.core.SimilarityMetric;

public class FindKSPD extends BasePathFindingAlgorithm {

    private int  numberOfPathsExplored;
    private int  nextPathId;
    private long heapCounter = 0;

    public FindKSPD(Graph graph, double threshold, SimilarityMetric metric) {
        super(graph, threshold, metric);
    }

    public FindKSPD(Graph graph, double threshold) {
        this(graph, threshold, SimilarityMetric.SIM1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  findPaths
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<Path> findPaths(int src, int dest, int k) {
        validateParameters(src, dest, k);

        numberOfPathsExplored = 0;
        nextPathId            = 2;

        PrefixMap prefixMap = new PrefixMap();

        List<Path> resultSet = new ArrayList<>();

        Graph      graphReverse = GraphUtils.reverse(graph);
        GraphState graphState   = new GraphState(graphReverse, dest);

        Path shortestPath = GraphUtils.dijkstra(graph, src, dest);
        if (shortestPath == null || shortestPath.getRoute().isEmpty()) {
            return resultSet;
        }
        resultSet.add(shortestPath);
        numberOfPathsExplored++;

        PriorityQueue<LocalQueueEntry>           globalPQ        = new PriorityQueue<>(
                Comparator.comparingDouble(LocalQueueEntry::getKey)
                        .thenComparingLong(LocalQueueEntry::getId));
        Map<Integer, PriorityQueue<Path>>        localQueues     = new HashMap<>();
        Map<Pair<Integer,Integer>, Set<Integer>> coveredVertices = new HashMap<>();

        Map<PriorityQueue<Path>, Double> lqMinKey = new IdentityHashMap<>();

        generateDeviationsFromPath(shortestPath, 1, globalPQ, localQueues,
                coveredVertices, graphState, prefixMap, lqMinKey);

        while (resultSet.size() < k && !globalPQ.isEmpty()) {
            Path candidate = findNextPath(graphState, globalPQ, localQueues,
                    resultSet, dest, coveredVertices, prefixMap, lqMinKey);
            if (candidate != null) {
                if (threshold >= 1.0 - 1e-9 || candidate.isFeasible(metric, threshold, resultSet)) {
                    resultSet.add(candidate);
                }
            }
        }

        prefixMap.clear();
        return resultSet;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  findNextPath
    // ─────────────────────────────────────────────────────────────────────────

    private Path findNextPath(GraphState graphState,
                              PriorityQueue<LocalQueueEntry> globalPQ,
                              Map<Integer, PriorityQueue<Path>> localQueues,
                              List<Path> resultSet,
                              int dest,
                              Map<Pair<Integer,Integer>, Set<Integer>> coveredVertices,
                              PrefixMap prefixMap,
                              Map<PriorityQueue<Path>, Double> lqMinKey) {
        numberOfPathsExplored++;

        while (!globalPQ.isEmpty()) {

            LocalQueueEntry     entry = globalPQ.poll();
            PriorityQueue<Path> lq    = entry.getLocalQueue();

            // lqMinKey kaydını temizle (bu entry artık "consumed")
            lqMinKey.remove(lq);

            if (lq.isEmpty()) continue;

            // Lazy-deletion: globalPQ'daki entry ile LQ'nun mevcut top'u uyuşmuyor
            if (Math.abs(entry.getKey() - lq.peek().getLb()) > 1e-9) continue;

            while (!lq.isEmpty() && !lq.peek().isActive()) {
                lq.poll();
            }
            if (lq.isEmpty()) continue;

            // Eğer inactive'leri temizledikten sonra top değiştiyse LQ'yu geri ekle
            // ve bir sonraki iterasyonda doğru priority ile işle
            if (Math.abs(entry.getKey() - lq.peek().getLb()) > 1e-9) {
                offerGlobal(globalPQ, lq, lqMinKey);
                continue;
            }
            // ─────────────────────────────────────────────────────────────────

            Path currentPath = lq.poll();

            if (!lq.isEmpty()) {
                offerGlobal(globalPQ, lq, lqMinKey);
            }

            while (currentPath.tail() != dest) {

                if (threshold < 1.0 - 1e-9 && !resultSet.isEmpty()) {
                    double lb2 = currentPath.calculateLB2(metric, threshold, resultSet);

                    if (lb2 > currentPath.getLb()) {
                        currentPath.setLb(lb2);
                        adjustPath(currentPath, localQueues, nextPathId, dest, prefixMap);
                        int tail = currentPath.tail();
                        PriorityQueue<Path> tailLQ =
                                localQueues.computeIfAbsent(tail, v -> new PriorityQueue<>());
                        tailLQ.offer(currentPath);
                        offerGlobal(globalPQ, tailLQ, lqMinKey);
                        currentPath = null;
                        break;
                    }
                }

                if (!extendPath(currentPath, graphState, localQueues,
                        coveredVertices, globalPQ, prefixMap, lqMinKey)) {
                    break;
                }
            }

            if (currentPath != null && currentPath.tail() == dest) {
                Pair<Integer,Integer> cls = currentPath.getCls();
                if (cls != null) {
                    coveredVertices.remove(cls);
                }
                prefixMap.remove(currentPath);
                adjustPath(currentPath, localQueues, nextPathId++, dest, prefixMap);
                return currentPath;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  extendPath
    // ─────────────────────────────────────────────────────────────────────────

    private boolean extendPath(Path path,
                               GraphState graphState,
                               Map<Integer, PriorityQueue<Path>> localQueues,
                               Map<Pair<Integer,Integer>, Set<Integer>> coveredVertices,
                               PriorityQueue<LocalQueueEntry> globalPQ,
                               PrefixMap prefixMap,
                               Map<PriorityQueue<Path>, Double> lqMinKey) {
        int                   tail = path.tail();
        Pair<Integer,Integer> cls  = path.getCls();

        PriorityQueue<Path> tailLQ = localQueues.get(tail);
        if (tailLQ != null) {
            for (Path p : tailLQ) {
                if (p.isActive()
                        && p.getCls() != null
                        && p.getCls().equals(cls)
                        && p.getLength() > path.getLength()) {
                    p.setActive(false);
                }
            }
        }

        Integer parent = graphState.getParent(tail);

        for (int nb : graph.getNeighbors(tail)) {
            if (path.containsVertex(nb)) continue;
            if (nb == parent)            continue;

            double w      = graph.getEdgeWeight(tail, nb);
            Path   branch = path.branch(nb, w);
            branch.setLb(branch.calculateLB1(graphState));
            branch.setCls(cls);

            if (cls != null) {
                Set<Integer> covered =
                        coveredVertices.computeIfAbsent(cls, _k -> new HashSet<>());
                if (covered.contains(nb)) {
                    branch.setActive(false);
                } else {
                    covered.add(nb);
                }
            }

            PriorityQueue<Path> nbLQ =
                    localQueues.computeIfAbsent(nb, v -> new PriorityQueue<>());
            nbLQ.add(branch);
            offerGlobal(globalPQ, nbLQ, lqMinKey);
            prefixMap.insert(branch);
        }

        if (parent == null) return false;
        if (path.containsVertex(parent)) {
            prefixMap.remove(path);
            return false;
        }

        path.appendVertex(parent);
        path.addEdge(tail, parent, graph.getEdgeWeight(tail, parent));
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  adjustPath
    // ─────────────────────────────────────────────────────────────────────────

    private void adjustPath(Path path,
                            Map<Integer, PriorityQueue<Path>> localQueues,
                            int newPathId,
                            int actualDest,
                            PrefixMap prefixMap) {
        Pair<Integer,Integer> cls = path.getCls();

        for (int v : path.getRoute()) {
            PriorityQueue<Path> lq = localQueues.get(v);
            if (lq == null) continue;
            for (Path p : lq) {
                if (!p.isActive() && p.getCls() != null && p.getCls().equals(cls)) {
                    p.setActive(true);
                }
            }
        }

        if (path.tail() != actualDest) return;

        List<Integer> route = path.getRoute();
        for (int i = 1; i < route.size(); i++) {
            List<Integer> prefix = route.subList(0, i + 1);
            List<Path>    sharing = prefixMap.findPathsWithPrefix(prefix);
            if (sharing.isEmpty()) continue;
            int deviationVertex = route.get(i);
            for (Path p : sharing) {
                if (p.getRoute().size() > prefix.size()) {
                    p.setCls(newPathId, deviationVertex);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  generateDeviationsFromPath
    // ─────────────────────────────────────────────────────────────────────────

    private void generateDeviationsFromPath(Path completePath,
                                            int pathId,
                                            PriorityQueue<LocalQueueEntry> globalPQ,
                                            Map<Integer, PriorityQueue<Path>> localQueues,
                                            Map<Pair<Integer,Integer>, Set<Integer>> coveredVertices,
                                            GraphState graphState,
                                            PrefixMap prefixMap,
                                            Map<PriorityQueue<Path>, Double> lqMinKey) {
        List<Integer> route = completePath.getRoute();

        for (int idx = 0; idx < route.size() - 1; idx++) {
            int vertex     = route.get(idx);
            int nextVertex = route.get(idx + 1);

            for (int nb : graph.getNeighbors(vertex)) {
                if (nb == nextVertex) continue;

                List<Integer> prefixRoute = new ArrayList<>(route.subList(0, idx + 1));
                boolean cyclic = false;
                for (int v : prefixRoute) {
                    if (v == nb) { cyclic = true; break; }
                }
                if (cyclic) continue;

                Path dev = new Path();
                dev.setRoute(new ArrayList<>(prefixRoute));
                dev.appendVertex(nb);

                List<Integer> devRoute = dev.getRoute();
                for (int i = 0; i < devRoute.size() - 1; i++) {
                    int u = devRoute.get(i), v = devRoute.get(i + 1);
                    Double w = graph.getEdgeWeight(u, v);
                    if (w != null && w != Double.POSITIVE_INFINITY) dev.addEdge(u, v, w);
                }

                dev.setCls(pathId, vertex);
                dev.setLb(dev.calculateLB1(graphState));

                int tail = dev.tail();
                PriorityQueue<Path> lq =
                        localQueues.computeIfAbsent(tail, v -> new PriorityQueue<>());
                lq.offer(dev);
                offerGlobal(globalPQ, lq, lqMinKey);
                prefixMap.insert(dev);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  offerGlobal
    // ─────────────────────────────────────────────────────────────────────────

    private void offerGlobal(PriorityQueue<LocalQueueEntry> globalPQ,
                             PriorityQueue<Path> lq,
                             Map<PriorityQueue<Path>, Double> lqMinKey) {
        if (lq.isEmpty()) return;
        double newKey = lq.peek().getLb();
        Double curKey = lqMinKey.get(lq);
        if (curKey != null && curKey <= newKey + 1e-9) return;
        globalPQ.offer(new LocalQueueEntry(newKey, heapCounter++, lq));
        lqMinKey.put(lq, newKey);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LocalQueueEntry
    // ─────────────────────────────────────────────────────────────────────────

    private static final class LocalQueueEntry {
        private final double               key;
        private final long                 id;
        private final PriorityQueue<Path>  lq;

        LocalQueueEntry(double key, long id, PriorityQueue<Path> lq) {
            this.key = key; this.id = id; this.lq = lq;
        }
        double              getKey()        { return key; }
        long                getId()         { return id; }
        PriorityQueue<Path> getLocalQueue() { return lq; }
    }

    public int getNumberOfPathsExplored() { return numberOfPathsExplored; }
}