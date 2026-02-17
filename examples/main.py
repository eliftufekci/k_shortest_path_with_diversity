from examples.comparison1 import kspd_vs_kspd_minus_vs_kspd_yen
from examples.comparison2 import ksp_vs_iterbound
from examples.comparison3 import ksp_vs_iterbound_diff_k_values
from examples.comparison4 import kspd_vs_kspd_minus_diff_k_values
from examples.comparison5 import kspd_vs_kspd_minus_diff_t_values
from examples.dowload_graphs import download_and_prepare_graphs


if __name__ == "__main__":
    download_and_prepare_graphs()

    kspd_vs_kspd_minus_vs_kspd_yen()
    ksp_vs_iterbound()
    ksp_vs_iterbound_diff_k_values()
    kspd_vs_kspd_minus_diff_k_values()
    kspd_vs_kspd_minus_diff_t_values()



