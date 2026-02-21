import sys
import os
import runpy


top_level_container_dir = "k_shortest_path_with_diversity/"


if top_level_container_dir not in sys.path:
    sys.path.insert(0, top_level_container_dir)


module_name = "examples.main"


init_py_path = os.path.join(top_level_container_dir, "src", "algorithms", "__init__.py")

with open(init_py_path, 'r') as f:
    content = f.read()

with open(init_py_path, 'w') as f:
    f.write(content)

os.makedirs(os.path.join(top_level_container_dir, "graph-data"), exist_ok=True)

original_cwd = os.getcwd()

try:

    os.chdir(top_level_container_dir)

    runpy.run_module(module_name, run_name="__main__", alter_sys=True)
finally:
    os.chdir(original_cwd)