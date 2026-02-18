import seaborn as sns
import matplotlib.pyplot as plt

def draw_time_distribution(plot_configs):
    fig, axes = plt.subplots(len(plot_configs), 1, figsize=(12, 4 * len(plot_configs)), squeeze=False)
    for i, (title, data, color) in enumerate(plot_configs):
        sb.histplot(data, kde=True, color=color, ax=axes[i][0])
        axes[i][0].set_title(title)
        axes[i][0].set_xlabel("Time (seconds)")
        axes[i][0].set_ylabel("Frequency")
    plt.tight_layout()
    plt.show()

def draw_num_of_path_distribution(plot_configs):
    fig, axes = plt.subplots(len(plot_configs), 1, figsize=(12, 4 * len(plot_configs)), squeeze=False)
    for i, (title, data, color) in enumerate(plot_configs):
        sb.histplot(data, kde=True, color=color, ax=axes[i][0])
        axes[i][0].set_title(title)
        axes[i][0].set_xlabel("Number of Paths Explored")
        axes[i][0].set_ylabel("Frequency")
    plt.tight_layout()
    plt.show()
