#!/usr/bin/env python3
"""PreToolUse guard for Bash commands (wired in .claude/settings.json).

Blocks, with a message Claude sees:
  1. Anything that wipes the real app's data (dev.nerzouille.koan) on a phone:
     adb uninstall (with or without -k), pm / cmd package uninstall or clear, run-as rm/find -delete
     on its files, rm on /data/data/dev.nerzouille.koan. Installs are allowed (dev build included);
     staging (.staging), baseline-profile (.bp), debug (.debug) and test ids are never blocked.
  2. git push to main (explicit main refspec, --all / --mirror, or a bare push while on main).
  3. git add . / -A / --all (stage files by name instead).

Exit code 2 = block (stderr goes back to Claude). Exit code 0 = allow.
"""
import json
import os
import re
import shlex
import subprocess
import sys

REAL_PACKAGE = "dev.nerzouille.koan"


def block(msg):
    print(f"Blocked by .claude/hooks/guard.py: {msg}", file=sys.stderr)
    sys.exit(2)


def split_commands(command):
    """Split a shell line into simple commands (on ; && || | & and newlines), as token lists."""
    lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|")
    lexer.whitespace_split = True
    lexer.commenters = ""
    try:
        tokens = list(lexer)
    except ValueError:  # unbalanced quotes: fall back to a plain split
        tokens = command.split()
    cmds, cur = [], []
    for t in tokens:
        if t and set(t) <= set(";&|\n"):
            if cur:
                cmds.append(cur)
            cur = []
        else:
            cur.append(t)
    if cur:
        cmds.append(cur)
    # Also look inside `bash -c "..."`, `sh -c '...'`, `$( ... )` is left to the tokens.
    out = []
    for c in cmds:
        out.append(c)
        for i, t in enumerate(c[:-1]):
            if os.path.basename(t) in ("bash", "sh", "zsh") and c[i + 1] == "-c" and i + 2 < len(c):
                out.extend(split_commands(c[i + 2]))
    return out


def strip_prefix(cmd):
    """Drop env assignments, sudo, env, time, etc. before the real program."""
    i = 0
    while i < len(cmd):
        t = cmd[i]
        if re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", t) or t in ("sudo", "env", "time", "nice", "command", "exec"):
            i += 1
        elif t.startswith("(") or t.startswith("{"):
            cmd = cmd[:i] + [t.lstrip("({")] + cmd[i + 1:]
            if not cmd[i]:
                i += 1
        else:
            break
    return cmd[i:]


# ---------- adb ----------

ADB_OPTS_WITH_ARG = {"-s", "-t", "-H", "-P", "-L"}
WIPE_MSG = ("would wipe the real app's data on the phone (the owner's tabs, spaces, account). "
            "Installs are fine, wipes are not. If an install failed on a signature mismatch "
            "(INSTALL_FAILED_UPDATE_INCOMPATIBLE), stop and tell the owner instead of uninstalling.")


def is_real(token):
    return token == REAL_PACKAGE


def check_device_command(cmd):
    """One command run on the phone (after `adb shell`)."""
    cmd = strip_prefix(cmd)
    if not cmd:
        return
    prog = os.path.basename(cmd[0])
    if prog == "su":  # su [uid] [-c] <command...>
        rest = cmd[1:]
        while rest and (rest[0] in ("-c", "root", "-") or rest[0].isdigit()):
            rest = rest[1:]
        for c in split_commands(" ".join(rest)):
            check_device_command(c)
        return
    if prog == "cmd" and len(cmd) > 1 and cmd[1] == "package":
        prog, cmd = "pm", ["pm"] + cmd[2:]
    if prog == "pm" and len(cmd) > 1 and cmd[1] in ("uninstall", "clear"):
        if any(is_real(a) for a in cmd[2:]):
            block(f"`{cmd[0]} {cmd[1]} {REAL_PACKAGE}` " + WIPE_MSG)
    elif prog == "run-as" and len(cmd) > 1 and is_real(cmd[1]):
        inner = " ".join(cmd[2:])
        if re.search(r"(^|[\s;&|'\"(])(rm|rmdir|truncate|shred)\b|\bfind\b.*\s-delete\b|>\s*\S", inner):
            block(f"`run-as {REAL_PACKAGE}` deleting or overwriting files " + WIPE_MSG)
    elif prog == "rm":
        pkg_path = re.compile(r"/data/(data|user(_de)?/\d+)/" + re.escape(REAL_PACKAGE) + r"(/|$)")
        if any(pkg_path.search(a) for a in cmd[1:]):
            block(f"`rm` on {REAL_PACKAGE}'s data folder " + WIPE_MSG)


