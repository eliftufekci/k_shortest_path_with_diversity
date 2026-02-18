import networkx as nx
import random
import datetime
import numpy as np

from . import draw_line_chart
from . import draw_distribution
from src.core.graph_utils import reverse
from src.algorithms import FindKSP, FindIterBound

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
    ksp_times, ksp_num_paths = run_algorithm(FindKSP, G, diversity_threshold, k_to_find, node_pairs)
    print("working with Iterbound")
    iterbound_times, iterbound_num_paths = run_algorithm(FindIterBound, G, diversity_threshold, k_to_find, node_pairs)

    return (
        (np.average(ksp_times) if ksp_times else 0,
         np.average(ksp_num_paths) if ksp_num_paths else 0,
         ksp_times, ksp_num_paths),
        (np.average(iterbound_times) if iterbound_times else 0,
         np.average(iterbound_num_paths) if iterbound_num_paths else 0,
         iterbound_times, iterbound_num_paths)
    )

def ksp_vs_iterbound_diff_k_values():
    k_list = [10, 20, 30, 40, 50]
    diversity_threshold = 0.6 #not important

    roadFLA_path     = "/graph-data/USA-road-d.FLA.gr"

    all_results = []

    for k_to_find in k_list:
        roadFLA_result = find_results_based_on_graph(roadFLA_path, k_to_find, diversity_threshold)
        all_results.append(roadFLA_result)

    graph_types = ("RoadFLA",)

    algorithms_paths = {
        'FindKSP':       [r[0][1] for r in all_results],  # index 1 = avg_num_paths
        'FindIterbound': [r[1][1] for r in all_results],
    }

    algorithms_time = {
        'FindKSP':       [r[0][0] for r in all_results],  # index 0 = avg_time
        'FindIterbound': [r[1][0] for r in all_results],
    }

    markers = {
        'FindKSP':       's',  # □ kare
        'FindIterbound': '^',  # △ üçgen
    }

    draw_line_chart(k_list, markers, algorithms_paths, algorithms_time, graph_name="RoadFLA")

    # Collect all raw data across k values for distribution plots
    all_ksp_times = []
    all_iterbound_times = []
    all_ksp_num_paths = []
    all_iterbound_num_paths = []

    for result in all_results:
        all_ksp_times.extend(result[0][2])
        all_ksp_num_paths.extend(result[0][3])
        all_iterbound_times.extend(result[1][2])
        all_iterbound_num_paths.extend(result[1][3])

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
