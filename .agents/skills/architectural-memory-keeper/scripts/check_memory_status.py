import os
import re
import subprocess
import json
import sys
from datetime import datetime

def find_project_root():
    """Dynamically locates the root directory of the current project."""
    try:
        res = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="ignore"
        )
        if res.returncode == 0 and res.stdout.strip():
            return os.path.abspath(res.stdout.strip())
    except Exception:
        pass
        
    curr = os.path.abspath(os.getcwd())
    while True:
        if os.path.exists(os.path.join(curr, ".git")) or os.path.exists(os.path.join(curr, ".specify")):
            return curr
        parent = os.path.dirname(curr)
        if parent == curr:
            break
        curr = parent
    return os.path.abspath(os.getcwd())

def detect_project_name(project_root):
    """Detects project name from common manifests or folder name."""
    # Check Android / Gradle manifests
    for settings_file in ["settings.gradle.kts", "settings.gradle"]:
        settings_path = os.path.join(project_root, settings_file)
        if os.path.exists(settings_path):
            try:
                with open(settings_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        m = re.search(r'rootProject\.name\s*=\s*["\']([^"\']+)["\']', line)
                        if m:
                            return m.group(1).strip()
            except Exception:
                pass

    pubspec = os.path.join(project_root, "pubspec.yaml")
    if os.path.exists(pubspec):
        try:
            with open(pubspec, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    if line.startswith("name:"):
                        return line.split(":", 1)[1].strip()
        except Exception:
            pass
            
    pkg_json = os.path.join(project_root, "package.json")
    if os.path.exists(pkg_json):
        try:
            with open(pkg_json, "r", encoding="utf-8", errors="ignore") as f:
                d = json.load(f)
                if "name" in d:
                    return d["name"]
        except Exception:
            pass

    return os.path.basename(project_root)

def detect_memory_file(project_root):
    """Finds existing project memory file across standard conventions."""
    candidates = [
        os.path.join(project_root, ".specify", "memory", "project_memory.md"),
        os.path.join(project_root, ".memory", "project_memory.md"),
        os.path.join(project_root, "memory", "project_memory.md"),
        os.path.join(project_root, "docs", "project_memory.md"),
        os.path.join(project_root, "project_memory.md")
    ]
    for c in candidates:
        if os.path.exists(c):
            return c, True
    return candidates[0], False

def get_last_memory_date(memory_file):
    if not os.path.exists(memory_file):
        return None, "FILE_NOT_FOUND"
    
    with open(memory_file, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
        
    # Search for date markers: [YYYY-MM-DD]
    dates = re.findall(r'\[(\d{4}-\d{2}-\d{2})\]', content)
    if not dates:
        mtime = os.path.getmtime(memory_file)
        dt = datetime.fromtimestamp(mtime).strftime("%Y-%m-%d")
        return dt, "FROM_FILE_MTIME"
        
    dates.sort(reverse=True)
    return dates[0], "FROM_CONTENT_LOG"

def get_git_commits_since(project_root, since_date):
    try:
        cmd = ["git", "log", f'--since="{since_date}"', "--oneline"]
        result = subprocess.run(cmd, cwd=project_root, capture_output=True, text=True, encoding="utf-8", errors="ignore")
        if result.returncode != 0:
            return []
        lines = [line.strip() for line in result.stdout.strip().splitlines() if line.strip()]
        return lines
    except Exception:
        return []

def main():
    project_root = find_project_root()
    project_name = detect_project_name(project_root)
    memory_file, exists = detect_memory_file(project_root)
    
    print("=" * 60)
    print(f"🏛️ UNIVERSAL ARCHITECTURAL MEMORY AUDITOR: {project_name}")
    print(f"📂 Project Root: {project_root}")
    print("=" * 60)
    
    if not exists:
        print(f"❌ STATUS: Memory file not found at {memory_file}")
        print("➡️ ACTION: Mode 1 (Fresh Bootstrap) is required.")
        print(f"   Run bootstrap using universal template at standard location: {memory_file}")
        return

    last_date, source = get_last_memory_date(memory_file)
    print(f"📄 Memory File: {memory_file}")
    print(f"📅 Last Checkpoint Date: {last_date} (Source: {source})")
    
    today = datetime.now().strftime("%Y-%m-%d")
    d_last = datetime.strptime(last_date, "%Y-%m-%d")
    d_now = datetime.strptime(today, "%Y-%m-%d")
    delta_days = (d_now - d_last).days
    
    print(f"⏳ Time Drift: {delta_days} day(s) since last memory update.")
    
    commits = get_git_commits_since(project_root, last_date)
    print(f"📦 Git Commits Since {last_date}: {len(commits)}")
    
    for c in commits[:8]:
        print(f"   • {c}")
    if len(commits) > 8:
        print(f"   ... and {len(commits) - 8} more commits.")
        
    print("-" * 60)
    if len(commits) == 0:
        print("✅ STATUS: Memory is 100% UP TO DATE with git commit history.")
        print("➡️ ACTION: No code sync needed. Ready for next task.")
    elif len(commits) <= 5 and delta_days <= 3:
        print("⚡ STATUS: Minor incremental drift detected.")
        print("➡️ ACTION: Mode 2 (Fast-Path Incremental Update) is recommended.")
    else:
        print("🔍 STATUS: Significant architectural drift detected.")
        print("➡️ ACTION: Mode 3 (Deep Panoramic Audit) is recommended.")
    print("=" * 60)

if __name__ == "__main__":
    main()
