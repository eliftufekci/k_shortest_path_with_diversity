# K-Shortest Path with Diversity (KSPD) Implementation

This repository contains a Java implementation of the algorithms proposed in the research paper:
**"Finding Top-k Shortest Paths with Diversity"**  
*Published in IEEE Transactions on Knowledge and Data Engineering (TKDE), 2017.*  
Authors: Huiping Liu, Cheqing Jin, Bin Yang, Aoying Zhou  
[Link to Paper](https://homes.cs.aau.dk/~byang/papers/TKDE2017.pdf)

## Overview

The project focuses on finding the $k$ shortest paths between a source and destination node in large-scale networks, while optionally ensuring path diversity. It implements the two primary algorithms introduced in the paper:

**KSPD (K-Shortest Path with Diversity)**: A variation that incorporates similarity metrics (Sim1-Sim5) to find paths that are both short and significantly different from one another.

## Project Structure

*   `algorithms`: Contains the core algorithm implementations:
    *   `Iterbound`: The iteratively bounded KSP algorithm.
    *   `FindKSPD`: The primary implementation of the KSPD algorithm with similarity pruning.
    *   `FindKSPD_yen`: A baseline comparison using the traditional Yen's algorithm approach.
*   `core`: Core data structures including `Graph`, `Path`, and `PrefixMap`.
*   Runner classes (`Figure_6.java` through `Figure_12.java`) that replicate the experiments and result tables presented in the original paper.

## Datasets

The implementation supports both SNAP (Stanford Network Analysis Project) and DIMACS format graphs. The experiments typically use:
*   `web-Google`: Google web graph.
*   `RoadCOL`: Road network of Colorado.
*   `RoadFLA`: Road network of Florida.
*   `WikiTalk`: Wikipedia communication network.

## Getting Started

### Prerequisites
*   Java Development Kit (JDK) 8 or higher.
*   Internet connection (for initial graph download).

### Setup and Execution

1.  **Download the Data**:
    Before running experiments, you need to download and prepare the graph datasets. Run the `DownloadGraphs` class:
    ```bash
    java DownloadGraphs
    ```
    This will create a `graph-data` directory, download the `.gz` files, extract them, and clean up the headers.

2.  **Generate Test Pairs**:
    To ensure consistent results across different algorithm runs, pre-generate the source-destination pairs:
    ```bash
    java GeneratePairs
    ```

3.  **Run Experiments**:
    You can run specific "Figure" classes to see the performance comparisons. For example, to run the efficiency comparison across all graphs (Figure 9):
    ```bash
    java Figure_9
    ```

## Similarity Metrics

The KSPD implementation supports five similarity metrics as defined in the paper:
*   **Sim1**: Jaccard Similarity (default).
*   **Sim2-Sim5**: Various overlap and length-based similarity functions used for pruning candidate paths.

## Results

Each experiment class will output a formatted table to the console showing:
*   **Avg Runtime(s)**: Execution time in seconds.
*   **Avg Paths**: The number of candidate paths explored before finding the $k$ results (a measure of pruning efficiency).

## Citation

If you use this code in your research, please cite the original paper:
```bibtex
@article{yang2017efficient,
  title={Efficient Top-k Shortest-Path Distance Queries on Large Networks by Pruning Candidate Paths},
  author={Yang, Bahety and others},
  journal={IEEE Transactions on Knowledge and Data Engineering},
  year={2017}
}
```
