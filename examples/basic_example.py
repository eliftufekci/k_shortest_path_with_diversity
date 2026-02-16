import networkx as nx
from src.algorithms import FindKSPD
from src.core.graph_utils import reverse

def run_basic_example():
    print("--- Running Basic Example ---")

    # Create a simple directed graph
    G = nx.DiGraph()
    edges = [
        (1, 2, 10),   # A -> B
        (2, 3, 1),    # B -> C
        (3, 4, 10),   # C -> D
        (1, 8, 20),   # A -> I
        (8, 2, 1),    # I -> B
        (2, 6, 1),    # B -> F
        (6, 7, 1),    # F -> H
        (7, 8, 1),    # H -> I
        (2, 7, 3),    # B -> H
        (7, 5, 15),   # H -> E
        (3, 5, 18),   # C -> E
        (5, 4, 1)     # E -> D
    ]
    
    # Node mapping: A=1, B=2, C=3, D=4, E=5, F=6, H=7, I=8
    G.add_weighted_edges_from(edges)

    src_node = 1
    dest_node = 4
    k_paths = 3
    diversity_threshold = 0.5

    print(f"Finding {k_paths} KSPD paths from {src_node} to {dest_node} with threshold {diversity_threshold}...")

    # Initialize the algorithm
    # Note: FindKSPD needs the graph and the reverse graph if you uncomment 'GR' below
    # For FindKSPD_Yen, only the graph is needed.
    algorithm = FindKSPD(G, threshold=diversity_threshold)
    # Or for Yen's:
    # algorithm = FindKSPD_Yen(G, threshold=diversity_threshold)

    # Find paths
    paths = algorithm.find_paths(src_node, dest_node, k_paths)

    if paths:
        print(f"Found {len(paths)} diverse paths:")
        for i, path in enumerate(paths):
            print(f"Path {i+1}: Route={path.route}, Length={path.length:.2f}")
    else:
        print("No paths found.")
    print("-----------------------------\n")

if __name__ == "__main__":
    run_basic_example()