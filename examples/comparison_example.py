import networkx as nx
import random
import datetime
import numpy as np
import matplotlib.pyplot as plt
import gzip
from urllib.request import urlretrieve
import os

from ..src.core.graph_utils import reverse
from ..src.algorithms import FindKSPD, FindKSPD_Yen, FindKSPD_Minus

def download_and_prepare_web_google_graph(filename_gz, filename_txt):
    url = "https://snap.stanford.edu/data/web-Google.txt.gz"
    print(f"Downloading {url}...")
    urlretrieve(url, filename_gz)
    print(f"Decompressing and filtering {filename_gz}...")
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_txt, 'wb') as f_out:
            for _ in range(4): # Skip header lines
                next(f_in)
            for line_bytes in f_in:
                f_out.write(line_bytes)
    print(f"Successfully prepared {filename_txt}")

def download_and_prepare_wikitalk_graph(filename_gz, filename_txt):
    url = "https://snap.stanford.edu/data/wiki-Talk.txt.gz"
    print(f"Downloading {url}...")
    urlretrieve(url, filename_gz)
    print(f"Decompressing and filtering {filename_gz}...")
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_txt, 'wb') as f_out:
            for _ in range(4): # Skip header lines
                next(f_in)
            for line_bytes in f_in:
                f_out.write(line_bytes)
    print(f"Successfully prepared {filename_txt}")

