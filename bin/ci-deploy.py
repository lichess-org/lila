#!/usr/bin/env python3

import sys
import os
import os.path
import pickle
import git
import requests
import shlex
import subprocess
import time


ASSETS_FILES = [
    ".github/workflows/assets.yml",
    "public",
    "ui",
    "package.json",
    "yarn.lock",
]

SERVER_FILES = [
    ".github/workflows/server.yml",
    "app",
    "conf",
    "modules",
    "project",
    "build.sbt",
]

ASSETS_BUILD_URL = "https://api.github.com/repos/ornicar/lila/actions/workflows/assets.yml/runs"

SERVER_BUILD_URL = "https://api.github.com/repos/ornicar/lila/actions/workflows/server.yml/runs"

PROFILES = {
    "khiaw-assets": {
        "ssh": "root@khiaw.lichess.ovh",
        "wait": 2,
        "files": ASSETS_FILES,
        "workflow_url": ASSETS_BUILD_URL,
        "artifact_name": "lila-assets",
        "symlinks": ["public"],
        "post": "echo Reload assets on https://lichess.dev/dev/cli",
    },
    "khiaw-server": {
        "ssh": "root@khiaw.lichess.ovh",
        "wait": 2,
        "files": SERVER_FILES,
        "workflow_url": SERVER_BUILD_URL,
        "artifact_name": "lila-server",
        "symlinks": ["lib", "bin"],
        "post": "echo Run: systemctl restart lichess-stage",
    },
}


class DeployFailed(Exception):
    pass


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


def workflow_runs(profile, session, repo):
    with open(os.path.join(repo.common_dir, "workflow_runs.pickle"), "ab+") as f:
        try:
            f.seek(0)
            data = pickle.load(f)
        except EOFError:
            print("Created workflow run database.")
            data = {}

        try:
            new = 0
            synced = False
            url = profile["workflow_url"]

            while not synced:
                print("Fetching workflow runs ...")
                res = session.get(url)
                if res.status_code != 200:
                    print(f"Unexpected response: {res.status_code} {res.text}")
                    break

                for run in res.json()["workflow_runs"]:
                    if run["id"] in data and data[run["id"]]["status"] == "completed":
                        synced = True
                    else:
                        new += 1
                    run["_workflow_url"] = profile["workflow_url"]
                    data[run["id"]] = run

                if "next" not in res.links:
                    break
                url = res.links["next"]["url"]
        finally:
            f.seek(0)
            f.truncate()
            pickle.dump(data, f)
            print(f"Added/updated {new} workflow run(s).")

        return data


def find_workflow_run(profile, runs, wanted_commits):
    found = None

    print("Matching workflow runs:")
    for run in runs.values():
        if run["head_commit"]["id"] not in wanted_commits or run["_workflow_url"] != profile["workflow_url"]:
            continue

        if run["status"] != "completed":
            print(f"- {run['html_url']} PENDING.")
        elif run["conclusion"] != "success":
            print(f"- {run['html_url']} FAILED.")
        else:
            print(f"- {run['html_url']} succeeded.")
            if found is None:
                found = run

    if found is None:
        raise DeployFailed("Did not find successful matching workflow run.")

    print(f"Selected {found['html_url']}.")
    return found


def artifact_url(session, run, name):
    for artifact in session.get(run["artifacts_url"]).json()["artifacts"]:
        if artifact["name"] == name:
            if artifact["expired"]:
                print("Artifact expired.")
            return artifact["archive_download_url"]

    raise DeployFailed(f"Did not find artifact {name}.")


def main(profile):
    try:
        github_api_token = os.environ["GITHUB_API_TOKEN"]
    except KeyError:
        print("Need environment variable GITHUB_API_TOKEN. See https://github.com/settings/tokens/new. Scope public_repo.")
        return 128

    session = requests.Session()
    session.headers["Authorization"] = f"token {github_api_token}"

    repo = git.Repo(search_parent_directories=True)
    runs = workflow_runs(profile, session, repo)

    return deploy(profile, session, repo, runs)


def deploy(profile, session, repo, runs):
    try:
        wanted_hash = hash_files(repo.head.commit.tree, profile["files"])
    except KeyError:
        raise DeployFailed("Commit is missing a required file.")

    wanted_commits = set(find_commits(repo.head.commit, profile["files"], wanted_hash))
    print(f"Found {len(wanted_commits)} matching commits.")

    run = find_workflow_run(profile, runs, wanted_commits)
    url = artifact_url(session, run, profile["artifact_name"])

    print(f"Deploying {url} to {profile['ssh']} in {profile['wait']}s ...")
    time.sleep(profile["wait"])
    header = f"Authorization: {session.headers['Authorization']}"
    artifact_target = f"/home/lichess-artifacts/{profile['artifact_name']}-{run['id']:d}.zip"
    command = ";".join([
        f"mkdir -p /home/lichess-artifacts",
        f"mkdir -p /home/lichess-deploy/application.home_IS_UNDEFINED/logs",
        f"wget --header={shlex.quote(header)} -O {shlex.quote(artifact_target)} --no-clobber {shlex.quote(url)}",
        f"unzip -q -o {shlex.quote(artifact_target)} -d /home/lichess-artifacts/{profile['artifact_name']}-{run['id']:d}",
        f"cat /home/lichess-artifacts/{profile['artifact_name']}-{run['id']:d}/commit.txt",
        "chown -R lichess:lichess /home/lichess-artifacts"
    ] + [
        f"ln -f --no-target-directory -s /home/lichess-artifacts/{profile['artifact_name']}-{run['id']:d}/{symlink} /home/lichess-deploy/{symlink}"
        for symlink in profile["symlinks"]
    ] + [
        "chown -R lichess:lichess /home/lichess-deploy",
        "chmod -f +x /home/lichess-deploy/bin/lila || true",
        profile["post"],
        "/bin/bash",
    ])
    return subprocess.call(["ssh", "-t", profile["ssh"], "tmux", "new-session", "-s", "lila-deploy", f"/bin/sh -c {shlex.quote(command)}"], stdout=sys.stdout, stdin=sys.stdin)


if __name__ == "__main__":
    if len(sys.argv) <= 1:
        print(f"Usage: {sys.argv[0]} <profile>")
        for profile_name in PROFILES:
            print(f"- {profile_name}")
    else:
        try:
            sys.exit(main(PROFILES[sys.argv[1]]))
        except DeployFailed as err:
            print(err)
            sys.exit(1)
