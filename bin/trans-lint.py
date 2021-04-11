#!/usr/bin/env python3

import os
import re
import sys
import pathlib
import urllib

# Do not load C implementation, so that we can override some parser methods.
sys.modules["_elementtree"] = None
import xml.etree.ElementTree as ET


class AnnotatingParser(ET.XMLParser):
    def _start(self, tag, attrs):
        elem = super()._start(tag, attrs)
        elem.line = self.parser.CurrentLineNumber
        elem.col = self.parser.CurrentColumnNumber
        return elem


class Report:
    def __init__(self):
        self.errors = 0
        self.warnings = 0


def short_lang(lang):
    if lang in ["ne-NP", "la-LA", "nn-NO", "zh-CN", "ur-PK", "zh-TW", "tlh-AA"]:
        return lang.replace("-", "").lower()
    elif lang == "kab-DZ":
        return "kaby"
    else:
        return lang.split("-")[0]


def western_punctuation(lang):
    return lang not in [
        "zh-TW", "zh-CN", "hi-IN", "ja-JP", "bn-BD", "ar-SA", "th-TH", "ne-NP",
        "ko-KR", "ur-PK", "hy-AM", "ml-IN", "ka-GE", "he-IL", "jbo-EN",
        "fa-IR", "or-IN", "el-GR", "ur-PK", "uk-UA", "my-MM", "ckb-IR",
        "ta-IN", "sr-SP", "pa-IN", "mn-MN", "mk-MK",
    ]


def ends_with_punctuation(text):
    return any(text.rstrip().endswith(char) for char in ".!?")


def crowdin_q(text):
    return urllib.parse.quote((text or "").replace("\\\"", "\""))


class ReportContext:
    def __init__(self, report, path, el, name, text):
        self.report = report
        self.path = path
        self.el = el
        self.name = name
        self.text = text

    def lang(self):
        return self.path.stem

    def log(self, level, message):
        if level == "error":
            self.report.errors += 1
        elif level == "warning":
            self.report.warnings += 1
        lang = short_lang(self.lang())
        url = f"https://crowdin.com/translate/lichess/all/en-{lang}#q={crowdin_q(self.text)}"
        print(f"::{level} file={self.path},line={self.el.line},col={self.el.col}::{message} ({self.name}): {self.text!r} @ {url}")

    def error(self, message):
        self.log("error", message)

    def warning(self, message):
        self.log("warning", message)

    def notice(self, message):
        self.log("notice", message)


def lint(report, path):
    db = path.parent.name # site, study, ...
    source = ET.parse(path.parent.parent.parent / "source" / f"{db}.xml", parser=AnnotatingParser()).getroot()

    root = ET.parse(path, parser=AnnotatingParser()).getroot()
    for el in root:
        name = el.attrib["name"]
        ctx = ReportContext(report, path, el, name, el.text)
        if "'" in name or " " in name:
            ctx.error(f"bad {el.tag} name")
            continue

        source_el = source.find(f".//{el.tag}[@name='{name}']")
        if source_el is None:
            ctx.error(f"did not find source element for {el.tag}")
        elif el.tag == "string":
            lint_string(ctx, el.text, source_el.text)
        elif el.tag == "plurals":
            for item in el:
                quantity = item.attrib["quantity"]
                allow_missing = 1 if quantity in ["zero", "one", "two"] else 0
                plural_name = f"{name}:{quantity}"
                lint_string(ReportContext(report, path, item, plural_name, item.text), item.text, source_el.find("./item[@quantity='other']").text, allow_missing)
        else:
            ctx.error(f"bad resources tag: {el.tag}")


def lint_string(ctx, dest, source, allow_missing=0):
    if not dest:
        ctx.error("empty translation")
        return

    placeholders = source.count("%s")
    if placeholders > 1:
        ctx.error("more than 1 %s in source")

    diff = placeholders - dest.count("%s")
    if diff > 0:
        allow_missing -= diff
        if allow_missing < 0:
            ctx.log("error", "missing %s")
    elif diff < 0:
        ctx.error("too many %s")

    for placeholder in re.findall(r"%\d+\$s", source):
        if placeholder == "%1$s" and placeholder not in dest and allow_missing > 0:
            allow_missing -= 1
        elif dest.count(placeholder) < 1:
            ctx.error(f"missing {placeholder}")

    for placeholder in re.findall(r"%\d+\$s", dest):
        if source.count(placeholder) < 1:
            ctx.error(f"unexpected {placeholder}")

    if "%s" in dest:
        for placeholder in re.findall(r"%\d+\$s", dest):
            ctx.error(f"mixing placeholder styles: {placeholder} and %s")

    for pattern in ["O-O", "SAN", "FEN", "PGN", "K, Q, R, B, N"]:
        m_source = source if pattern.isupper() else source.lower()
        m_dest = dest if pattern.isupper() else dest.lower()
        if pattern in m_source and pattern not in m_dest:
            ctx.notice(f"missing {pattern}")

    if "%$" in dest:
        ctx.error("invalid %$")

    if "%%" in source and "%%" not in dest:
        ctx.warning("missing %%")

    if "PGN" in source and "PNG" in dest:
        ctx.warning("PNG instead of PGN")

    if "\n" not in source and "\n" in dest:
        ctx.notice("expected single line string")

    if western_punctuation(ctx.lang()) and ends_with_punctuation(source) and not ends_with_punctuation(dest):
        ctx.notice("translation does not end with punctuation")

    if re.match(r"\n", dest):
        ctx.error("has leading newlines")
    elif re.match(r"\s+", dest):
        ctx.warning("has leading spaces")

    if re.search(r"\s+$", dest):
        ctx.warning("has trailing spaces")

    if re.search(r"\t", dest):
        ctx.warning("has tabs")

    if re.search(r"\n{3,}", dest):
        ctx.warning("has more than one successive empty line")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args:
        report = Report()
        for arg in sys.argv[1:]:
            lint(report, pathlib.Path(arg))
        print(f"{report.errors} error(s), {report.warnings} warning(s)")
        if report.errors:
            sys.exit(1)
    else:
        print(f"Usage: {sys.argv[0]} translation/dest/*/*.xml")
