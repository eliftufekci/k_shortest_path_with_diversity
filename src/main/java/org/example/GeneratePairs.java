package org.example;

import java.io.IOException;
import java.util.List;

import org.example.core.Graph;
import org.example.core.Pair;

/**
 * Tüm graflar için 20'şer pair üretip dosyaya kaydeder.
 *
 * Bu sınıf bir kez çalıştırılır, sonra tüm Comparison sınıfları
 * aynı pair dosyalarını okuyarak aynı çiftlerle çalışır.
 *
 * Pair dosyaları: graph-data/pairs/{graphLabel}.pairs
 *
 */
public class GeneratePairs {

    private static final int NUM_PAIRS = 20;

    /** Tüm grafların tanımları: [label, dosya yolu] */
    public static final String[][] ALL_GRAPHS = {
            { "web-Google", "graph-data/web-Google.txt" },
            { "RoadCOL",    "graph-data/RoadCOL.gr"     },
            { "RoadFLA",    "graph-data/RoadFLA.gr"     },
            { "WikiTalk",   "graph-data/WikiTalk.txt"   },
    };

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║     Pair Üretici — Tüm graflar için 20 çift       ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        if (!GraphLoader.ensureGraphsExist(ALL_GRAPHS)) {
            System.err.println("❌ Graf dosyaları hazırlanamadı, çıkılıyor.");
            return;
        }

        int successCount = 0;

        for (String[] entry : ALL_GRAPHS) {
            String label = entry[0];
            String path  = entry[1];

            System.out.println("\n── " + label + " ──");
            System.out.println("  Loading graph: " + path);

            try {
                Graph graph = GraphLoader.loadGraph(path);
                System.out.printf("  Nodes: %d  |  Edges: %d%n",
                        graph.getNodeCount(), graph.getEdgeCount());

                List<Pair<Integer, Integer>> pairs = GraphLoader.generatePairs(graph, NUM_PAIRS);
                System.out.println("  Generated " + pairs.size() + " pairs");

                // Çiftleri göster
                for (int i = 0; i < pairs.size(); i++) {
                    Pair<Integer, Integer> p = pairs.get(i);
                    System.out.printf("    [%2d] %d → %d%n", i + 1, p.getFirst(), p.getSecond());
                }

                // Dosyaya kaydet
                GraphLoader.savePairs(label, pairs);
                successCount++;

            } catch (IOException e) {
                System.err.println("  ❌ Error processing " + label + ": " + e.getMessage());
            }
        }

        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.printf( "║  Tamamlandı: %d/%d graf için pair üretildi          ║%n",
                successCount, ALL_GRAPHS.length);
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("\nŞimdi herhangi bir Comparison sınıfını çalıştırabilirsiniz.");
        System.out.println("Hepsi aynı pair dosyalarını okuyacak.");
    }
}