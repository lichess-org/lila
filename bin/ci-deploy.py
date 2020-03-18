#!/usr/bin/env python3

import os.path
import pickle
import git
import requests
import logging

logging.basicConfig(level=logging.DEBUG)


ASSET_FILES = [
    ".github/workflows/assets.yml",
    "public",
    "ui",
    "package.json",
    "yarn.lock",
]


def hash_files(tree, files):
    return tuple(tree[path].hexsha for path in files)


def find_commits(commit, files, wanted_hash):
    try:
        if hash_files(commit.tree, files) != wanted_hash:
            return
    except KeyError:
        return

    yield commit.hexsha

    for parent in commit.parents:
        yield from find_commits(parent, files, wanted_hash)


def workflow_runs(repo):
    with open(os.path.join(repo.common_dir, "workflow_runs.pickle"), "ab+") as f:
        try:
            f.seek(0)
            data = pickle.load(f)
        except EOFError:
            logging.info("Created workflow run database.")
            data = {}


        try:
            new = 0
            synced = False
            url = "https://api.github.com/repos/ornicar/lila/actions/workflows/assets.yml/runs"

            while not synced:
                logging.info("Fetching workflow runs ...")
                res = requests.get(url)
                for run in res.json()["workflow_runs"]:
                    if run["id"] in data:
                        logging.debug(f"Found workflow run {run['id']}.")
                        synced = True
                    else:
                        new += 1
                    data[run["id"]] = run

                if "next" not in res.links:
                    break
                url = res.links["next"]["url"]
        finally:
            f.seek(0)
            f.truncate()
            pickle.dump(data, f)
            logging.info(f"Added {new} new workflow run(s) to database.")

        return data


def find_workflows(wanted_commits):
    for workflow in workflows():
        if workflow["head_commit"]["id"] in wanted_commits:
            yield workflow


def main():
    repo = git.Repo(search_parent_directories=True)
    workflow_runs(repo)
    return

    try:
        wanted_hash = hash_files(repo.head.commit.tree, ASSET_FILES)
        print("Wanted hash:", wanted_hash)
    except KeyError:
        print("commit is missing asset file")
        raise

    wanted_commits = set(find_commits(repo.head.commit, ASSET_FILES, wanted_hash))
    print(f"{len(wanted_commits)} wanted commits:", wanted_commits)

    wanted_workflows = list(find_workflows(wanted_commits))
    print(f"{len(wanted_workflows)} wanted workflows:", wanted_workflows)


if __name__ == "__main__":
    main()
