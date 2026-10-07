#!/usr/bin/env python3
"""Publish build outputs (APKs + build log) into this repository via the GitHub API.

Each artifact is stored as a set of chunked git blobs (Git Data API) and a small JSON
manifest with the blob SHAs is committed through the Contents API. Everything is
reachable through api.github.com alone, so the artifacts can be fetched back even
where the Actions artifact/release CDNs are blocked.

Usage: ci_publish.py <log-path> <artifact1> [artifact2 ...]
"""
import base64
import json
import os
import sys
import urllib.error
import urllib.request

REPO = os.environ["GITHUB_REPOSITORY"]          # owner/repo
BRANCH = os.environ["GITHUB_REF_NAME"]          # branch this workflow runs on
TOKEN = os.environ["GITHUB_TOKEN"]
API = "https://api.github.com"
MANIFEST_PATH = "ci-build/apk-manifest.json"
CHUNK_SIZE = 24 * 1000 * 1000                    # 24 MB binary chunks (blob API limit: 100 MB)


def api_request(method, path, body=None):
    req = urllib.request.Request(API + path, method=method)
    req.add_header("Authorization", f"token {TOKEN}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, data) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        print(f"API {method} {path} -> HTTP {e.code}: {e.read().decode()[:500]}", file=sys.stderr)
        raise


def upload_blob(data: bytes) -> str:
    res = api_request(
        "POST",
        f"/repos/{REPO}/git/blobs",
        {"content": base64.b64encode(data).decode(), "encoding": "base64"},
    )
    return res["sha"]


def upload_chunks(path: str, prefix: str):
    """Split a file into chunks and upload each as a git blob. Returns (shas, total_size)."""
    if not os.path.exists(path):
        return [], 0
    shas = []
    size = os.path.getsize(path)
    with open(path, "rb") as f:
        index = 0
        while True:
            data = f.read(CHUNK_SIZE)
            if not data:
                break
            shas.append(upload_blob(data))
            print(f"  uploaded {prefix} chunk {index} ({len(data)} bytes) -> blob {shas[-1]}")
            index += 1
    return shas, size


def main():
    if len(sys.argv) < 2:
        print("usage: ci_publish.py <log-path> [artifact ...]", file=sys.stderr)
        sys.exit(2)
    log_path = sys.argv[1]
    artifacts = sys.argv[2:]

    print(f"Publishing artifacts for {REPO} @ {BRANCH}")
    files = {}
    for path in artifacts:
        name = os.path.basename(path)
        shas, size = upload_chunks(path, name)
        files[name] = {"size": size, "blob_shas": shas}
        print(f"  {name}: {size} bytes in {len(shas)} chunk(s)")
    log_shas, log_size = upload_chunks(log_path, "log")

    manifest = {
        "ref": BRANCH,
        "commit": os.environ.get("GITHUB_SHA"),
        "run_id": os.environ.get("GITHUB_RUN_ID"),
        "files": files,
        "build_log": {
            "size": log_size,
            "blob_shas": log_shas,
        },
    }

    # Create or update the manifest file ([skip ci] so this commit does not retrigger builds)
    body = {
        "message": "ci: publish build artifacts manifest [skip ci]",
        "content": base64.b64encode(json.dumps(manifest, indent=2).encode()).decode(),
        "branch": BRANCH,
    }
    try:
        existing = api_request("GET", f"/repos/{REPO}/contents/{MANIFEST_PATH}?ref={BRANCH}")
        body["sha"] = existing["sha"]
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
    api_request("PUT", f"/repos/{REPO}/contents/{MANIFEST_PATH}", body)

    print("MANIFEST_JSON_BEGIN")
    print(json.dumps(manifest, indent=2))
    print("MANIFEST_JSON_END")
    print(f"Manifest committed to {MANIFEST_PATH} on {BRANCH}")


if __name__ == "__main__":
    main()
