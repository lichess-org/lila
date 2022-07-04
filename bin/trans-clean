#!/usr/bin/env python3

"""
Remove old translation keys (i.e. ones without matching source keys)
from `translation/dest/*` to avoid failing trans lints until
the next crowdin pull.
"""

import sys
import pathlib
import xml.etree.ElementTree as ET


def clean(path):
    db = path.parent.name  # site, study, ...
    source = ET.parse(path.parent.parent.parent / "source" / f"{db}.xml").getroot()
    tree = ET.parse(path)
    root = tree.getroot()
    change = False

    for el in list(root):
        name = el.attrib["name"]
        source_el = source.find(f".//{el.tag}[@name='{name}']")
        if source_el is None:
            root.remove(el)
            change = True
    
    if change:
        if root:
            last = root[-1]
            last.tail = last.tail.rstrip(" ")

        with open(path, "wb") as f:
            f.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
            tree.write(f, xml_declaration=False, encoding="utf-8", short_empty_elements=False)
            f.write(b"\n")
        
    return change


if __name__ == "__main__":
    args = sys.argv[1:]
    if args:
        changed = sum(clean(pathlib.Path(arg)) for arg in sys.argv[1:])
        print("Changed", changed, "files")
    else:
        print(f"Usage: {sys.argv[0]} translation/dest/*/*.xml")
