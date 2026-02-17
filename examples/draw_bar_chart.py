import matplotlib.pyplot as plt
import numpy as np

def draw_bar_chart(graph_types, algorithms_paths, algorithms_time):
    x = np.arange(len(graph_types))
    width = 0.25

    # --- Plot 1: Running Time ---
    fig, ax = plt.subplots(layout='constrained')
    multiplier = 0
    for attribute, measurement in algorithms_time.items():  
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg Running Time (seconds)')
    ax.set_title('Average Running Time for Real-world Graphs')
    ax.set_xticks(x + width, graph_types)               
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()

    # --- Plot 2: # of Paths Explored ---
    fig, ax = plt.subplots(layout='constrained')
    multiplier = 0
    for attribute, measurement in algorithms_paths.items():  
        offset = width * multiplier
        rects = ax.bar(x + offset, measurement, width, label=attribute)
        ax.bar_label(rects, padding=3)
        multiplier += 1

    ax.set_ylabel('Avg # of Paths Explored')
    ax.set_title('Average Paths Explored for Real-world Graphs')
    ax.set_xticks(x + width, graph_types)              
    ax.legend(loc='upper left', ncols=2)
    plt.yscale('symlog')
    plt.show()