def download_and_prepare_roadFLA_graph(filename_gz, filename_gr):
    url = "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.FLA.gr.gz"
    print(f"Downloading {url}...")
    urlretrieve(url, filename_gz)
    print(f"Decompressing and filtering {filename_gz}...")

    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_gr, 'wb') as f_out:
            f_out.write(f_in.read())

    edges = []

    with open(filename_gr, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('c') or line.startswith('p'):
                continue

            parts = line.split()
            line_type = parts[0]

            if line_type == 'a':
                # Arc line: a u v cost
                u = int(parts[1])
                v = int(parts[2])
                cost = int(parts[3])
                edges.append((u, v, cost))
    
    print(f"Successfully prepared {filename_gr}")

def download_and_prepare_roadCOL_graph(filename_gz, filename_gr):
    url = "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.COL.gr.gz"
    print(f"Downloading {url}...")
    urlretrieve(url, filename_gz)
    print(f"Decompressing and filtering {filename_gz}...")

    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_gr, 'wb') as f_out:
            f_out.write(f_in.read())

    edges = []

    with open(filename_gr, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('c') or line.startswith('p'):
                continue

            parts = line.split()
            line_type = parts[0]

            if line_type == 'a':
                # Arc line: a u v cost
                u = int(parts[1])
                v = int(parts[2])
                cost = int(parts[3])
                edges.append((u, v, cost))
    
    print(f"Successfully prepared {filename_gr}")


def average_hop_count(result):
    if not result:
        return 0

    return sum(len(p.route)-1 for p in result) / len(result)

def run_comparison_example():
    print("--- Running Comparison Example ---")

    # Ensure sample_data directory exists
    os.makedirs('/content/sample_data', exist_ok=True)

    # --- Graph Data Preparation (from your original code) ---
    filename_gz = "/content/sample_data/web-Google.txt.gz"
    filename_txt = "/content/sample_data/web-Google.txt"
    download_and_prepare_web_google_graph(filename_gz, filename_txt)

    G = nx.DiGraph()
    with open(filename_txt) as f:
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

    # --- Algorithm Comparison (from your original code) ---
    kspd_yen_times = []
    kspd_yen_num_paths = []
    kspd_yen_all_hop_counts = []

    kspd_times = []
    kspd_num_paths = []
    kspd_all_hop_counts = []

    kspd_minus_times = []
    kspd_minus_num_paths = []
    kspd_minus_all_hop_counts = []

    k_to_find = 10
    diversity_threshold = 0.6

    for src, dest in node_pairs:
        print(f"\nComparing algorithms for SRC: {src}, DEST: {dest}")

        # FindKSPD_Yen
        start_time = datetime.datetime.now()
        algorithm_yen = FindKSPD_Yen(G, threshold=diversity_threshold)
        result_kspd_yen = algorithm_yen.find_paths(src=src, dest=dest, k=k_to_find)
        end_time = datetime.datetime.now()
        execution_time_kspd_yen = end_time - start_time

        kspd_yen_times.append(execution_time_kspd_yen.total_seconds())
        kspd_yen_num_paths.append(algorithm_yen.number_of_paths_explored)
        kspd_yen_all_hop_counts.append(average_hop_count(result_kspd_yen))
        print(f"FindKSPD_Yen: Found {len(result_kspd_yen)} paths, Explored {algorithm_yen.number_of_paths_explored} paths, Time: {execution_time_kspd_yen.total_seconds():.4f}s")

        # FindKSPD
        start_time = datetime.datetime.now()
        algorithm_kspd = FindKSPD(G, threshold=diversity_threshold)
        result_kspd = algorithm_kspd.find_paths(src=src, dest=dest, k=k_to_find)
        end_time = datetime.datetime.now()
        execution_time_kspd = end_time - start_time

        kspd_times.append(execution_time_kspd.total_seconds())
        kspd_num_paths.append(algorithm_kspd.number_of_paths_explored)
        kspd_all_hop_counts.append(average_hop_count(result_kspd))
        print(f"FindKSPD: Found {len(result_kspd)} paths, Explored {algorithm_kspd.number_of_paths_explored} paths, Time: {execution_time_kspd.total_seconds():.4f}s")

        #FindKSPD_Minus
        start_time = datetime.datetime.now()
        algorithm_kspd_minus = FindKSPD_Minus(G, threshold=diversity_threshold)
        result_kspd_minus = algorithm_kspd_minus.find_paths(src=src, dest=dest, k=k_to_find)
        end_time = datetime.datetime.now()
        execution_time_kspd_minus = end_time - start_time

        kspd_minus_times.append(execution_time_kspd_minus.total_seconds())
        kspd_minus_num_paths.append(algorithm_kspd_minus.number_of_paths_explored)
        kspd_minus_all_hop_counts.append(average_hop_count(result_kspd_minus))
        print(f"FindKSPD_Minus: Found {len(result_kspd_minus)} paths, Explored {algorithm_kspd_minus.number_of_paths_explored} paths, Time: {execution_time_kspd_minus.total_seconds():.4f}s")


    kspd_yen_avg_time = np.average(kspd_yen_times) if kspd_yen_times else 0
    kspd_yen_avg_num_paths = np.average(kspd_yen_num_paths) if kspd_yen_num_paths else 0
    kspd_yen_avg_hop_count = np.average(kspd_yen_all_hop_counts) if kspd_yen_all_hop_counts else 0

    kspd_avg_time = np.average(kspd_times) if kspd_times else 0
    kspd_avg_num_paths = np.average(kspd_num_paths) if kspd_num_paths else 0
    kspd_avg_hop_count = np.average(kspd_all_hop_counts) if kspd_all_hop_counts else 0

    kspd_minus_avg_time = np.average(kspd_minus_times) if kspd_minus_times else 0
    kspd_minus_avg_num_paths = np.average(kspd_minus_num_paths) if kspd_minus_num_paths else 0
    kspd_minus_avg_hop_count = np.average(kspd_minus_all_hop_counts) if kspd_minus_all_hop_counts else 0

    print("\n------------------ Comparison Results ------------------")
    print(f"KSPD_Yen average Times: {kspd_yen_avg_time:.4f}s")
    print(f"KSPD_Yen average number of paths explored: {kspd_yen_avg_num_paths:.2f}")
    print(f"KSPD_Yen average hop count: {kspd_yen_avg_hop_count:.2f}")
    print("\n---")
    print(f"KSPD average Times: {kspd_avg_time:.4f}s")
    print(f"KSPD average number of paths explored: {kspd_avg_num_paths:.2f}")
    print(f"KSPD average hop count: {kspd_avg_hop_count:.2f}")
    print("\n---")
    print(f"KSPD_Minus average Times: {kspd_minus_avg_time:.4f}s")
    print(f"KSPD_Minus average number of paths explored: {kspd_minus_avg_num_paths:.2f}")
    print(f"KSPD_Minus average hop count: {kspd_minus_avg_hop_count:.2f}")

    # --- Plotting Results (from your original code) ---
    graph_type = "web-google"

    # Plot Average # of Paths Explored
    algorithms_paths = {
        'FindKSPD': kspd_avg_num_paths,
        'FindKSPD_Minus': kspd_minus_avg_num_paths,
        'FindKSPD_Yen': kspd_yen_avg_num_paths
    }

    x = np.arange(1) # For a single graph_type
    width = 0.25
    multiplier = 0

    fig, ax = plt.subplots(layout='constrained')
    for attribute, measurement in algorithms_paths.items():
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

    # Plot Average Running Time
    algorithms_time = {
        'FindKSPD': kspd_avg_time,
        'FindKSPD_Minus': kspd_minus_avg_time,
        'FindKSPD_Yen': kspd_yen_avg_time
    }

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

    print("------------------------------\n")

if __name__ == "__main__":
    run_comparison_example()
