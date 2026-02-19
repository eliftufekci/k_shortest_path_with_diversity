import gzip
import os
from urllib.request import urlretrieve


def _download_and_extract(url, filename_gz, filename_out, skip_lines=0):
    """
    Helper: dosya zaten varsa indir ve aç işlemini atla.
    skip_lines > 0 ise ilk N satırı atarak yazar (txt formatı için).
    """
    if os.path.exists(filename_out):
        print(f"Already exists, skipping: {filename_out}")
        return

    if not os.path.exists(filename_gz):
        print(f"Downloading: {url}")
        urlretrieve(url, filename_gz)
    else:
        print(f"Archive already exists, skipping download: {filename_gz}")

    print(f"Extracting: {filename_gz} -> {filename_out}")
    is_gr_file = filename_out.endswith('.gr')

    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_out, 'wb') as f_out:
            if is_gr_file:
                for line_bytes in f_in:
                    line = line_bytes.decode('utf-8').strip()
                    if line.startswith('a'):  # Only write arc lines for .gr files
                        f_out.write(line_bytes) # Keep original newline if present, or add if stripped
            else:  # For .txt files or other generic files
                # Skip the specified number of lines from the gzipped stream
                for _ in range(skip_lines):
                    try:
                        next(f_in)
                    except StopIteration:
                        # Handle cases where the file might have fewer lines than skip_lines
                        break
                # Write the rest of the content
                for line_bytes in f_in:
                    f_out.write(line_bytes)

    os.remove(filename_gz)
    print(f"Removed archive: {filename_gz}")
    print(f"Done: {filename_out}")


def download_and_prepare_graphs():
    os.makedirs("/content/graph-data", exist_ok=True)

    """WEB GOOGLE GRAPH"""
    _download_and_extract(
        url="https://snap.stanford.edu/data/web-Google.txt.gz",
        filename_gz="/content/graph-data/web-Google.txt.gz",
        filename_out="/content/graph-data/web-Google.txt",
        skip_lines=4
    )

    """WIKI TALK GRAPH"""
    _download_and_extract(
        url="https://snap.stanford.edu/data/wiki-Talk.txt.gz",
        filename_gz="/content/graph-data/wiki-Talk.txt.gz",
        filename_out="/content/graph-data/wiki-Talk.txt",
        skip_lines=4
    )

    """ROADFLA GRAPH"""
    _download_and_extract(
        url="https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.FLA.gr.gz",
        filename_gz="/content/graph-data/USA-road-d.FLA.gr.gz",
        filename_out="/content/graph-data/USA-road-d.FLA.gr",
        skip_lines=0
    )

    """ROADCOL GRAPH"""
    _download_and_extract(
        url="https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.COL.gr.gz",
        filename_gz="/content/graph-data/USA-road-d.COL.gr.gz",
        filename_out="/content/graph-data/USA-road-d.COL.gr",
        skip_lines=0
    )