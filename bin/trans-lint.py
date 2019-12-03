#!/usr/bin/env python3

import os
import re
import sys
import xml.etree.ElementTree as ET


def lint(path):
    errors, warnings = 0, 0

    dirname = os.path.dirname(path)
    db = os.path.basename(dirname) # site, study, ...

    source = ET.parse(os.path.join(dirname, "..", "..", "source", "{}.xml".format(db))).getroot()

    root = ET.parse(path).getroot()
    for el in root:
        name = el.attrib["name"]
        if "'" in name or " " in name:
            print("ERROR:", path, "bad {} name:".format(el.tag, name))
            errors += 1
            continue

        source_el = source.find(".//{}[@name='{}']".format(el.tag, name))

        if el.tag == "string":
            errs, warns = lint_string(path, name, el.text, source_el.text)
            errors += errs
            warnings += warns
        elif el.tag == "plurals":
            for item in el:
                quantity = item.attrib["quantity"]
                allow_missing = 1 if quantity in ["zero", "one", "two"] else 0
                errs, warns = lint_string(path, "{}:{}".format(name, quantity), item.text, source_el.find("./item[@quantity='other']").text, allow_missing)
                errors += errs
                warnings += warns
        else:
            print("ERROR:", path, "bad resources tag:", el.tag)
            errors += 1

    return errors, warnings


def lint_string(path, name, dest, source, allow_missing=0):
    errs, warns = 0, 0

    placeholders = source.count("%s")
    if placeholders > 1:
        print("ERROR", path, "more than 1 %s in source:", name, source)
        errs += 1

    diff = placeholders - dest.count("%s")
    if diff > 0:
        allow_missing -= diff
        print("ERROR" if allow_missing < 0 else "WARNING", path, "missing %s:", name, dest)
        errs += 1 if allow_missing < 0 else 0
        warns += 0 if allow_missing < 0 else 1
    elif diff < 0:
        print("ERROR", path, "too many %s:", name, dest)
        errs += 1

    for placeholder in re.findall(r"%\d+\$s", source):
        if placeholder == "%1$s" and placeholder not in dest and allow_missing > 0:
            print("WARNING", path, "missing %1$s:", name, dest)
            allow_missing -= 1
            warns += 1
        elif dest.count(placeholder) < 1:
            print("ERROR", path, "missing {}:".format(placeholder), name, dest)
            errs += 1

    for placeholder in re.findall(r"%\d+\$s", dest):
        if source.count(placeholder) < 1:
            print("ERROR", path, "unexpected {}:".format(placeholder), name, dest)
            errs += 1

    for pattern in ["O-O", "SAN", "FEN", "PGN", "K, Q, R, B, N"]:
        m_source = source if pattern.isupper() else source.lower()
        m_dest = dest if pattern.isupper() else dest.lower()
        if pattern in m_source and pattern not in m_dest:
            print("NOTICE", path, "missing {}:".format(pattern), name, dest)
        #elif pattern not in m_source and pattern in m_dest:
        #    print("NOTICE", path, "unexpected {}:".format(pattern), name, dest)

    if "\n" not in source and "\n" in dest:
        print("NOTICE", path, "expected single line string:", name, dest)

    if re.match(r"\n", dest):
        print("ERROR", path, "has leading newlines:", name, dest)
        errs += 1
    elif re.match(r"\s+", dest):
        print("WARNING", path, "has leading spaces:", name, dest)
        warns += 1

    if re.search(r"\s+$", dest):
        print("WARNING", path, "has trailing spaces:", name, dest)
        warns += 1

    if re.search(r"\t", dest):
        print("WARNING", path, "has tabs:", name, dest)
        warns += 1

    if re.search(r"\n{3,}", dest):
        print("WARNING", path, "has more than one successive empty line:", name, dest)
        warns += 1

    return errs, warns


if __name__ == "__main__":
    args = sys.argv[1:]
    if args:
        errors, warnings = 0, 0
        for arg in sys.argv[1:]:
            errs, warns = lint(arg)
            errors += errs
            warnings += warns
        print("{} error(s), {} warning(s)".format(errors, warnings))
        if errors:
            sys.exit(1)
    else:
        print("Usage: {} translation/dest/*/*.xml".format(sys.argv[0]))
