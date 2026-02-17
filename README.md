python-graph
Python implementation and comparative analysis of algorithms for finding K Shortest Paths with Diversity (KSPD) on directed graphs.
📌 About
This project implements five different path-finding algorithms on directed graph structures and compares them against each other. The algorithms are benchmarked on large-scale real-world graphs in terms of execution time, number of paths explored, and average hop count.
🧠 Algorithms
AlgorithmDescriptionFindKSPDMain algorithm for finding K diverse shortest pathsFindKSPD_MinusSimplified version of KSPDFindKSPD_YenKSPD built on top of Yen's algorithmFindKSPK shortest paths without diversity constraintsFindIterBoundIterative bound-based path finding
All algorithms accept a threshold parameter (diversity threshold, between 0 and 1). The higher the value, the more distinct the returned paths will be from each other.

🚀 Setup & Usage
Google Colab (Recommended)
This project is designed for Google Colab. To run it on Colab:

Clone the repo:

python:
!git clone https://github.com/eliftufekci/python-graph.git

Install dependencies:

python:
!pip install networkx matplotlib numpy

Download graph data and run comparisons:

python:
import sys
sys.path.insert(0, '/content/python-graph')

from examples.download_graphs import download_and_prepare_graphs
download_and_prepare_graphs()
Datasets
Comparisons are run on the following SNAP datasets:

web-Google — Web link graph
wiki-Talk — Wikipedia user interaction graph
USA-road-d.FLA — Florida road network
USA-road-d.COL — Colorado road network

💡 Basic Usage
python:
import networkx as nx
from src.algorithms import FindKSPD

# Create a directed graph
G = nx.DiGraph()
G.add_weighted_edges_from([
    (1, 2, 10),
    (2, 3, 1),
    (3, 4, 10),
    (1, 4, 25),
])

# Initialize the algorithm
algorithm = FindKSPD(G, threshold=0.5)

# Find k=3 diverse shortest paths
paths = algorithm.find_paths(src=1, dest=4, k=3)

for i, path in enumerate(paths):
    print(f"Path {i+1}: {path.route}, Length: {path.length:.2f}")
📊 Benchmark Results
Running examples/main.py compares the algorithms across the following metrics:

Execution time (seconds)
Number of paths explored
Average hop count

Results are visualized as bar and line charts.
📦 Requirements
networkx
matplotlib
numpy
