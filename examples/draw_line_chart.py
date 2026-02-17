import numpy as np
import matplotlib.ticker as ticker
import matplotlib.pyplot as plt

def draw_line_chart(k_values, markers, algorithms_paths, algorithms_time, graph_name):

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))

    # ── Sol: Running Time ──────────────────────
    for label, times in algorithms_time.items():
        # saniyeyi milisaniyeye çevir
        times_ms = [t * 1000 for t in times]
        ax1.plot(k_values, times_ms,
                 label=label, color='black',
                 marker=markers[label], markersize=6, linewidth=1.8)

    ax1.set_xlabel('k', fontstyle='italic')
    ax1.set_ylabel('Running Time (ms)')
    ax1.set_xticks(k_values)
    ax1.yaxis.set_major_formatter(
        ticker.FuncFormatter(lambda x, _: f'{int(x):,}')
    )
    ax1.legend(loc='lower right', frameon=True)
    ax1.grid(axis='y', linestyle='-', linewidth=0.4, color='lightgrey')
    ax1.set_axisbelow(True)

    # ── Sağ: # of Paths ───────────────────────
    for label, paths in algorithms_paths.items():
        ax2.plot(k_values, paths,
                 label=label, color='black',
                 marker=markers[label], markersize=6, linewidth=1.8)

    ax2.set_xlabel('k', fontstyle='italic')
    ax2.set_ylabel('# of Paths')
    ax2.set_xticks(k_values)
    ax2.legend(loc='upper left', frameon=True)
    ax2.grid(axis='y', linestyle='-', linewidth=0.4, color='lightgrey')
    ax2.set_axisbelow(True)

    fig.suptitle(f'(a)  {graph_name}', fontstyle='italic', fontsize=13, y=0.02)
    plt.tight_layout(rect=[0, 0.06, 1, 1])
    plt.savefig(f'{graph_name}_varying_k.png', dpi=150, bbox_inches='tight')
    plt.show()
