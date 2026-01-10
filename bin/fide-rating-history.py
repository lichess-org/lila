import requests
from requests.adapters import HTTPAdapter
from urllib3.util import Retry
import re
import zipfile
import io
import json
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

# --- CONFIGURATION ---
START_DATE = "2000-01-01"
END_DATE   = "2099-01-01"

OUTPUT_FILE = "fide_history.jsonl"
LIST_URL = "https://ratings.fide.com/download_lists.phtml"
DOWNLOAD_ENDPOINT = "https://ratings.fide.com/a_download.php"

# Data store: players[fide_id][rating_type] = list of [date_str, rating]
players = defaultdict(lambda: {"standard": [], "rapid": [], "blitz": []})

# define the retry strategy
retry_strategy = Retry(
    total=10,  # maximum number of retries
    backoff_factor=2,
    status_forcelist=[ 403, 429, 500, 502, 503, 504 ],
)
adapter = HTTPAdapter(max_retries=retry_strategy)
session = requests.Session()
session.mount("https://", adapter)

def get_periods():
    """Scrapes available YYYY-MM-DD periods from the FIDE dropdown and filters by range."""
    print(f"Fetching period list from {LIST_URL}...")
    try:
        resp = session.get(LIST_URL)
        resp.raise_for_status()
    except Exception as e:
        sys.exit(f"Failed to fetch period list: {e}")

    # Regex matches: <option value="2021-05-01">
    period_pattern = re.compile(r'<option value="(\d{4}-\d{2}-\d{2})">')

    found = set()
    for match in period_pattern.finditer(resp.text):
        date_str = match.group(1)
        # Lexicographical string comparison works for ISO dates (YYYY-MM-DD)
        if START_DATE <= date_str <= END_DATE:
            found.add(date_str)

    return sorted(list(found))

def get_xml_links(period):
    """Fetches the download page for a period and finds XML zip links."""
    try:
        resp = session.get(DOWNLOAD_ENDPOINT, params={'period': period})
        resp.raise_for_status()
    except Exception as e:
        print(f"[{period}] Request failed: {e}")
        return []

    # Regex matches: href="...zip"> XML </a>
    link_pattern = re.compile(
        r"href=['\"]?(http://ratings\.fide\.com/download/([a-zA-Z0-9_]+\.zip))['\"]?[^>]*>\s*XML\s*</a>",
        re.IGNORECASE
    )

    files = []
    for match in link_pattern.finditer(resp.text):
        url, filename = match.group(1), match.group(2).lower()

        rtype = None
        if "standard" in filename: rtype = "standard"
        elif "rapid" in filename: rtype = "rapid"
        elif "blitz" in filename: rtype = "blitz"

        if rtype:
            files.append((url, rtype))

    return files

def parse_xml(content, rtype, date_key):
    """Parses FIDE XML content."""
    count = 0
    try:
        root = ET.fromstring(content)
        for player in root.findall('player'):
            fid_node = player.find('fideid')
            if fid_node is None:
                fid_node = player.find('fide_id')

            rating_node = player.find('rating')

            if fid_node is not None and rating_node is not None and rating_node.text:
                try:
                    fid = int(fid_node.text)
                    rating = int(rating_node.text)
                    if rating > 0:
                        players[fid][rtype].append([date_key, rating])
                        count += 1
                except ValueError:
                    continue
    except ET.ParseError:
        print(f"    -> XML Parse Error (malformed file)")
    return count

def main():
    print(f"Running backfill for range: {START_DATE} to {END_DATE}")
    periods = get_periods()

    if not periods:
        print("No periods found in that range. Check your dates.")
        return

    print(f"Found {len(periods)} periods to process.")

    for period in periods:
        date_key = period[:7] # YYYY-MM
        print(f"[{date_key}] Fetching XML links...")

        links = get_xml_links(period)
        if not links:
            print(f"    -> No XML links found.")
            continue

        for url, rtype in links:
            print(f"    -> Downloading {rtype.upper()}...")
            try:
                r = session.get(url)
                z = zipfile.ZipFile(io.BytesIO(r.content))

                found_xml = False
                for name in z.namelist():
                    if name.lower().endswith('.xml'):
                        with z.open(name) as f:
                            count = parse_xml(f.read(), rtype, date_key)
                            print(f"       Parsed {count} records.")
                            found_xml = True
                        break

                if not found_xml:
                    print("       Warning: ZIP downloaded but no .xml file inside.")

            except Exception as e:
                print(f"       Error processing {url}: {e}")

    print("Sorting and saving output...")
    with open(OUTPUT_FILE, 'w') as f:
        for fid, data in players.items():
            doc = {
                "_id": fid,
                "standard": sorted(data['standard']),
                "rapid": sorted(data['rapid']),
                "blitz": sorted(data['blitz'])
            }
            f.write(json.dumps(doc) + "\n")

    print(f"Done! Saved {len(players)} players to {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
