import networkx as nx
import random
import datetime
import numpy as np

from . import draw_bar_chart
from src.core.graph_utils import reverse
from src.algorithms import FindKSPD, FindKSPD_Yen, FindKSPD_Minus

def run_algorithm(algorithm, G, threshold, k, node_pairs):
    times = []
    num_paths = []

    for src, dest in node_pairs:
        print(f"\nComparing algorithms for SRC: {src}, DEST: {dest}")

        start_time = datetime.datetime.now()
        alg = algorithm(G, threshold)
        result = alg.find_paths(src=src, dest=dest, k=k)
        end_time = datetime.datetime.now()
        execution_time = end_time - start_time

        times.append(execution_time.total_seconds())
        num_paths.append(alg.number_of_paths_explored)


    return times, num_paths

def find_results_based_on_graph(filename, k_to_find, diversity_threshold):
    G = nx.DiGraph()
    with open(filename) as f:
        for line in f:
            u, v = map(int, line.split())
            G.add_edge(u, v, weight=1)

    GR = reverse(G)

    node_pairs = []
    num_pairs = 10
    for _ in range(num_pairs):
        src = random.choice(list(G.nodes()))
        reachable = list(nx.descendants(G, src))

        while not reachable:
            src = random.choice(list(G.nodes()))
            reachable = list(nx.descendants(G, src))

        dest = random.choice(reachable)
        node_pairs.append((src, dest))

    # Get times and num_paths for each algorithm
    print("working with KSPD")
    kspd_times, kspd_num_paths = run_algorithm(FindKSPD, G, diversity_threshold, k_to_find, node_pairs)
    print("working with KSPD_Minus")
    kspd_minus_times, kspd_minus_num_paths = run_algorithm(FindKSPD_Minus, G, diversity_threshold, k_to_find, node_pairs)
    print("working with KSPD_Yen")
    kspd_yen_times, kspd_yen_num_paths = run_algorithm(FindKSPD_Yen, G, diversity_threshold, k_to_find, node_pairs)

    # Calculate averages
    kspd_yen_avg_time = np.average(kspd_yen_times) if kspd_yen_times else 0
    kspd_yen_avg_num_paths = np.average(kspd_yen_num_paths) if kspd_yen_num_paths else 0

    kspd_avg_time = np.average(kspd_times) if kspd_times else 0
    kspd_avg_num_paths = np.average(kspd_num_paths) if kspd_num_paths else 0

    kspd_minus_avg_time = np.average(kspd_minus_times) if kspd_minus_times else 0
    kspd_minus_avg_num_paths = np.average(kspd_minus_num_paths) if kspd_minus_num_paths else 0

    # Return averages and the raw data for distributions
    return (
        (kspd_avg_time, kspd_avg_num_paths, kspd_times, kspd_num_paths),
        (kspd_minus_avg_time, kspd_minus_avg_num_paths, kspd_minus_times, kspd_minus_num_paths),
        (kspd_yen_avg_time, kspd_yen_avg_num_paths, kspd_yen_times, kspd_yen_num_paths)
    )


def kspd_vs_kspd_minus_vs_kspd_yen():
    k_to_find = 10
    diversity_threshold = 0.6

    web_google_path  = "/graph-data/web-Google.txt"
    wiki_talk_path   = "/graph-data/wiki-Talk.txt"
    roadFLA_path     = "/graph-data/USA-road-d.FLA.gr"
    roadCOL_path     = "/graph-data/USA-road-d.COL.gr"

    print("working on web-google graph")
    web_google_result = find_results_based_on_graph(web_google_path, k_to_find, diversity_threshold)
    print("working on wiki-talk graph")
    wiki_talk_result  = find_results_based_on_graph(wiki_talk_path,  k_to_find, diversity_threshold)
    print("working on roadFLA graph")
    roadFLA_result    = find_results_based_on_graph(roadFLA_path,    k_to_find, diversity_threshold)
    print("working on roadCOL graph")
    roadCOL_result    = find_results_based_on_graph(roadCOL_path,    k_to_find, diversity_threshold)

    # Each result tuple: (kspd_result, kspd_minus_result, kspd_yen_result)
    # Each algorithm result: (avg_time, avg_num_paths, all_times_list, all_num_paths_list)
    all_results = [web_google_result, wiki_talk_result, roadCOL_result, roadFLA_result]

    graph_types = ("web-Google", "wiki-Talk", "RoadCOL", "RoadFLA")

    algorithms_time = {
        'FindKSPD':       [r[0][0] for r in all_results],  # index 0 = avg_time
        'FindKSPD_Minus': [r[1][0] for r in all_results],
        'FindKSPD_Yen':   [r[2][0] for r in all_results],
    }

    algorithms_paths = {
        'FindKSPD':       [r[0][1] for r in all_results],  # index 1 = avg_num_paths
        'FindKSPD_Minus': [r[1][1] for r in all_results],
        'FindKSPD_Yen':   [r[2][1] for r in all_results],
    }

    draw_bar_chart(graph_types, algorithms_paths, algorithms_time)

    all_kspd_times = []
    all_kspd_minus_times = []
    all_kspd_yen_times = []

    all_kspd_num_paths = []
    all_kspd_minus_num_paths = []
    all_kspd_yen_num_paths = []

    for graph_result in all_results:
        all_kspd_times.extend(graph_result[0][2])
        all_kspd_num_paths.extend(graph_result[0][3])

        all_kspd_minus_times.extend(graph_result[1][2])
        all_kspd_minus_num_paths.extend(graph_result[1][3])

        all_kspd_yen_times.extend(graph_result[2][2])
        all_kspd_yen_num_paths.extend(graph_result[2][3])

    # Plotting distributions for times
    plot_configs_time = [
        ('FindKSPD Execution Times', all_kspd_times, 'skyblue'),
        ('FindKSPD_Minus Execution Times', all_kspd_minus_times, 'lightcoral'),
        ('FindKSPD_Yen Execution Times', all_kspd_yen_times, 'lightgreen'),
    ]
    draw_time_distribution(plot_configs_time)

    # Plotting distributions for number of paths
    plot_configs_num_paths = [
        ('FindKSPD Number of Paths Explored', all_kspd_num_paths, 'skyblue'),
        ('FindKSPD_Minus Number of Paths Explored', all_kspd_minus_num_paths, 'lightcoral'),
        ('FindKSPD_Yen Number of Paths Explored', all_kspd_yen_num_paths, 'lightgreen'),
    ]
    draw_num_of_path_distribution(plot_configs_num_paths)
