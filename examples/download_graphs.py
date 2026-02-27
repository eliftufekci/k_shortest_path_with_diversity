import gzip
import os
from urllib.request import urlretrieve
import math
import pandas as pd

def _download_and_extract(url, filename_gz, filename_out):
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

    with gzip.open(filename_gz, 'rb') as f_in:
        with open(filename_out, 'wb') as f_out:
                for line_bytes in f_in:
                    if not line_bytes.startswith(b'#'):
                        f_out.write(line_bytes)

    os.remove(filename_gz)
    print(f"Removed archive: {filename_gz}")
    print(f"Done: {filename_out}")


def _haversine(lat1, lon1, lat2, lon2):
        R = 6371
        phi1, phi2 = math.radians(lat1), math.radians(lat2)
        dphi = math.radians(lat2 - lat1)
        dlambda = math.radians(lon2 - lon1)
        a = math.sin(dphi/2)**2 + math.cos(phi1)*math.cos(phi2)*math.sin(dlambda/2)**2
        return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def _download_flights(url1, filename1, url2, filename2, filename_out):
    if os.path.exists(filename_out):
        print(f"Already exists, skipping: {filename_out}")
        return

    if not os.path.exists(filename1):
        print(f"Downloading: {url1}")
        urlretrieve(url1, filename1)
    else:
        print(f"Archive already exists, skipping download: {filename1}")


    if not os.path.exists(filename2):
        print(f"Downloading: {url2}")
        urlretrieve(url2, filename2)
    else:
        print(f"Archive already exists, skipping download: {filename2}")


    airports = pd.read_csv(filename1, header=None, # Used filename1
    names=["id","name","city","country","iata","icao","lat","lon",
           "alt","tz","dst","tz_name","type","source"])

    airport_coords = {}
    for _, row in airports.iterrows():
        try:
            airport_coords[int(row["id"])] = (float(row["lat"]), float(row["lon"]))
        except:
            pass


    routes = pd.read_csv(filename2, header=None, # Used filename2
        names=["airline","airline_id","src","src_id","dest","dest_id","codeshare","stops","equipment"])

    print(f"Generating flight routes to: {filename_out}")
    # Open the output file ONCE before the loop in write text mode
    with open(filename_out, 'w') as f_out: # Changed 'wb' to 'w' for writing strings
        for _, row in routes.iterrows():
            try:
                src_id = int(row["src_id"])
                dest_id = int(row["dest_id"])
                if src_id in airport_coords and dest_id in airport_coords:
                    lat1, lon1 = airport_coords[src_id]
                    lat2, lon2 = airport_coords[dest_id]
                    dist = _haversine(lat1, lon1, lat2, lon2)
                    if dist > 0:  # self-loop engelle
                        # Write formatted string to the file, followed by a newline
                        f_out.write(f"{src_id} {dest_id} {round(dist, 2)}\n")
            except:
                pass

    os.remove(filename1)
    print(f"Removed archive: {filename1}")

    os.remove(filename2)
    print(f"Removed archive: {filename2}")
    print(f"Done: {filename_out}") # Added completion message

def download_and_prepare_graphs():
    os.makedirs("/content/graph-data", exist_ok=True)

    """WEB GOOGLE GRAPH"""
    _download_and_extract(
        url="https://snap.stanford.edu/data/web-Google.txt.gz",
        filename_gz="/content/graph-data/web-Google.txt.gz",
        filename_out="/content/graph-data/web-Google.txt",
    )

    """WIKI TALK GRAPH"""
    _download_and_extract(
        url="https://snap.stanford.edu/data/wiki-Talk.txt.gz",
        filename_gz="/content/graph-data/wiki-Talk.txt.gz",
        filename_out="/content/graph-data/wiki-Talk.txt",
    )

    """SLASHDOT SOCIAL GRAPH"""
    _download_and_extract(
        url="https://snap.stanford.edu/data/soc-Slashdot0811.txt.gz",
        filename_gz="/content/graph-data/soc-Slashdot0811.txt.gz",
        filename_out="/content/graph-data/soc-Slashdot0811.txt",
    )

    """FLIGHTS GRAPH"""
    _download_flights(
        url1="https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat",
        filename1="/content/graph-data/airports.dat",

        url2="https://raw.githubusercontent.com/jpatokal/openflights/master/data/routes.dat",
        filename2="/content/graph-data/routes.dat",

        filename_out="/content/graph-data/flights.txt"
    )