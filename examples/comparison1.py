import networkx as nx
import random
import datetime
import numpy as np
import matplotlib.pyplot as plt
import os

from ..src.core.graph_utils import reverse
from ..src.algorithms import FindKSPD, FindKSPD_Yen, FindKSPD_Minus

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
        result= alg.find_paths(src=src, dest=dest, k=k_to_find)
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
            u,v = map(int, line.split())
            G.add_edge(u,v,weight=1) # Assuming unweighted graph, weight=1

    GR = reverse(G) # Reversed graph needed for FindKSPD

    # --- Node Pair Generation (from your original code) ---
    node_pairs = []
    num_pairs = 1 # Keep it small for quick example execution
    for _ in range(num_pairs):
        src = random.choice(list(G.nodes()))
        reachable = list(nx.descendants(G, src))

        while not reachable: # Ensure destination is reachable
            src = random.choice(list(G.nodes()))
            reachable = list(nx.descendants(G, src))

        dest = random.choice(reachable)
        node_pairs.append((src, dest))

    kspd_yen_avg_time, kspd_yen_avg_num_paths, kspd_yen_avg_hop_count = run_algorithm(FindKSPD_Yen, G, diversity_threshold, k_to_find, node_pairs)
    kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count = run_algorithm(FindKSPD, G, diversity_threshold, k_to_find, node_pairs)
    kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count = run_algorithm(FindKSPD_Minus, G, diversity_threshold, k_to_find, node_pairs)

    return (
        (kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count),
        (kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count),
        (kspd_yen_avg_time, kspd_yen_avg_num_paths, kspd_yen_avg_hop_count))

def run_comparison_example():
    k_to_find = 10
    diversity_threshold = 0.6

    web_google_path = "/graph-data/web-Google.txt"
    wiki-talk_path = "/graph-data/wiki-Talk.txt"
    roadFLA_path = "/graph-data/USA-road-d.FLA.gr"
    roadCOL_path = "/graph-data/USA-road-d.COL.gr"

    web_google_result = find_results_based_on_graph(web_google_path, k_to_find, diversity_threshold)
    wiki-talk_result = find_results_based_on_graph(wiki-talk_path, k_to_find, diversity_threshold)
    roadFLA_result = find_results_based_on_graph(roadFLA_path, k_to_find, diversity_threshold)
    roadCOL_result = find_results_based_on_graph(roadCOL_path, k_to_find, diversity_threshold)

    # --- Plotting Results (from your original code) ---
    graph_types = ("web-google", "wiki-Talk", "roadCol", "roadFLA")

    # Plot Average # of Paths Explored
    algorithms_paths = {
        'FindKSPD': web_google_result,
        'FindKSPD_Minus': kspd_minus_avg_num_paths,
        'FindKSPD_Yen': kspd_yen_avg_num_paths
    }

    algorithms_time = {
        'FindKSPD': kspd_avg_time,
        'FindKSPD_Minus': kspd_minus_avg_time,
        'FindKSPD_Yen': kspd_yen_avg_time
    }

    draw_plot(graph_types, algorithms_paths, algorithms_time)


def draw_plot(graph_types, algorithms_paths, algorithms_time):

    x = np.arange(len(graph_types))
    width = 0.25
    multiplier = 0

    fig, ax = plt.subplots(layout='constrained')

    for attribute, measurement in algorithms.items():
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg # of paths Explored')
    ax.set_title(f'Average Paths Explored for {graph_type}')
    ax.set_xticks(x + width / 2, [graph_type])
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()


    multiplier = 0 # Reset multiplier for the second plot
    fig, ax = plt.subplots(layout='constrained')
    for attribute, measurement in algorithms_time.items():
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg Running Time (seconds)')
    ax.set_title(f'Average Running Time for {graph_type}')
    ax.set_xticks(x + width / 2, [graph_type])
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()


if __name__ == "__main__":
    run_comparison_example()
