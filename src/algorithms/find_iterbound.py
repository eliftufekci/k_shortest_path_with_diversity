import heapq
import networkx as nx
from typing import List, Optional, Tuple, Set

from .base import BasePathFindingAlgorithm
from ..core.data_structures import Path, GraphState
from ..core.graph_utils import dijkstra, reverse, construct_partial_spt

class Subspace:
    """
    IterBound için alt-uzay tanımı
    S = <P_{s,u}, X_u>
    - P_{s,u}: s'den u'ya giden yol (prefix)
    - X_u: u düğümünden çıkan yasaklı kenarlar
    """
    def __init__(self, path_prefix: Optional[Path] = None, excluded_edges: Optional[Set[Tuple[int, int]]] = None):
        self.path_prefix = path_prefix if path_prefix else Path()
        self.excluded_edges = excluded_edges if excluded_edges else set()
        self.computed_path: Optional[Path] = None  # En kısa yol hesaplanmışsa

    def __lt__(self, other: "Subspace") -> bool:
        """Priority queue için karşılaştırma"""
        # Eğer path hesaplanmışsa, gerçek uzunluğu kullan
        if self.computed_path:
            my_key = self.computed_path.length
        else:
            my_key = self.path_prefix.lb

        if other.computed_path:
            other_key = other.computed_path.length
        else:
            other_key = other.path_prefix.lb

        return my_key < other_key

