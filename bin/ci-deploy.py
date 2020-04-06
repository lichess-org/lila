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
    "translation",
    "build.sbt",
]

ASSETS_BUILD_URL = "https://api.github.com/repos/ornicar/lila/actions/workflows/assets.yml/runs"

SERVER_BUILD_URL = "https://api.github.com/repos/ornicar/lila/actions/workflows/server.yml/runs"

PROFILES = {
    "khiaw-assets": {
        "ssh": "root@khiaw.lichess.ovh",
        "artifact_dir": "/home/lichess-artifacts",
        "deploy_dir": "/home/lichess-deploy",
        "files": ASSETS_FILES,
        "workflow_url": ASSETS_BUILD_URL,
        "artifact_name": "lila-assets",
        "symlinks": ["public"],
        "post": "echo Reload assets on https://lichess.dev/dev/cli",
    },
    "khiaw-server": {
        "ssh": "root@khiaw.lichess.ovh",
        "artifact_dir": "/home/lichess-artifacts",
        "deploy_dir": "/home/lichess-deploy",
        "files": SERVER_FILES,
        "workflow_url": SERVER_BUILD_URL,
        "artifact_name": "lila-server",
        "symlinks": ["lib", "bin"],
        "post": "systemctl restart lichess-stage",
    },
    "ocean-server": {
        "ssh": "root@ocean.lichess.ovh",
        "artifact_dir": "/home/lichess-artifacts",
        "deploy_dir": "/home/lichess",
        "files": SERVER_FILES,
        "workflow_url": SERVER_BUILD_URL,
        "artifact_name": "lila-server",
        "symlinks": ["lib", "bin"],
        "post": "systemctl restart lichess",
    },
    "maple-assets": {
        "ssh": "root@maple.lichess.ovh",
        "artifact_dir": "/home/lichess-artifacts",
        "deploy_dir": "/home/lichess-deploy",
        "files": ASSETS_FILES,
        "workflow_url": ASSETS_BUILD_URL,
        "artifact_name": "lila-assets",
        "symlinks": ["public"],
        "post": "echo Reload assets on https://lichess.org/dev/cli",
    },
    "ocean-assets": {
        "ssh": "root@ocean.lichess.ovh",
        "artifact_dir": "/home/lichess-artifacts",
        "deploy_dir": "/home/lichess-deploy",
        "files": ASSETS_FILES,
        "workflow_url": ASSETS_BUILD_URL,
        "artifact_name": "lila-assets",
        "symlinks": ["public"],
        "post": "echo Reload assets on https://lichess.org/dev/cli",
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

    print(f"Deploying {url} to {profile['ssh']}...")
    header = f"Authorization: {session.headers['Authorization']}"
    artifact_target = f"{profile['artifact_dir']}/{profile['artifact_name']}-{run['id']:d}.zip"
    command = ";".join([
        f"mkdir -p {profile['artifact_dir']}",
        f"mkdir -p {profile['deploy_dir']}/application.home_IS_UNDEFINED/logs",
        f"wget --header={shlex.quote(header)} -O {shlex.quote(artifact_target)} --no-clobber {shlex.quote(url)}",
        f"unzip -q -o {shlex.quote(artifact_target)} -d {profile['artifact_dir']}/{profile['artifact_name']}-{run['id']:d}",
        f"cat {profile['artifact_dir']}/{profile['artifact_name']}-{run['id']:d}/commit.txt",
        f"chown -R lichess:lichess {profile['artifact_dir']}"
    ] + [
        f"ln -f --no-target-directory -s {profile['artifact_dir']}/{profile['artifact_name']}-{run['id']:d}/{symlink} {profile['deploy_dir']}/{symlink}"
        for symlink in profile["symlinks"]
    ] + [
        f"chown -R lichess:lichess {profile['deploy_dir']}",
        f"chmod -f +x {profile['deploy_dir']}/bin/lila || true",
        f"echo \"----------------------------------------------\"",
        f"echo \"SERVER:   {profile['ssh']}\"",
        f"echo \"ARTIFACT: {profile['artifact_name']}\"",
        f"echo \"COMMAND:  {profile['post']}\"",
        f"/bin/bash -c \"read -n 1 -p 'Press [Enter] to proceed.'\"",
        profile["post"],
        f"echo \"done.\"",
        "/bin/bash",
    ])
    return subprocess.call(["ssh", "-t", profile["ssh"], "tmux", "new-session", "-A", "-s", "lila-deploy", f"/bin/sh -c {shlex.quote(command)}"], stdout=sys.stdout, stdin=sys.stdin)


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
