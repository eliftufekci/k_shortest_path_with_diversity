from typing import List, Optional
import heapq
import networkx as nx

from .base import BasePathFindingAlgorithm
from ..core.data_structures import Path

class FindKSPD_Yen(BasePathFindingAlgorithm):
    def __init__(self, graph: nx.DiGraph, threshold: float = 0.5):
        super().__init__(graph, threshold)
        self.number_of_paths_explored = 0

    def _dijkstra_simple(self, src: int, dest: int, excluded_nodes: Optional[Set[int]] = None, excluded_edges: Optional[Set[Tuple[int, int]]] = None) -> Optional[Path]:
        """
        Yen's algorithm için Dijkstra.
        excluded_nodes: bu node'lardan geçme
        excluded_edges: bu edge'leri kullanma (set of (u,v) tuples)
        """
        if src == dest:
            path = Path()
            path.route = [src]
            return path

        if excluded_nodes is None:
            excluded_nodes = set()
        if excluded_edges is None:
            excluded_edges = set()

        distances = {node: float('inf') for node in self.graph.nodes()}
        distances[src] = 0
        previous_nodes = {node: None for node in self.graph.nodes()}
        heap = [(0, src)]

        while heap:
            cost, node = heapq.heappop(heap)

            if node == dest:
                break

            if cost > distances[node]:
                continue
            if node in excluded_nodes:
                continue

            for neighbor, data in self.graph[node].items():
                if neighbor in excluded_nodes:
                    continue
                if (node, neighbor) in excluded_edges:
                    continue
                new_cost = cost + data['weight']
                if new_cost < distances[neighbor]:
                    distances[neighbor] = new_cost
                    previous_nodes[neighbor] = node
                    heapq.heappush(heap, (new_cost, neighbor))

        if distances[dest] == float('inf'):
            return None

        path_route = []
        current = dest
        while current is not None:
            path_route.insert(0, current)
            current = previous_nodes[current]

        if path_route[0] != src:
            return None

        shortest_path = Path()
        shortest_path.route = path_route
        for i in range(len(path_route) - 1):
            u, v = path_route[i], path_route[i+1]
            shortest_path.edges[(u, v)] = self.graph[u][v]['weight']
            shortest_path.length += self.graph[u][v]['weight']
        shortest_path.lb = shortest_path.length

        return shortest_path


    def find_paths(
        self,
        src: int,
        dest: int,
        k: int
    ) -> List[Path]:
        """
        Yen tabanlı KSPD — düzeltilmiş sayaç ve O(1) duplicate kontrolü.

        number_of_paths_explored tanımı (makale ile uyumlu):
            candidates heap'inden her POP edilen path = 1 keşif.
            Yani Sim filtresine giren her candidate sayılır.
            Bu KSPD'deki FindNextPath sayacıyla simetrik.

        Diğer düzeltmeler:
            - seen_routes (set) ile O(1) duplicate kontrolü
            - Klasik Yen akışı: sadece pop edilen path'in spur'ları işlenir
            - excluded_edges kullanımı (node silmek yerine edge silmek)
        """
        self.validate_parameters(src, dest, k)

        self.number_of_paths_explored = 0

        P1 = self._dijkstra_simple(src, dest)
        if P1 is None:
            return []

        result_set = [P1]

        # Yen'in A listesi: candidates'dan pop edilen + ilk path
        # (diversity filtresinden bağımsız olarak her pop buraya girer)
        accepted_paths = [P1]

        # O(1) duplicate kontrolü: candidates'a eklenmiş veya zaten
        # accepted olan tüm route'ların tuple hali
        seen_routes = {tuple(P1.route)}
        candidates = []  # min-heap

        def generate_spurs(base_path):
            """base_path'in her spur node'undan yeni candidate'lar üret."""

            for i in range(len(base_path.route) - 1):
                spur_node = base_path.route[i]
                root_route = base_path.route[:i + 1]

                # Root path nesnesini oluştur
                root_path_obj = Path()
                root_path_obj.route = root_route.copy()
                for j in range(len(root_route) - 1):
                    u, v = root_route[j], root_route[j + 1]
                    root_path_obj.edges[(u, v)] = self.graph[u][v]['weight']
                    root_path_obj.length += self.graph[u][v]['weight']

                # Aynı root prefix'e sahip accepted path'lerde
                # spur_node'dan çıkan edge'leri yasakla (Yen kuralı)
                excluded_edges = set()
                for path in accepted_paths:
                    if (len(path.route) > i and
                            path.route[:i + 1] == root_route and
                            i + 1 < len(path.route)):
                        excluded_edges.add((path.route[i], path.route[i + 1]))

                # Root prefix'teki node'ları (spur_node hariç) yasakla
                excluded_nodes = set(root_route[:-1])

                spur_path = self._dijkstra_simple(
                    spur_node, dest,
                    excluded_nodes=excluded_nodes,
                    excluded_edges=excluded_edges
                )

                if spur_path is None:
                    continue

                self.number_of_paths_explored += 1
                total_route = root_path_obj.route[:-1] + spur_path.route
                route_key = tuple(total_route)

                if route_key in seen_routes:
                    continue  # O(1) duplicate kontrolü

                seen_routes.add(route_key)

                total_path = Path()
                total_path.route = total_route
                total_path.edges = root_path_obj.edges.copy()
                total_path.edges.update(spur_path.edges)
                total_path.length = root_path_obj.length + spur_path.length
                total_path.lb = total_path.length

                heapq.heappush(candidates, (total_path.lb, total_path))

        # İlk path'ten spur'ları üret
        generate_spurs(P1)

        while len(result_set) < k and candidates:
            _, current_path = heapq.heappop(candidates)

            accepted_paths.append(current_path)
            generate_spurs(current_path)

            # Diversity kontrolü — sadece result_set eklemesi için
            if current_path.similarity(self.threshold, result_set):
                result_set.append(current_path)
                print(f"KSPD-Yen: result_set={len(result_set)}, "
                    f"explored={self.number_of_paths_explored}")

        return result_set