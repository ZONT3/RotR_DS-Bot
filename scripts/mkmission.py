#!/usr/local/bin/python
# -*- coding: utf-8 -*-

import os
import pathlib
import sys
import tempfile
import shutil
import subprocess
from distutils.dir_util import copy_tree

# Platform-specific script. *nix only.

AD_DIR = f"/home/arma3sw/.zontpy"
FILE_PREV = f"{AD_DIR}/makepbo.last"
FILE_CFGCONVERT = "/home/arma3sw/a3tools/CfgConvert/CfgConvert.exe"
FILE_FILEBANK = "/home/arma3sw/a3tools/FileBank/FileBank.exe"
THIS_DIR = pathlib.Path(__file__).parent.absolute()


def get_prev() -> str:
    try:
        with open(FILE_PREV, 'r') as f:
            res = f.read()
    except IOError:
        res = ""
    return res


def set_prev(value: str):
    os.makedirs(AD_DIR, exist_ok=True)
    with open(FILE_PREV, 'w+') as f:
        f.write(value)


def request_mission() -> str:
    return sys.argv[1]


def copy_mission(src: str, dst: str):
    for copy_file in os.listdir(src):
        copy_path = os.path.join(src, copy_file)
        dest_path = os.path.join(dst, copy_file)
        if copy_file.startswith(".git"): continue
        if os.path.isfile(copy_path):
            shutil.copy(copy_path, dest_path)
        elif os.path.isdir(copy_path):
            copy_tree(copy_path, dest_path)


def convert_sqm(sqm_path: str):
    sqm_dir, sqm_name = os.path.split(sqm_path)
    old_path = os.path.join(sqm_dir, "old.sqm")
    os.rename(sqm_path, old_path)
    convert_arg = f"-bin -dst \"{sqm_path}\" \"{old_path}\""
    # os.system(f"{FILE_CFGCONVERT} {convert_arg}")
    subprocess.call(f"{FILE_CFGCONVERT} {convert_arg}")
    os.remove(old_path)


def make_pbo(source: str, dest: str, name):
    subprocess.call(f"{FILE_FILEBANK} -dst \"{dest}\" \"{source}\"")
    _, src_name = os.path.split(source)
    newpath = os.path.join(dest, name + ".pbo")
    try:
        os.remove(newpath)
    except FileNotFoundError:
        pass
    os.rename(os.path.join(dest, src_name + ".pbo"), newpath)


mission = request_mission()
if not os.path.isdir(mission):
    print("Enter a path, please!", file=sys.stderr)
    sys.exit(1)
_, mission_name = os.path.split(mission)

if len(sys.argv) < 3:
    to = THIS_DIR
else:
    to = sys.argv[2]

with tempfile.TemporaryDirectory() as tmpdir:
    copy_mission(mission, tmpdir)

    for file in os.listdir(tmpdir):
        path = os.path.join(tmpdir, file)
        if file == "mission.sqm":
            convert_sqm(path)

    make_pbo(tmpdir, to, mission_name)

print("OK!")
