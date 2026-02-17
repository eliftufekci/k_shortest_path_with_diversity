import gzip
from urllib.request import urlretrieve


def download_and_prepare_graphs():
    """WEB GOOGLE GRAPH"""
    url = ("https://snap.stanford.edu/data/web-Google.txt.gz")
    filename_gz = "graph-data/web-Google.txt.gz"
    filename_txt = "graph-data/web-Google.txt"

    urlretrieve(url, filename_gz)

    # Decompress the .gz file and filter lines starting with '#'
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_txt, 'wb') as f_out:
            # Skip the first 4 lines
            for _ in range(4):
                next(f_in)  # Read and discard a line

            # Write the remaining lines to the output file
            for line_bytes in f_in:
                f_out.write(line_bytes)

    print(f"Successfully decompressed and filtered {filename_gz} to {filename_txt}")

    """WIKI TALK  GRAPH"""
    url = ("https://snap.stanford.edu/data/wiki-Talk.txt.gz")
    filename_gz = "graph-data/wiki-Talk.txt.gz"
    filename_txt = "graph-data/wiki-Talk.txt"  # This will be the decompressed file

    urlretrieve(url, filename_gz)

    # Decompress the .gz file and skip the first 4 lines
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_txt, 'wb') as f_out:
            # Skip the first 4 lines
            for _ in range(4):
                next(f_in)  # Read and discard a line

            # Write the remaining lines to the output file
            for line_bytes in f_in:
                f_out.write(line_bytes)

    print(f"Successfully decompressed and filtered (skipped first 4 lines) {filename_gz} to {filename_txt}")

    """ROADFLA GRAPH"""
    url = "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.FLA.gr.gz"
    filename_gz = "graph-data/USA-road-d.FLA.gr.gz"
    filename_gr = "graph-data/USA-road-d.FLA.gr"

    urlretrieve(url, filename_gz)

    # Decompress the .gz file
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_gr, 'wb') as f_out:
            f_out.write(f_in.read())

    edges = []

    with open(filename_gr, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('c') or line.startswith('p'):  # Skip empty lines and comment lines
                continue

            parts = line.split()
            line_type = parts[0]

            if line_type == 'a':
                # Arc line: a u v cost
                u = int(parts[1])
                v = int(parts[2])
                cost = int(parts[3])
                edges.append((u, v, cost))

    print(f"Successfully decompressed and filtered (skipped first 4 lines) {filename_gz} to {filename_gr}")

    """ROADCOL GRAPH"""
    url = "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.COL.gr.gz"
    filename_gz = "graph-data/USA-road-d.COL.gr.gz"
    filename_gr = "graph-data/USA-road-d.COL.gr"

    urlretrieve(url, filename_gz)

    # Decompress the .gz file
    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_gr, 'wb') as f_out:
            f_out.write(f_in.read())

    edges = []

    with open(filename_gr, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('c') or line.startswith('p'):  # Skip empty lines and comment lines
                continue

            parts = line.split()
            line_type = parts[0]

            if line_type == 'a':
                # Arc line: a u v cost
                u = int(parts[1])
                v = int(parts[2])
                cost = int(parts[3])
                edges.append((u, v, cost))

    print(f"Successfully decompressed and filtered (skipped first 4 lines) {filename_gz} to {filename_gr}")

