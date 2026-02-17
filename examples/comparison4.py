import networkx as nx
import random
import datetime
import numpy as np

from . import draw_line_chart
from src.core.graph_utils import reverse
from src.algorithms import FindKSPD, FindKSPD_Minus

def average_hop_count(result):
    if not result:
        return 0
    return sum(len(p.route)-1 for p in result) / len(result)

def run_algorithm(algorithm, G, threshold, k, node_pairs):
    times = []
    num_paths = []
    all_hop_counts = []

    for src, dest in node_pairs:
        print(f"\nComparing algorithms for SRC: {src}, DEST: {dest}")

        start_time = datetime.datetime.now()
        alg = algorithm(G, threshold)
        result = alg.find_paths(src=src, dest=dest, k=k)  # DÜZELTME: k_to_find -> k
        end_time = datetime.datetime.now()
        execution_time = end_time - start_time

        times.append(execution_time.total_seconds())
        num_paths.append(alg.number_of_paths_explored)
        all_hop_counts.append(average_hop_count(result))

    avg_time = np.average(times) if times else 0
    avg_num_paths = np.average(num_paths) if num_paths else 0
    avg_hop_count = np.average(all_hop_counts) if all_hop_counts else 0

    return avg_time, avg_num_paths, avg_hop_count

def find_results_based_on_graph(filename, k_to_find, diversity_threshold):
    G = nx.DiGraph()
    with open(filename) as f:
        for line in f:
            u, v = map(int, line.split())
            G.add_edge(u, v, weight=1)

    GR = reverse(G)

    node_pairs = []
    num_pairs = 1
    for _ in range(num_pairs):
        src = random.choice(list(G.nodes()))
        reachable = list(nx.descendants(G, src))

        while not reachable:
            src = random.choice(list(G.nodes()))
            reachable = list(nx.descendants(G, src))

        dest = random.choice(reachable)
        node_pairs.append((src, dest))

    kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count = run_algorithm(FindKSPD, G, diversity_threshold, k_to_find, node_pairs)
    kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count = run_algorithm(FindKSPD_Minus, G, diversity_threshold, k_to_find, node_pairs)

    return (
        (kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count),
        (kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count)
    )

def kspd_vs_kspd_minus_diff_k_values():
    k_list = [5, 10, 15, 20]
    diversity_threshold = 0.6 #not important

    roadFLA_path     = "/graph-data/USA-road-d.FLA.gr"

    all_results = []

    for k_to_find in k_list:
        roadFLA_result = find_results_based_on_graph(roadFLA_path, k_to_find, diversity_threshold)
        all_results.append(roadFLA_result)

    graph_types = ("RoadFLA",)

    # DÜZELTME: Her grafik için doğru indekslerle değerleri topla
    algorithms_paths = {
        'FindKSPD':       [r[0][1] for r in all_results],  # index 1 = avg_num_paths
        'FindKSPD_Minus': [r[1][1] for r in all_results],
    }

    algorithms_time = {
        'FindKSPD':       [r[0][0] for r in all_results],  # index 0 = avg_time
        'FindKSPD_Minus': [r[1][0] for r in all_results],
    }

    markers = {
        'FindKSPD':       's',  # □ kare
        'FindKSPD_Minus': '^',  # △ üçgen
    }

    draw_line_chart(k_list, markers, algorithms_paths, algorithms_time, graph_name="RoadFLA")