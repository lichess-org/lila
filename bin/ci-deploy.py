#!/usr/bin/env python3

import sys
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

ASSET_BUILDS_URL = "https://api.github.com/repos/ornicar/lila/actions/workflows/assets.yml/runs"


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


def workflow_runs(session, repo):
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
            url = ASSET_BUILDS_URL

            while not synced:
                logging.info("Fetching workflow runs ...")
                res = session.get(url)
                if res.status_code != 200:
                    logging.error(f"Unexpected response: {res.status_code} {res.text}")
                    break

                for run in res.json()["workflow_runs"]:
                    if run["id"] in data and data[run["id"]]["status"] == "completed":
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
            logging.info(f"Added/updated {new} workflow run(s).")

        return data


def find_workflow_run(runs, wanted_commits):
    found = None

    logging.info("Matching workflow runs:")
    for run in runs.values():
        if run["head_commit"]["id"] not in wanted_commits:
            continue

        if run["status"] != "completed":
            logging.info(f"- {run['html_url']} pending.")
        elif run["conclusion"] != "success":
            logging.info(f"- {run['html_url']} failed.")
        else:
            logging.info(f"- {run['html_url']} succeeded.")
            if found is None:
                found = run

    if found is None:
        raise RuntimeError("Did not find successful matching workflow run.")

    logging.info(f"Selected {found['html_url']}.")
    return found


def artifact_url(session, run, name):
    for artifact in session.get(run["artifacts_url"]).json()["artifacts"]:
        if artifact["name"] == name:
            if artifact["expired"]:
                logging.error("Artifact expired.")
            return artifact["archive_download_url"]

    raise RuntimeError(f"Did not find artifact {name}.")


def main():
    try:
        github_api_token = os.environ["GITHUB_API_TOKEN"]
    except KeyError:
        logging.error("Need environment variable GITHUB_API_TOKEN. See https://github.com/settings/tokens/new. Scope public_repo.")
        return 128

    session = requests.Session()
    session.headers["Authorization"] = f"token {github_api_token}"

    repo = git.Repo(search_parent_directories=True)
    runs = workflow_runs(session, repo)

    try:
        wanted_hash = hash_files(repo.head.commit.tree, ASSET_FILES)
    except KeyError:
        logging.exception("Commit is missing asset file.")
        return 1

    wanted_commits = set(find_commits(repo.head.commit, ASSET_FILES, wanted_hash))
    print(f"Found {len(wanted_commits)} matching commits.")

    run = find_workflow_run(runs, wanted_commits)
    url = artifact_url(session, run, "lila-assets")
    logging.info(f"Artifact URL: {url}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
