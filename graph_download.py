import gzip
from urllib.request import urlretrieve

url = ("https://snap.stanford.edu/data/web-Google.txt.gz")
filename_gz = "/content/sample_data/web-Google.txt.gz"
filename_txt = "/content/sample_data/web-Google.txt" # This will be the decompressed file

urlretrieve(url, filename_gz)

# Decompress the .gz file and filter lines starting with '#'
with gzip.open(filename_gz, 'rb') as f_in:
    with open(filename_txt, 'wb') as f_out:
        for line_bytes in f_in:
            # Decode bytes to string to check for '#'
            line_str = line_bytes.decode('utf-8')
            if not line_str.startswith('#'):
                # Encode back to bytes before writing
                f_out.write(line_bytes)

print(f"Successfully decompressed and filtered {filename_gz} to {filename_txt}")