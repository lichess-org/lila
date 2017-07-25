#!/usr/bin/python3

# timeago-to-scala - convert 
# https://github.com/hustcc/timeago.js/tree/master/locales to scala
#
# Copyright (C) 2017 Lakin Wecker <lakin@wecker.ca>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.  

import argparse
import glob
from collections import defaultdict

template = '''object TimeagoLocales {{
    val locales: Map[String, String] = Map(
        {cases}
    )
}}
'''

case_template = '''        "{key}" -> """{contents}"""'''

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("locale_dir", help="A lichess username")
    args = parser.parse_args()

    cases = {}

    locale_dir = args.locale_dir
    for file in glob.glob("{}/*.js".format(locale_dir)):
        locale_key = file.split("/")[-1].replace(".js", "")
        if locale_key == "locales":
            continue
        lines = [l.strip() for l in open(file, "r").readlines()]
        lines = [l for l in lines if l and not l.startswith("//")]
        contents = " ".join(lines)
        contents = contents.replace("module.exports = ", "")
        cases[locale_key] = case_template.format(key=locale_key, contents=contents)
    cases = [v for k,v in sorted(cases.items())]
    print(template.format(cases=",\n".join(cases)))

if __name__ == '__main__':
    main()
