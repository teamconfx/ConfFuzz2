from typing import List, Tuple
from pathlib import Path
from config import DATA_DIR, DIR_MAPPING, RESULT_DIR, COV_DATA_DIR, ROUNDS, FP_RESULT_DIR, FP_CONFIG_DIR
import matplotlib.pyplot as plt
def get_cov_last_point_csv() -> Path:
    return RESULT_DIR / 'meta_coverage' / 'last_point_cov.csv'

def get_mode(rnd: str) -> str|None:
    modes = [k for k in ROUNDS if rnd in ROUNDS[k]]
    assert len(modes) <= 1
    return modes[0] if modes else None

def get_mode(rnd: str) -> str:
    if not rnd.isnumeric():
        return rnd
    return [k for k in ROUNDS if rnd in ROUNDS[k]][0]

def get_bug_csv(mode: str) -> Path:
    return RESULT_DIR / 'meta_failures' / f"{mode}.csv"

def get_bug_id_json(mode: str) -> Path:
    if mode.isnumeric():
        mode = get_mode(mode)
    return RESULT_DIR / 'meta_failures' / f"{mode}_bug_id.json"

def get_fp_config_dir(project: str) -> Path:
    return FP_CONFIG_DIR / project.lower()

def get_fp_result_dir(rnd: str, project: str) -> Path:
    return FP_RESULT_DIR / f"r{rnd}" / f"r{rnd}-{project.lower()}-cov" /  f"r{rnd}-{project.lower()}-cov-output"

def get_cov_dir(rnd: str, projId: str) -> Path:
    return COV_DATA_DIR/ f"r{rnd}" 

def get_result_dir(rnd: str, projId: str) -> Path:
    return RESULT_DIR / f"r{rnd}-output" / f"r{rnd}-{projId}-output"

def get_data_dir(rnd: str, projId: str) -> Path:
    return DATA_DIR / f"r{rnd}" / f"r{rnd}-{projId}" / f"r{rnd}-{projId}-output"

def get_trunc_module(project: str) -> str:
    return DIR_MAPPING[project].split("/")[-1].replace('-', '')

def is_parsable(s:str):
    return s.isdigit() or (s[1:].isdigit() and s[0] == '-')

def is_boolean(s):
    return s in ["true", "false"]

def match_strings(s, p):
    if p == "" and s == "":
        return True
    if len(s) < len(p):
        return False
    j = 0
    for i in range(len(s)):
        if j == len(p) or s[i] != p[j]:
            if s[i] in p or s[i].isalnum():
                return False
        else:
            j += 1
    return j == len(p)
