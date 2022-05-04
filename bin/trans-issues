#!/usr/bin/env python3

import os
import requests

CROWDIN_USER = os.environ["CROWDIN_USER"]
CROWDIN_KEY = os.environ["CROWDIN_KEY"]

r = requests.get("https://api.crowdin.com/api/project/lichess/issues", params={
    "login": CROWDIN_USER,
    "account-key": CROWDIN_KEY,
    "status": "unresolved",
    "json": "1",
})

data = r.json()

def print_list(data, t):
    for issue in data:
        if issue["type"] != t:
            continue

        print("* [ ]", issue["comment"])
        print("     ", issue["string_url"])

print("# Source mistake")
print()
print_list(data, "source_mistake")
print()
print("# Context request")
print()
print_list(data, "context_request")
print()
print("# General question")
print()
print_list(data, "general_question")
