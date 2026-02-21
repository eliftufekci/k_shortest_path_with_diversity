import gc

import networkx as nx
import random
import datetime
import numpy as np

from . import draw_bar_chart
from . import draw_distribution
from src.algorithms import FindKSP, FindIterBound

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
        result = alg.find_paths(src=src, dest=dest, k=k)
        end_time = datetime.datetime.now()
        execution_time = end_time - start_time

        times.append(execution_time.total_seconds())
        num_paths.append(alg.number_of_paths_explored)
        all_hop_counts.append(average_hop_count(result))

    avg_time = np.average(times) if times else 0
    avg_num_paths = np.average(num_paths) if num_paths else 0
    avg_hop_count = np.average(all_hop_counts) if all_hop_counts else 0

    return avg_time, avg_num_paths, avg_hop_count, times, num_paths

def find_results_based_on_graph(filename, k_to_find, diversity_threshold):
    G = nx.DiGraph()
    with open(filename) as f:
        for line in f:
            parts = line.split()
            if len(parts) == 3:  # Weighted graph (3 parçalı satır: kaynak, hedef, ağırlık)
                u, v, weight = int(parts[0]), int(parts[1]), float(parts[2])
                G.add_edge(u, v, weight=weight)
            elif len(parts) == 2:  # Unweighted graph (2 parçalı satır: kaynak ve hedef)
                u, v = map(int, parts)
                G.add_edge(u, v, weight=1)  # Ağırlıksız kenarları varsayılan olarak '1' ağırlıkla ekle
            else:
                raise ValueError("Unexpected line format in graph file.")

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

    print("working with KSP")
    ksp_avg_time, ksp_avg_num_paths, ksp_avg_hop_count, ksp_times, ksp_num_paths = run_algorithm(FindKSP, G, diversity_threshold, k_to_find, node_pairs)
    print("working with Iterbound")
    iterbound_avg_time, iterbound_avg_num_paths, iterbound_avg_hop_count, iterbound_times, iterbound_num_paths = run_algorithm(FindIterBound, G, diversity_threshold, k_to_find, node_pairs)

    del G
    gc.collect()

    return (
        (ksp_avg_time, ksp_avg_num_paths, ksp_avg_hop_count, ksp_times, ksp_num_paths),
        (iterbound_avg_time, iterbound_avg_num_paths, iterbound_avg_hop_count, iterbound_times, iterbound_num_paths)
    )

def print_hop_count_table(graph_types, all_results):
    col_w = 15
    header = f"{'Graph':<{col_w}} {'FindKSP Avg Hop':>{col_w}} {'FindIterBound Avg Hop':>{col_w+5}}"
    print("\n" + "=" * len(header))
    print("Average Hop Count Table")
    print("=" * len(header))
    print(header)
    print("-" * len(header))
    for graph_name, result in zip(graph_types, all_results):
        ksp_hop  = result[0][2]
        iter_hop = result[1][2]
        print(f"{graph_name:<{col_w}} {ksp_hop:>{col_w}.4f} {iter_hop:>{col_w+5}.4f}")
    print("=" * len(header) + "\n")

def ksp_vs_iterbound():
    k_to_find = 30
    diversity_threshold = 0.6 #not important

    web_google_path = "/content/graph-data/web-Google.txt"
    wiki_talk_path  = "/content/graph-data/wiki-Talk.txt"
    roadFLA_path    = "/content/graph-data/USA-road-d.FLA.gr"
    roadCOL_path    = "/content/graph-data/USA-road-d.COL.gr"

    print("working on web-google graph")
    web_google_result = find_results_based_on_graph(web_google_path, k_to_find, diversity_threshold)
    print("working on wiki-talk graph")
    wiki_talk_result  = find_results_based_on_graph(wiki_talk_path,  k_to_find, diversity_threshold)    
    print("working on roadFLA graph")
    roadFLA_result    = find_results_based_on_graph(roadFLA_path,    k_to_find, diversity_threshold)
    print("working on roadCOL graph")
    roadCOL_result    = find_results_based_on_graph(roadCOL_path,    k_to_find, diversity_threshold)

    all_results = [web_google_result, wiki_talk_result, roadCOL_result, roadFLA_result]

    graph_types = ("web-Google", "wiki-Talk", "RoadCOL", "RoadFLA")

    algorithms_paths = {
        'FindKSP':       [r[0][1] for r in all_results],  # index 1 = avg_num_paths
        'FindIterbound': [r[1][1] for r in all_results],
    }

    algorithms_time = {
        'FindKSP':       [r[0][0] for r in all_results],  # index 0 = avg_time
        'FindIterbound': [r[1][0] for r in all_results],
    }

    draw_bar_chart(graph_types, algorithms_paths, algorithms_time)

    # Print avg_hop_count as a table
    print_hop_count_table(graph_types, all_results)

    # Collect raw data across graphs for distribution plots
    all_ksp_times = []
    all_iterbound_times = []
    all_ksp_num_paths = []
    all_iterbound_num_paths = []

    for result in all_results:
        all_ksp_times.extend(result[0][3])
        all_ksp_num_paths.extend(result[0][4])
        all_iterbound_times.extend(result[1][3])
        all_iterbound_num_paths.extend(result[1][4])

    # Plotting distributions for times
    plot_configs_time = [
        ('FindKSP Execution Times', all_ksp_times, 'skyblue'),
        ('FindIterBound Execution Times', all_iterbound_times, 'lightcoral'),
    ]
    draw_distribution(plot_configs_time)

    # Plotting distributions for number of paths
    plot_configs_num_paths = [
        ('FindKSP Number of Paths Explored', all_ksp_num_paths, 'skyblue'),
        ('FindIterBound Number of Paths Explored', all_iterbound_num_paths, 'lightcoral'),
    ]
    draw_distribution(plot_configs_num_paths)