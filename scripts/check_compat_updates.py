import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WATCHLIST_PATH = ROOT / ".github" / "compat-watchlist.json"
SUMMARY_PATH = ROOT / "compat-watch-summary.md"


def fetch_json(url, headers=None):
    request = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def fetch_modrinth_latest(project_id, loader, game_version):
    query = urllib.parse.urlencode(
        {
            "loaders": json.dumps([loader]),
            "game_versions": json.dumps([game_version]),
        }
    )
    url = f"https://api.modrinth.com/v2/project/{project_id}/version?{query}"
    versions = fetch_json(url, headers={"User-Agent": "ami-compat-watch/1.0"})
    if not versions:
        return None
    versions.sort(key=lambda version: version.get("date_published", ""), reverse=True)
    latest = versions[0]
    return {
        "version_number": latest.get("version_number"),
        "name": latest.get("name"),
        "date_published": latest.get("date_published"),
        "version_type": latest.get("version_type"),
        "url": f"https://modrinth.com/mod/{latest.get('project_id')}/version/{latest.get('id')}",
    }


def channel_label(channel):
    return f"{channel['loader']} {channel['game_version']}"


def issue_body(updates):
    lines = [
        "AMI's compat watch detected upstream mod updates for tracked channels.",
        "",
        "Refresh the affected compats, test them against the new upstream builds, and then update both:",
        "",
        "- `.github/compat-watchlist.json` `last_known_upstream_version`",
        "- `docs/compat-support-matrix.md` exact tested versions and support notes when applicable",
        "",
        "| Mod | Channel | Support | Last known upstream | Latest upstream | Exact tested | Published |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for update in updates:
        exact_tested = update["exact_tested_version"] or "Not recorded"
        latest_link = f"[{update['latest_version']}]({update['latest_url']})" if update["latest_url"] else update["latest_version"]
        lines.append(
            f"| {update['name']} | {update['channel']} | {update['support_level']} | "
            f"`{update['last_known_version']}` | {latest_link} | `{exact_tested}` | {update['latest_date']} |"
        )
    lines.extend(
        [
            "",
            "When a compat still works unchanged, bump the watchlist baseline in the same commit that records the tested version.",
            "If the upstream update broke AMI behavior, keep the issue open until the compat and docs are fixed together.",
        ]
    )
    return "\n".join(lines) + "\n"


def github_request(method, path, token, payload=None):
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "User-Agent": "ami-compat-watch/1.0",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    data = None
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(
        f"https://api.github.com{path}",
        data=data,
        headers=headers,
        method=method,
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        if response.headers.get("Content-Type", "").startswith("application/json"):
            return json.load(response)
        return response.read().decode("utf-8")


def ensure_label(owner, repo, token, label_name):
    try:
        github_request("GET", f"/repos/{owner}/{repo}/labels/{urllib.parse.quote(label_name)}", token)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        github_request(
            "POST",
            f"/repos/{owner}/{repo}/labels",
            token,
            {"name": label_name, "color": "0e8a16", "description": "AMI compat watch automation"},
        )


def find_open_issue(owner, repo, token, title, label_name):
    issues = github_request(
        "GET",
        f"/repos/{owner}/{repo}/issues?state=open&labels={urllib.parse.quote(label_name)}&per_page=100",
        token,
    )
    for issue in issues:
        if issue.get("title") == title:
            return issue
    return None


def sync_issue(config, updates):
    token = os.getenv("GITHUB_TOKEN")
    repository = os.getenv("GITHUB_REPOSITORY")
    if not token or not repository:
        return

    owner, repo = repository.split("/", 1)
    title = config["issue_title"]
    label_name = config["issue_label"]

    ensure_label(owner, repo, token, label_name)
    open_issue = find_open_issue(owner, repo, token, title, label_name)

    if updates:
        body = issue_body(updates)
        payload = {"title": title, "body": body, "labels": [label_name]}
        if open_issue:
            github_request("PATCH", f"/repos/{owner}/{repo}/issues/{open_issue['number']}", token, payload)
        else:
            github_request("POST", f"/repos/{owner}/{repo}/issues", token, payload)
        return

    if open_issue:
        github_request(
            "PATCH",
            f"/repos/{owner}/{repo}/issues/{open_issue['number']}",
            token,
            {
                "state": "closed",
                "body": open_issue.get("body", "") + "\n\nClosed automatically because the compat watch found no pending upstream updates.\n",
            },
        )


def write_summary(updates, failures):
    lines = ["# Compat Watch Summary", ""]
    if updates:
        lines.append("## Upstream Updates Detected")
        lines.append("")
        lines.append("| Mod | Channel | Last known | Latest |")
        lines.append("| --- | --- | --- | --- |")
        for update in updates:
            lines.append(
                f"| {update['name']} | {update['channel']} | `{update['last_known_version']}` | `{update['latest_version']}` |"
            )
    else:
        lines.append("No upstream compat updates detected for tracked channels.")

    if failures:
        lines.extend(["", "## Fetch Failures", ""])
        for failure in failures:
            lines.append(f"- {failure}")

    SUMMARY_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    config = json.loads(WATCHLIST_PATH.read_text(encoding="utf-8"))
    updates = []
    failures = []

    for entry in config["entries"]:
        if entry.get("source") != "modrinth":
            failures.append(f"{entry['name']}: unsupported source {entry.get('source')}")
            continue
        for channel in entry["channels"]:
            try:
                latest = fetch_modrinth_latest(entry["project_id"], channel["loader"], channel["game_version"])
            except Exception as error:  # noqa: BLE001
                failures.append(f"{entry['name']} {channel_label(channel)}: {error}")
                continue

            if latest is None:
                failures.append(f"{entry['name']} {channel_label(channel)}: no upstream version found")
                continue

            last_known = channel["last_known_upstream_version"]
            if latest["version_number"] != last_known:
                updates.append(
                    {
                        "name": entry["name"],
                        "channel": channel_label(channel),
                        "support_level": entry["support_level"],
                        "last_known_version": last_known,
                        "latest_version": latest["version_number"],
                        "latest_url": latest["url"],
                        "latest_date": latest["date_published"],
                        "exact_tested_version": channel.get("exact_tested_version"),
                    }
                )

    updates.sort(key=lambda item: (item["name"].lower(), item["channel"].lower()))
    write_summary(updates, failures)
    sync_issue(config, updates)

    print(Path(SUMMARY_PATH).read_text(encoding="utf-8"))
    if failures:
        print(f"Compat watch completed with {len(failures)} fetch failure(s).", file=sys.stderr)


if __name__ == "__main__":
    main()
