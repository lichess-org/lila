#!/usr/bin/env python3

import os
import re
import sys

# Do not load C implementation, so that we can override some parser methods.
sys.modules["_elementtree"] = None
import xml.etree.ElementTree as ET


class AnnotatingParser(ET.XMLParser):
    def _start(self, tag, attrs):
        elem = super()._start(tag, attrs)
        elem.line = self.parser.CurrentLineNumber
        elem.col = self.parser.CurrentColumnNumber
        return elem


def log(level, path, el, message):
    return f"::{level} file={path},line={el.line},col={el.col}::{message}"

def error(path, el, message):
    return log("error", path, el, message)

def warning(path, el, message):
    return log("warning", path, el, message)

def notice(path, el, message):
    return log("notice", path, el, message)


def lint(path):
    errors, warnings = 0, 0

    dirname = os.path.dirname(path)
    db = os.path.basename(dirname) # site, study, ...

    source = ET.parse(os.path.join(dirname, "..", "..", "source", f"{db}.xml"), parser=AnnotatingParser()).getroot()

    root = ET.parse(path, parser=AnnotatingParser()).getroot()
    for el in root:
        name = el.attrib["name"]
        if "'" in name or " " in name:
            print(error(path, el, f"bad {el.tag} name: {name}"))
            errors += 1
            continue

        source_el = source.find(f".//{el.tag}[@name='{name}']")

        if el.tag == "string":
            errs, warns = lint_string(path, el, name, el.text, source_el.text)
            errors += errs
            warnings += warns
        elif el.tag == "plurals":
            for item in el:
                quantity = item.attrib["quantity"]
                allow_missing = 1 if quantity in ["zero", "one", "two"] else 0
                errs, warns = lint_string(path, item, "{}:{}".format(name, quantity), item.text, source_el.find("./item[@quantity='other']").text, allow_missing)
                errors += errs
                warnings += warns
        else:
            print(error(path, el, f"bad resources tag: {el.tag}"))
            errors += 1

    return errors, warnings


def lint_string(path, el, name, dest, source, allow_missing=0):
    errs, warns = 0, 0

    placeholders = source.count("%s")
    if placeholders > 1:
        print(error(path, el, f"more than 1 %s in source: {name} {source}"))
        errs += 1

    diff = placeholders - dest.count("%s")
    if diff > 0:
        allow_missing -= diff
        if allow_missing < 0:
            print(log("error", path, el, f"missing %s: {name} {dest}"))
            errs += 1
    elif diff < 0:
        print(error(path, el, f"too many %s: {name} {dest}"))
        errs += 1

    for placeholder in re.findall(r"%\d+\$s", source):
        if placeholder == "%1$s" and placeholder not in dest and allow_missing > 0:
            allow_missing -= 1
        elif dest.count(placeholder) < 1:
            print(error(path, el, f"missing {placeholder}: {name} {dest}"))
            errs += 1

    for placeholder in re.findall(r"%\d+\$s", dest):
        if source.count(placeholder) < 1:
            print(error(path, el, f"unexpected {placeholder}: {name} {dest}"))
            errs += 1

    for pattern in ["O-O", "SAN", "FEN", "PGN", "K, Q, R, B, N"]:
        m_source = source if pattern.isupper() else source.lower()
        m_dest = dest if pattern.isupper() else dest.lower()
        if pattern in m_source and pattern not in m_dest:
            print(notice(path, el, f"missing {pattern}: {name} {dest}"))
        #elif pattern not in m_source and pattern in m_dest:
        #    print(notice(path, el, f"unexpected {pattern}: {name} {dest}"))

    if "\n" not in source and "\n" in dest:
        print(notice(path, el, f"expected single line string: {name} {dest}"))

    if re.match(r"\n", dest):
        print(error(path, el, f"has leading newlines: {name} {dest}"))
        errs += 1
    elif re.match(r"\s+", dest):
        print(warning(path, el, f"has leading spaces: {name} {dest}"))
        warns += 1

    if re.search(r"\s+$", dest):
        print(warning(path, el, f"has trailing spaces: {name} {dest}"))
        warns += 1

    if re.search(r"\t", dest):
        print(warning(path, el, f"has tabs: {name} {dest}"))
        warns += 1

    if re.search(r"\n{3,}", dest):
        print(warning(path, el, f"has more than one successive empty line: {name} {dest}"))
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
        print(f"{errors} error(s), {warnings} warning(s)")
        if errors:
            sys.exit(1)
    else:
        print(f"Usage: {sys.argv[0]} translation/dest/*/*.xml")