def check_adb(cmd):
    args = cmd[1:]
    i = 0
    while i < len(args) and args[i].startswith("-"):
        i += 2 if args[i] in ADB_OPTS_WITH_ARG else 1
    if i >= len(args):
        return
    sub, rest = args[i], args[i + 1:]
    if sub == "uninstall":
        if any(is_real(a) for a in rest):
            block(f"`adb uninstall {REAL_PACKAGE}` (with or without -k) " + WIPE_MSG)
    elif sub == "shell":
        for c in split_commands(" ".join(rest)):
            check_device_command(c)


# ---------- git ----------

GIT_OPTS_WITH_ARG = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--exec-path"}


def git_split(cmd):
    """Return (subcommand, args, cwd for -C) for a git command."""
    args = cmd[1:]
    i, cdir = 0, None
    while i < len(args) and args[i].startswith("-"):
        if args[i] in GIT_OPTS_WITH_ARG and i + 1 < len(args):
            if args[i] == "-C":
                cdir = args[i + 1]
            i += 2
        else:
            i += 1
    if i >= len(args):
        return None, [], cdir
    return args[i], args[i + 1:], cdir


def current_branch(cwd):
    try:
        r = subprocess.run(["git", "-C", cwd, "branch", "--show-current"], capture_output=True, text=True, timeout=10)
        return r.stdout.strip()
    except (OSError, subprocess.TimeoutExpired):
        return ""


PUSH_OPTS_WITH_ARG = {"-o", "--push-option", "--repo", "--receive-pack", "--exec", "--signed"}


def check_git_push(args, cwd):
    if any(a in ("--all", "--mirror") for a in args):
        block("git push --all / --mirror would push main. Push one branch by name (never main).")
    positional, i = [], 0
    while i < len(args):
        a = args[i]
        if a in PUSH_OPTS_WITH_ARG:
            i += 2
            continue
        if not a.startswith("-"):
            positional.append(a)
        i += 1
    refspecs = positional[1:]  # positional[0] is the remote
    for spec in refspecs:
        dst = spec.lstrip("+").split(":")[-1]
        if dst in ("main", "refs/heads/main") or (dst == "HEAD" and current_branch(cwd) == "main"):
            block("pushing to main is not allowed: main only changes through a release the owner "
                  "approves (CLAUDE.md 'Git branches'). Push dev or a feature branch.")
    if not refspecs and current_branch(cwd) == "main":
        block("bare `git push` while on main. Main only changes through a release the owner approves.")


def check_git_add(args):
    for a in args:
        if a == "--":
            break
        if a in (".", "./", "--all", "-A", ":/", "*") or re.match(r"^-[a-zA-Z]*A[a-zA-Z]*$", a):
            block(f"`git add {a}` stages everything, including files that may hold personal data "
                  "(public repo). Stage files by name: `git add path/one path/two`.")


def main():
    try:
        data = json.load(sys.stdin)
    except ValueError:
        return
    if data.get("tool_name") != "Bash":
        return
    command = (data.get("tool_input") or {}).get("command", "")
    cwd = data.get("cwd") or os.getcwd()
    for cmd in split_commands(command):
        cmd = strip_prefix(cmd)
        if not cmd:
            continue
        prog = os.path.basename(cmd[0])
        if prog == "cd" and len(cmd) > 1:
            cwd = os.path.join(cwd, os.path.expanduser(cmd[1]))
        elif prog == "adb":
            check_adb(cmd)
        elif prog == "git":
            sub, args, cdir = git_split(cmd)
            gcwd = os.path.join(cwd, cdir) if cdir else cwd
            if sub == "push":
                check_git_push(args, gcwd)
            elif sub == "add":
                check_git_add(args)


if __name__ == "__main__":
    main()
