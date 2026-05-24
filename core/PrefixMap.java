package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrefixMap {
    private static class TrieNode {
        Map<Integer, TrieNode> children = new HashMap<>();
        List<Path> paths = new ArrayList<>();
    }

    private final TrieNode root;
    private int prefixCount;

    public PrefixMap() {
        this.root = new TrieNode();
        this.prefixCount = 0;
    }

    public void insert(Path path) {
        List<Integer> route = path.getRoute();
        TrieNode current = root;

        for (Integer vertex : route) {
            if (!current.children.containsKey(vertex)) {
                current.children.put(vertex, new TrieNode());
                prefixCount++;
            }
            current = current.children.get(vertex);
            current.paths.add(path);
        }
    }

    public void remove(Path path) {
        removeRecursive(root, path.getRoute(), 0, path);
    }

    private boolean removeRecursive(TrieNode current, List<Integer> route, int index, Path path) {
        if (index == route.size()) {
            return false;
        }

        Integer vertex = route.get(index);
        TrieNode next = current.children.get(vertex);
        
        if (next == null) return false;

        removeRecursive(next, route, index + 1, path);
        
        next.paths.remove(path);

        if (next.paths.isEmpty() && next.children.isEmpty()) {
            current.children.remove(vertex);
            prefixCount--;
            return true;
        }
        return false;
    }

    public List<Path> findPathsWithPrefix(List<Integer> route) {
        TrieNode current = root;
        for (Integer vertex : route) {
            current = current.children.get(vertex);
            if (current == null) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>(current.paths);
    }

    public List<Path> findPathsWithPrefix(int[] routeArray) {
        List<Integer> route = new ArrayList<>();
        for (int node : routeArray) {
            route.add(node);
        }
        return findPathsWithPrefix(route);
    }

    public boolean containsPrefix(List<Integer> route) {
        TrieNode current = root;
        for (Integer vertex : route) {
            current = current.children.get(vertex);
            if (current == null) return false;
        }
        return true;
    }

    public Set<List<Integer>> getAllPrefixes() {
        Set<List<Integer>> allPrefixes = new HashSet<>();
        collectPrefixes(root, new ArrayList<>(), allPrefixes);
        return allPrefixes;
    }

    private void collectPrefixes(TrieNode node, List<Integer> currentPath, Set<List<Integer>> results) {
        for (Map.Entry<Integer, TrieNode> entry : node.children.entrySet()) {
            List<Integer> nextPath = new ArrayList<>(currentPath);
            nextPath.add(entry.getKey());
            results.add(nextPath);
            collectPrefixes(entry.getValue(), nextPath, results);
        }
    }

    public int getPrefixCount() {
        return prefixCount;
    }

    public void clear() {
        root.children.clear();
        prefixCount = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PrefixMap (Trie) {\n");
        buildString(root, new ArrayList<>(), sb);
        sb.append("}");
        return sb.toString();
    }

    private void buildString(TrieNode node, List<Integer> currentPath, StringBuilder sb) {
        for (Map.Entry<Integer, TrieNode> entry : node.children.entrySet()) {
            List<Integer> nextPath = new ArrayList<>(currentPath);
            nextPath.add(entry.getKey());
            sb.append("  ").append(nextPath).append(" -> ").append(entry.getValue().paths.size()).append(" paths\n");
            buildString(entry.getValue(), nextPath, sb);
        }
    }

    public void printStatistics() {
        System.out.println("=== PrefixMap Statistics ===");
        System.out.println("Total nodes (prefixes): " + prefixCount);
        System.out.println(this.toString());
    }
}
