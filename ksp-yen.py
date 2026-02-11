import heapq
import networkx as nx
import datetime

class Path:
    """Path sınıfını kullan (aynı)"""
    def __init__(self):
        self.route = []
        self.edges = {}
        self.length = 0
        self.lb = 0
        self.cls = None
        self.isActive = True
        self.cached_intersections = {}
    
    def __lt__(self, other):
        return self.length < other.length  # Sadece length'e göre sırala
    
    def tail(self):
        return self.route[-1] if self.route else None
    
    def head(self):
        return self.route[0] if self.route else None
    
    def copy(self):
        new_path = Path()
        new_path.route = self.route.copy()
        new_path.edges = self.edges.copy()
        new_path.length = self.length
        new_path.lb = self.lb
        new_path.cls = self.cls
        return new_path
    
    def Sim(self, threshold, result_set):
        """Diversity check (aynı)"""
        for old_path in result_set:
            common_edges = set(old_path.edges.keys()).intersection(set(self.edges.keys()))
            intersection_length = sum(old_path.edges[e] for e in common_edges)
            union_length = self.length + old_path.length - intersection_length
            
            if union_length > 0:
                similarity = intersection_length / union_length
                if similarity > threshold:
                    return False
        return True


def dijkstra_simple(graph, src, dest, excluded_nodes=None):
    """
    Basit Dijkstra - excluded_nodes'ları kullanmadan
    Pure Yen's için
    """
    if src == dest:
        path = Path()
        path.route = [src]
        return path
    
    if excluded_nodes is None:
        excluded_nodes = set()
    
    heap = [(0, src, [])]
    visited = set()
    
    while heap:
        cost, node, path_list = heapq.heappop(heap)
        
        if node in visited or node in excluded_nodes:
            continue
        visited.add(node)
        
        if node == dest:
            shortest_path = Path()
            
            for i in range(len(path_list)):
                if i < len(path_list) - 1:
                    u, v = path_list[i], path_list[i+1]
                    shortest_path.edges[(u, v)] = graph[u][v]['weight']
                    shortest_path.length += graph[u][v]['weight']
                    shortest_path.route.append(u)
                else:
                    u, v = path_list[i], dest
                    shortest_path.edges[(u, v)] = graph[u][v]['weight']
                    shortest_path.length += graph[u][v]['weight']
                    shortest_path.route.append(u)
                    shortest_path.route.append(v)
            
            shortest_path.lb = shortest_path.length
            return shortest_path
        
        for neighbor, data in graph[node].items():
            if neighbor not in visited and neighbor not in excluded_nodes:
                new_cost = cost + data['weight']
                heapq.heappush(heap, (new_cost, neighbor, path_list + [node]))
    
    return None


def FindKSPD_Yen(graph, src, dest, k, threshold):
    """
    Args:
        graph: NetworkX DiGraph
        src: source node
        dest: destination node
        k: number of paths to find
        threshold: diversity threshold (τ)
    
    Returns:
        result_set: list of diverse paths
        kappa: number of evaluated paths (κ)
    """
    global number_of_paths_explored
    number_of_paths_explored = 0
    
    # 1. İlk en kısa yolu bul
    P1 = dijkstra_simple(graph, src, dest)
    
    if P1 is None:
        print(f"No path exists between {src} and {dest}")
        return [], 0
    
    print(f"KSPD-Yen: First shortest path found, length={P1.length}")
    
    # Result set (Ψ)
    result_set = [P1]
    
    # Candidate priority queue (sorted by length)
    candidates = []
    
    # 2. İlk yoldan deviation paths oluştur (Pure Yen's)
    for i in range(len(P1.route) - 1):
        spur_node = P1.route[i]
        root_path = P1.route[:i+1]
        
        # Root path'i oluştur
        root_path_obj = Path()
        root_path_obj.route = root_path.copy()
        for j in range(len(root_path) - 1):
            u, v = root_path[j], root_path[j+1]
            root_path_obj.edges[(u, v)] = graph[u][v]['weight']
            root_path_obj.length += graph[u][v]['weight']
        
        # Removed edges/nodes (Yen's deviation logic)
        removed_nodes = set()
        
        # Result set'teki yollarla aynı root path'i paylaşanları bul
        for path in result_set:
            if len(path.route) > i:
                # Aynı root path mi?
                if path.route[:i+1] == root_path:
                    # Spur node'dan sonraki düğümü kaldır
                    if i + 1 < len(path.route):
                        removed_nodes.add(path.route[i+1])
        
        # Root path'teki düğümleri (spur hariç) geçici kaldır
        excluded = set(root_path[:-1])
        
        # Spur node'dan dest'e yol bul (excluded nodes ile)
        spur_path = dijkstra_simple(graph, spur_node, dest, 
                                   excluded_nodes=excluded.union(removed_nodes))
        
        if spur_path is not None:
            # Total path = root_path + spur_path
            total_path = Path()
            total_path.route = root_path_obj.route[:-1] + spur_path.route
            total_path.edges = root_path_obj.edges.copy()
            total_path.edges.update(spur_path.edges)
            total_path.length = root_path_obj.length + spur_path.length
            total_path.lb = total_path.length
            
            # Duplicate check
            is_duplicate = False
            for existing in candidates:
                if existing.route == total_path.route:
                    is_duplicate = True
                    break
            
            if not is_duplicate:
                heapq.heappush(candidates, total_path)
    
    # 3. Ana döngü: k diverse path bulana kadar
    kappa = 1  # İlk yol zaten bulundu
    
    while len(result_set) < k and candidates:
        # En kısa candidate'ı al
        current_path = heapq.heappop(candidates)
        kappa += 1
        number_of_paths_explored += 1
        
        print(f"KSPD-Yen: Evaluating path #{kappa}, length={current_path.length}")
        
        # Diversity check
        if current_path.Sim(threshold, result_set):
            result_set.append(current_path)
            print(f"KSPD-Yen: Path added to result set (total: {len(result_set)})")
            
            # Bu yoldan yeni deviation paths oluştur
            for i in range(len(current_path.route) - 1):
                spur_node = current_path.route[i]
                root_path = current_path.route[:i+1]
                
                # Root path oluştur
                root_path_obj = Path()
                root_path_obj.route = root_path.copy()
                for j in range(len(root_path) - 1):
                    u, v = root_path[j], root_path[j+1]
                    root_path_obj.edges[(u, v)] = graph[u][v]['weight']
                    root_path_obj.length += graph[u][v]['weight']
                
                # Removed nodes
                removed_nodes = set()
                for path in result_set:
                    if len(path.route) > i and path.route[:i+1] == root_path:
                        if i + 1 < len(path.route):
                            removed_nodes.add(path.route[i+1])
                
                excluded = set(root_path[:-1])
                
                # Spur path bul
                spur_path = dijkstra_simple(graph, spur_node, dest,
                                           excluded_nodes=excluded.union(removed_nodes))
                
                if spur_path is not None:
                    # Total path
                    total_path = Path()
                    total_path.route = root_path_obj.route[:-1] + spur_path.route
                    total_path.edges = root_path_obj.edges.copy()
                    total_path.edges.update(spur_path.edges)
                    total_path.length = root_path_obj.length + spur_path.length
                    total_path.lb = total_path.length
                    
                    # Duplicate check
                    is_duplicate = False
                    for existing in candidates:
                        if existing.route == total_path.route:
                            is_duplicate = True
                            break
                    
                    for existing in result_set:
                        if existing.route == total_path.route:
                            is_duplicate = True
                            break
                    
                    if not is_duplicate:
                        heapq.heappush(candidates, total_path)
        else:
            print(f"KSPD-Yen: Path rejected (too similar)")
    
    print(f"\nKSPD-Yen: Total evaluated paths (κ): {kappa}")
    return result_set, kappa


