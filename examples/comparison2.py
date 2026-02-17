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

    kspd_yen_avg_time, kspd_yen_avg_num_paths, kspd_yen_avg_hop_count = run_algorithm(FindKSPD_Yen, G, diversity_threshold, k_to_find, node_pairs)
    kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count = run_algorithm(FindKSPD, G, diversity_threshold, k_to_find, node_pairs)
    kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count = run_algorithm(FindKSPD_Minus, G, diversity_threshold, k_to_find, node_pairs)

    return (
        (kspd_avg_time, kspd_avg_num_paths, kspd_avg_hop_count),
        (kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_avg_hop_count),
        (kspd_yen_avg_time, kspd_yen_avg_num_paths, kspd_yen_avg_hop_count)
    )

def findkspd_vs_findkspd_minus_vs_findkspd_yen():
    k_to_find = 10
    diversity_threshold = 0.6

    web_google_path  = "/graph-data/web-Google.txt"
    wiki_talk_path   = "/graph-data/wiki-Talk.txt"   # DÜZELTME: wiki-talk -> wiki_talk
    roadFLA_path     = "/graph-data/USA-road-d.FLA.gr"
    roadCOL_path     = "/graph-data/USA-road-d.COL.gr"

    web_google_result = find_results_based_on_graph(web_google_path, k_to_find, diversity_threshold)
    wiki_talk_result  = find_results_based_on_graph(wiki_talk_path,  k_to_find, diversity_threshold)  # DÜZELTME
    roadFLA_result    = find_results_based_on_graph(roadFLA_path,    k_to_find, diversity_threshold)
    roadCOL_result    = find_results_based_on_graph(roadCOL_path,    k_to_find, diversity_threshold)

    # Her result tuple'ı: (kspd, kspd_minus, kspd_yen)
    # Her biri de: (avg_time, avg_num_paths, avg_hop_count)
    all_results = [web_google_result, wiki_talk_result, roadCOL_result, roadFLA_result]

    graph_types = ("web-Google", "wiki-Talk", "RoadCOL", "RoadFLA")

    # DÜZELTME: Her grafik için doğru indekslerle değerleri topla
    algorithms_paths = {
        'FindKSPD':       [r[0][1] for r in all_results],  # index 1 = avg_num_paths
        'FindKSPD_Minus': [r[1][1] for r in all_results],
        'FindKSPD_Yen':   [r[2][1] for r in all_results],
    }

    algorithms_time = {
        'FindKSPD':       [r[0][0] for r in all_results],  # index 0 = avg_time
        'FindKSPD_Minus': [r[1][0] for r in all_results],
        'FindKSPD_Yen':   [r[2][0] for r in all_results],
    }

    draw_plot(graph_types, algorithms_paths, algorithms_time)


def draw_plot(graph_types, algorithms_paths, algorithms_time):
    x = np.arange(len(graph_types))
    width = 0.25

    # --- Plot 1: Running Time ---
    fig, ax = plt.subplots(layout='constrained')
    multiplier = 0
    for attribute, measurement in algorithms_time.items():  # DÜZELTME: algorithms -> algorithms_time
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg Running Time (seconds)')
    ax.set_title('Average Running Time for Real-world Graphs')
    ax.set_xticks(x + width, graph_types)               # DÜZELTME: graph_type -> graph_types
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()

    # --- Plot 2: # of Paths Explored ---
    fig, ax = plt.subplots(layout='constrained')
    multiplier = 0
    for attribute, measurement in algorithms_paths.items():  # DÜZELTME: algorithms -> algorithms_paths
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg # of Paths Explored')
    ax.set_title('Average Paths Explored for Real-world Graphs')
    ax.set_xticks(x + width, graph_types)               # DÜZELTME: graph_type -> graph_types
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()


if __name__ == "__main__":
    findkspd_vs_findkspd_minus_vs_findkspd_yen()