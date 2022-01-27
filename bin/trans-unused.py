#!/usr/local/bin/python3
#coding: utf-8

"""
Print i18n keys that are used nowhere in the code.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys

from pathlib import Path
from time import time
from typing import Any, Callable, Dict, List, Iterator, Set

#############
# Constants #
#############

LILA_DIR = Path(__file__).parent.resolve().parent.absolute()
APP_DIR = LILA_DIR / "app"
MODULE_DIR = LILA_DIR / "modules"
SOURCE_DIR = LILA_DIR / "translation" / "source"
TRANS_DUMP = LILA_DIR / "bin" / "trans-dump.js"

#############
# Functions #
#############

def is_key_used(key: str) -> bool:
    key = re.escape(key)
    if key.islower(): # One word key
        key_regex = f"\.{key}|{key}[\(,\"']" # Put a dot in front to reduce change of finding the word in commments. Will be not enough for very common keys such as `performance`
    else: # Multiple word key, such as `invitationToClass`, missmatch with over variable names considered negligible
        key_regex = f"{key}"
    for dir_ in [APP_DIR, MODULE_DIR]: # Check App/ first because the majority of the translations are there
        # grep -nr '.error.max*' modules --exclude=I18nKeys.scala --exclude-dir target
        r = subprocess.run(["grep", "-n", "-r", "-q", "-E", key_regex, dir_, "--exclude=I18nKeys.scala", "--exclude-dir=target"])
        if r.returncode == 0:
            return True
    return False

def is_key_contains_number(key: str) -> bool:
    return any([i in key for i in "0123456789"])

def look_trans(file: Path) -> List[str]:
    unused_trans: List[str] = []
    with open(file) as source:
        for i, line in enumerate(source):
            try:
                key = line.partition('<string name="')[2].partition('"')[0]
            except IndexError as e:
                print(e)
                print(f"No translation key found in line {line}")
                continue
            
            if not is_key_used(key):
                unused_trans.append(key)
                print(f"Key not found: {key}")
    return unused_trans

def check() -> List[str]:
    unused_trans: List[str] = []
    for path in SOURCE_DIR.iterdir():
        if path.is_file():
            file = path.name
            print(f"Looking at {file}")
            dep = time()
            unused_trans.extend(look_trans(path))
            print(f"{file} finished in {time() -dep:2f}s")
    return unused_trans

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["check", "remove"])
    args = parser.parse_args()
    unuseds = check()
    if args.action == "check":
        print(unuseds)
        if unuseds:
            sys.exit(1)
        sys.exit(0)
    # Action == remove
    print("Removing translation keys not used in xml files...")
    for unused in unuseds:
        for path in SOURCE_DIR.iterdir():
            if not path.is_file():
                continue
            # Does not remove automatically multiple lines keys
            r = subprocess.run(["sed","-i", "", f"/{unused}.*</d", path], capture_output=True)


    r = subprocess.run(["node",TRANS_DUMP], capture_output=True)
    sys.exit(r.returncode)




########
# Main #
########

if __name__ == "__main__":
    print('#'*80)
    main()
