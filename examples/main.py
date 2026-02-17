import sys
import os

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

from .comparison1 import kspd_vs_kspd_minus_vs_kspd_yen
from .comparison2 import ksp_vs_iterbound
from .comparison3 import ksp_vs_iterbound_diff_k_values
from .comparison4 import kspd_vs_kspd_minus_diff_k_values
from .comparison5 import kspd_vs_kspd_minus_diff_t_values
from .download_graphs import download_and_prepare_graphs


if __name__ == "__main__":
    download_and_prepare_graphs()

    kspd_vs_kspd_minus_vs_kspd_yen()
    ksp_vs_iterbound()
    ksp_vs_iterbound_diff_k_values()
    kspd_vs_kspd_minus_diff_k_values()
    kspd_vs_kspd_minus_diff_t_values()