class FindIterBound(BasePathFindingAlgorithm):
    def __init__(self, graph: nx.DiGraph, threshold: float = 0.5):
        super().__init__(graph, threshold)
        self.number_of_paths_explored = 0 # Instance variable for count

    def _compute_lower_bound(self, subspace: Subspace, graph_state: GraphState) -> float:
        """
        CompLB: Alt-uzay için alt sınır hesapla

        IterBound'daki CompLB algoritması (Algorithm 3)
        """
        u = subspace.path_prefix.tail()
        if u is None:
            return float('inf')

        lb = float('inf')
        has_valid_neighbor = False

        # u'nun her geçerli komşusu için
        for neighbor in self.graph[u]:
            # Geçerli kenar mı kontrol et
            if neighbor in subspace.path_prefix.route:  # Döngü oluşturur
                continue
            if (u, neighbor) in subspace.excluded_edges:  # Yasaklı
                continue

            has_valid_neighbor = True

            # neighbor'dan hedefe olan mesafeyi SPT'den al
            if not graph_state.isSettled[neighbor]:
                construct_partial_spt(graph_state=graph_state, v=neighbor) # Use imported function

            # Eğer neighbor'dan hedefe yol yoksa, skip
            if graph_state.distances[neighbor] == float('inf'):
                continue

            # Tahmin: prefix uzunluğu + edge weight + SPT distance
            edge_weight = self.graph[u][neighbor]['weight']
            estimate = subspace.path_prefix.length + edge_weight + graph_state.distances[neighbor]
            lb = min(lb, estimate)

        # Eğer hiç geçerli komşu yoksa veya hepsi infinity ise
        if not has_valid_neighbor or lb == float('inf'):
            return float('inf')

        return lb

    def _test_lower_bound(self, subspace: Subspace, graph_state: GraphState, tau: float, dest: int) -> Optional[Path]:
        """
        TestLB: Alt-uzayda en kısa yol tau'dan büyük mü test et

        IterBound'daki TestLB algoritması (Algorithm 5)
        Eğer shortest path <= tau ise yolu döndür, değilse None
        """
        u = subspace.path_prefix.tail()
        prefix_route = subspace.path_prefix.route
        prefix_length = subspace.path_prefix.length

        # A* benzeri arama - sadece estimated distance <= tau olanları genişlet
        distances = {u: prefix_length}
        parent = {}

        # Prefix'teki parent ilişkilerini kopyala
        for i in range(len(prefix_route) - 1):
            parent[prefix_route[i+1]] = prefix_route[i]

        # Priority queue: (estimated_distance, actual_distance, node)
        pq: List[Tuple[float, float, int]] = []

        # u'dan hedefe tahmini mesafe
        if not graph_state.isSettled[u]:
            construct_partial_spt(graph_state=graph_state, v=u) # Use imported function
        estimated = prefix_length + graph_state.distances[u]

        heapq.heappush(pq, (estimated, prefix_length, u))
        visited = set()

        while pq:
            est_dist, actual_dist, node = heapq.heappop(pq)

            if node in visited:
                continue
            visited.add(node)
            self.number_of_paths_explored += 1 # Use instance variable

            # Hedefe ulaştık mı?
            if node == dest:
                # Yolu yeniden oluştur
                path = Path()
                current = dest
                route_reversed = []

                while current is not None:
                    route_reversed.append(current)
                    current = parent.get(current)

                path.route = list(reversed(route_reversed))

                # Kenarları ve uzunluğu hesapla
                for i in range(len(path.route) - 1):
                    u_edge, v_edge = path.route[i], path.route[i+1]
                    weight = self.graph[u_edge][v_edge]['weight']
                    path.edges[(u_edge, v_edge)] = weight
                    path.length += weight

                path.lb = path.length
                return path

            # Komşuları genişlet
            for neighbor in self.graph[node]:
                # Geçerli kenar mı?
                if neighbor in prefix_route:  # Döngü
                    continue
                if (node, neighbor) in subspace.excluded_edges:  # Yasaklı
                    continue

                new_dist = actual_dist + self.graph[node][neighbor]['weight']

                # neighbor'dan hedefe tahmini mesafe (SPT kullanarak)
                if not graph_state.isSettled[neighbor]:
                    construct_partial_spt(graph_state=graph_state, v=neighbor) # Use imported function

                estimated_to_dest = new_dist + graph_state.distances[neighbor]

                # BUDAMA: Sadece tau'dan küçük tahminli düğümleri ekle
                if estimated_to_dest <= tau:
                    if neighbor not in distances or new_dist < distances[neighbor]:
                        distances[neighbor] = new_dist
                        parent[neighbor] = node
                        heapq.heappush(pq, (estimated_to_dest, new_dist, neighbor))

        # tau'dan küçük yol bulunamadı
        return None

    def _divide_subspace(self, subspace: Subspace, computed_path: Path, path_id: int) -> List[Subspace]:
        """
        Alt-uzayı böl

        Bir alt-uzayda en kısa yol bulunduktan sonra, bu alt-uzayı
        l+1 yeni alt-uzaya böl (IterBound Section 4.1)

        Her düğümde deviation yaparak yeni alt-uzaylar oluştur
        """
        new_subspaces: List[Subspace] = []

        # computed_path'teki her düğümde deviation yap
        for i in range(len(computed_path.route) - 1):  # Son düğüm hariç (hedef)
            vertex = computed_path.route[i]
            next_in_path = computed_path.route[i + 1]

            # Bu düğüme kadar olan prefix'i oluştur
            new_prefix = Path()
            new_prefix.route = computed_path.route[:i+1]

            # Prefix'in kenarlarını ve uzunluğunu hesapla
            for j in range(len(new_prefix.route) - 1):
                u, v = new_prefix.route[j], new_prefix.route[j+1]
                if (u, v) in computed_path.edges:
                    new_prefix.edges[(u, v)] = computed_path.edges[(u, v)]
                    new_prefix.length += computed_path.edges[(u, v)]

            # Bu düğümün TÜM alternatif kenarları için alt-uzay oluştur
            for neighbor in self.graph[vertex]:
                # Sadece path'te kullanılmayan kenarlara bak
                if neighbor != next_in_path and neighbor not in computed_path.route[:i+1]:
                    # Yeni alt-uzay: vertex'ten sonra next_in_path kenarını yasakla
                    # ve neighbor'a git
                    new_subspace = Subspace()
                    new_subspace.path_prefix = new_prefix.copy()

                    # neighbor'ı prefix'e ekle
                    new_subspace.path_prefix.route.append(neighbor)
                    edge_weight = self.graph[vertex][neighbor]['weight']
                    new_subspace.path_prefix.edges[(vertex, neighbor)] = edge_weight
                    new_subspace.path_prefix.length += edge_weight

                    # Path'teki kenarı yasakla
                    new_subspace.excluded_edges = set()
                    new_subspace.excluded_edges.add((vertex, next_in_path))

                    new_subspace.path_prefix.cls = (path_id, vertex)

                    new_subspaces.append(new_subspace)

        return new_subspaces

    def find_paths(
        self,
        src: int,
        dest: int,
        k: int
    ) -> List[Path]:
        """
        IterBound Algorithm (Algorithm 4)

        Top-k en kısa yolu iteratif sınır daraltma ile bul

        Args:
            src: Kaynak düğüm
            dest: Hedef düğüm
            k: Bulunacak yol sayısı

        Returns:
            result_set: k en kısa yol listesi
        """
        self.validate_parameters(src, dest, k) # Use validation from base class

        self.number_of_paths_explored = 0

        graph_reverse = reverse(self.graph) # Use imported function

        # GraphState: SPT yapısı
        graph_state = GraphState(graph_reverse=graph_reverse, destination=dest)

        # 1. İlk en kısa yolu hesapla (P0)
        P0 = dijkstra(graph=self.graph, src=src, dest=dest) # Use imported function

        if P0 is None:
            print(f"No path exists between {src} and {dest}")
            return []

        print(f"Initial shortest path found: length = {P0.length}")

        # 2. Priority queue başlat
        # Q: [(lower_bound, unique_id, subspace)]
        Q: List[Tuple[float, int, Subspace]] = []

        initial_subspace = Subspace()
        initial_subspace.path_prefix = Path()
        initial_subspace.path_prefix.route = [src]
        initial_subspace.path_prefix.length = 0
        initial_subspace.computed_path = P0

        heapq.heappush(Q, (P0.length, id(initial_subspace), initial_subspace))

        # 3. Sonuç listesi ve tau başlat
        result_set: List[Path] = []
        tau = P0.length # Use P0.length as initial tau
        i = 1  # Path counter
        max_iterations = 10000 # Hardcode max_iterations or pass as param to constructor
        iteration_count = 0  # Infinite loop koruması
        alpha = 1.1 # Hardcode alpha or pass as param to constructor

        # 4. k yol bulana kadar devam et
        while len(result_set) < k and Q and iteration_count < max_iterations:
            iteration_count += 1

            lb, _, subspace = heapq.heappop(Q)

            # Eğer lower bound infinity ise, bu alt-uzaydan yol bulunamaz
            if lb == float('inf'):
                print(f"  -> Skipping subspace with lb=inf (no path possible)")
                continue

            if subspace.computed_path is not None:
                # Bu alt-uzayın en kısa yolu bulunmuş
                path = subspace.computed_path

                # Apply diversity check using self.threshold
                if path.similarity(self.threshold, result_set): # Assuming Path has a .similarity method
                    result_set.append(path)
                    print(f"Path {len(result_set)} added to result: length = {path.length}")

                    # Alt-uzayı böl
                    new_subspaces = self._divide_subspace(subspace, path, i)
                    i += 1

                    for new_sub in new_subspaces:
                        # Her yeni alt-uzay için alt sınır hesapla
                        new_lb = self._compute_lower_bound(new_sub, graph_state)

                        # Infinity check
                        if new_lb == float('inf'):
                            continue  # Bu alt-uzayı ekleme

                        # Ensure new_lb is at least current path length for correctness
                        new_lb = max(new_lb, path.length)

                        new_sub.path_prefix.lb = new_lb
                        heapq.heappush(Q, (new_lb, id(new_sub), new_sub))
            else:
                # Alt-uzayın en kısa yolu henüz hesaplanmamış

                # tau'yu büyüt (Iterative Bounding)
                # If Q is empty, then lb must be the last element to pop, so tau should be based on it.
                # If Q is not empty, use the top_lb from Q to adjust tau more dynamically.
                if Q:
                    top_lb = Q[0][0] # peek at the smallest LB in the queue
                    tau_candidate = max(lb, top_lb) # Take the max of current LB and top LB
                    if tau_candidate == float('inf'): # If both are inf, don't multiply inf
                         tau = float('inf')
                    else:
                         tau = alpha * tau_candidate
                else: # Q is empty, only current lb left
                    if lb == float('inf'):
                        tau = float('inf')
                    else:
                        tau = alpha * lb


                print(f"Testing subspace with lb={lb:.2f}, tau={tau:.2f}")

                # TestLB: tau'dan büyük mü test et
                computed_path = self._test_lower_bound(subspace, graph_state, tau, dest)

                if computed_path is not None:
                    # En kısa yol bulundu ve tau'dan küçük
                    subspace.computed_path = computed_path
                    # Re-add to Q with its actual length for proper priority
                    heapq.heappush(Q, (computed_path.length, id(subspace), subspace))
                    print(f"  -> Path found: length = {computed_path.length}")
                else:
                    # tau'dan küçük yol yok, alt sınırı tau olarak güncelle
                    subspace.path_prefix.lb = tau
                    # Re-add to Q with updated LB
                    heapq.heappush(Q, (tau, id(subspace), subspace))
                    print(f"  -> No path <= tau, updated lb to {tau:.2f}")

        # Infinite loop check
        if iteration_count >= max_iterations:
            print(f"⚠️  Warning: Reached maximum iterations ({max_iterations})")
            print(f"   Found {len(result_set)} paths out of requested {k}")

        # Queue boşsa ama k'ya ulaşmadıysak
        if len(result_set) < k and not Q:
            print(f"⚠️  Warning: Only {len(result_set)} paths exist between {src} and {dest}")

        return result_set