if __name__ == "__main__":
    G = nx.DiGraph()

    
    with open("/content/sample_data/web-Google.txt") as f:
        for line in f:
            u,v = map(int, line.split())
            G.add_edge(u,v,weight=1)

    node_pairs = []

    for i in range(0,10):
        src = random.choice(list(G.nodes()))
        reachable = nx.descendants(G, src)
        
        while not reachable:
            src = random.choice(list(G.nodes()))
            reachable = nx.descendants(G, src)

        dest = random.choice(list(reachable))
        node_pairs.append((src, dest))

    
    kspd_yen_times = []
    kspd_yen_num_paths = []

    
    for src, dest in node_pairs:
        number_of_paths_explored = 0
        start_time = datetime.datetime.now()
        
        result_kspd_yen = FindKSPD_Yen(G, src=src, dest=dest, k=10, threshold=0.6)
        
        end_time = datetime.datetime.now()
        execution_time_kspd_yen = end_time - start_time

        kspd_yen_times.append(execution_time_kspd_yen)
        kspd_yen_num_paths.append(number_of_paths_explored)

        kspd_yen_hop_count = average_hop_count(result_kspd_yen)
        


    
    kspd_yen_avg_time = np.average(kspd_yen_times)
    kspd_yen_avg_num_paths = np.average(kspd_yen_num_paths)

    print(f"kspd_yen Times: {kspd_yen_times}")
    print(f"kspd_yen num of paths: {kspd_yen_num_paths}")


    print("------------------")

    print(f"kspd_yen average Times: {kspd_yen_avg_time}")
    print(f"kspd_yen average number of paths: {kspd_yen_avg_num_paths}")


    print(f"kspd_yen hop count: {np.average(kspd_yen_hop_count)}")


graph_types = ("web-google",)
algorithms = {
    'kspd_yen': kspd_yen_avg_num_paths
}

x = np.arange(len(graph_types)) 
width = 0.25  
multiplier = 0

fig, ax = plt.subplots(layout='constrained')

for attribute, measurement in algorithms.items():
    offset = width * multiplier
    rects = ax.bar(x + offset, measurement, width, label=attribute)
    ax.bar_label(rects, padding=3)
    multiplier += 1

# Add some text for labels, title and custom x-axis tick labels, etc.
ax.set_ylabel('Avg # of paths')
ax.set_xticks(x + width, graph_types)
ax.legend(loc='upper left', ncols=3)
ax.set_ylim(0, 250)

plt.show()


graph_types = ("web-google",)
algorithms = {
    'kspd_yen': kspd_yen_avg_time.total_seconds()
}

x = np.arange(len(graph_types)) 
width = 0.25  
multiplier = 0

fig, ax = plt.subplots(layout='constrained')

for attribute, measurement in algorithms.items():
    offset = width * multiplier
    rects = ax.bar(x + offset, measurement, width, label=attribute)
    ax.bar_label(rects, padding=3)
    multiplier += 1

# Add some text for labels, title and custom x-axis tick labels, etc.
ax.set_ylabel('Avg Running Time')
ax.set_xticks(x + width, graph_types)
ax.legend(loc='upper left', ncols=3)
ax.set_ylim(0, 250)

plt.show()