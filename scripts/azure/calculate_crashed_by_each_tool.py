# calculate crashes for each tool between different rounds, this script rely on the output of calculate_crashed.py 
import sys
from typing import List, Dict, Set
from util import get_mode
from config import ROUNDS, PROJECTS_MAPPING
from pathlib import Path

FUZZ_TIME_DICT = {}

def main(rounds: List[str]):
    assert all(rnd in sum(ROUNDS.values(), []) for rnd in rounds), \
            f"{rounds} rounds must be all in confuzz"
    tables: Dict[str, Dict[str, Dict[str, Set]]] = {}
    global FUZZ_TIME_DICT
    for rnd in rounds:
        mode = get_mode(rnd)
        if mode not in tables:
            tables[mode] = {}
            FUZZ_TIME_DICT[mode] = {}
        if rnd not in tables[mode]:
            tables[mode][rnd] = {}
            FUZZ_TIME_DICT[mode][rnd] = {}
            
        # Handle JVM crashes
        jvm_crash_input_dir: Path = Path(f"metadata/{rnd}/noFuzzTimeFile")
        # for all file in jvm_crash_input_dir, calculate the crashed by each tool
        for jvm_crash_file in jvm_crash_input_dir.iterdir():
            proj = jvm_crash_file.name.split("_")[1]
            if proj not in tables[mode][rnd]:
                tables[mode][rnd][proj] = {}
                FUZZ_TIME_DICT[mode][rnd][proj] = [0, 0, 0, 0, 0]
            with open(jvm_crash_file, "r") as f:
                lines = f.readlines()
                crash_test_set = set()
                for line in lines:
                    crash_test_name = line.split(" ")[0]
                    fuzz_time_str = line.split(" ")[2]
                    crash_test_name = f"{crash_test_name}`{check_fuzz_time(fuzz_time_str)}"
                    crash_test_set.add(crash_test_name)
                # crash_test_set = set(map(lambda x: x.split(" ")[0], f.readlines()))
                tables[mode][rnd][proj] = crash_test_set
                
    output_dir: Path = Path(f"metadata/JVM_crash_output")
    output_dir.mkdir(parents=True, exist_ok=True)
    # print_to_console the result

    print("mode,#_JVM_crashes,less_than_1_min,1-10_min,10-20_min,20-30_min,more_then_30_min")
    for mode in tables:
        for rnd in tables[mode]:
            FUZZ_TIME_DICT[mode][rnd]['total'] = [0, 0, 0, 0, 0]
            str_to_write = ""
            total = 0
 #           print_to_console("proj,total_crashes,less_than_1_min,1-10_min,10-20_min,20-30_min,more_then_30_min")
            str_to_write += "proj,total_crashes,less_than_1_min,1-10_min,10-20_min,20-30_min,more_then_30_min\n"
            for proj in PROJECTS_MAPPING.keys():
#                print_to_console(f"{proj}-{rnd}-{mode},{len(tables[mode][rnd][proj])}", end=",")
                str_to_write += f"{proj},{len(tables[mode][rnd][proj])}," + print_to_console_num_in_fuzz_time_interval(tables[mode][rnd][proj], mode, rnd, proj) + "\n"
                total += len(tables[mode][rnd][proj])
                for i in range(5):
                    FUZZ_TIME_DICT[mode][rnd]['total'][i] += FUZZ_TIME_DICT[mode][rnd][proj][i]
                
            # calculate the total number of each value in FUZZ_TIME_DICT[mode][rnd][proj]
            detailed_total_str = ",".join(map(str, FUZZ_TIME_DICT[mode][rnd]['total']))
            print(f"{mode}-{rnd},{total},{detailed_total_str}")
            with open(f"{output_dir}/jvm_crash_{rnd}_{mode}.csv", "w") as f:
                f.write(str_to_write)
            print_to_console("")
        print_to_console("")

    with open(f"{output_dir}/crash_test_list.csv", "w") as f:
        # write title
        f.write("mode,round,project,crash_test_name\n")
        for mode in tables:
            for rnd in tables[mode]:
                for proj in PROJECTS_MAPPING.keys():
                    for crash_test_name in tables[mode][rnd][proj]:
                        f.write(f"{mode},{rnd},{proj},{crash_test_name.split('`')[0]}\n")

              

def check_fuzz_time(timeStr: str) -> int:
    # split into 4 parts: less than 1 min, 1-10 min, 10-20 min, 20-30 min, return the corresponding index
    # timeStr has the format of "00:01" where 00 is the minute and 01 is the second
    minute = int(timeStr.split(":")[0])
    if minute == 0:
        return 0
    elif minute < 10:
        return 1
    elif minute < 20:
        return 2
    elif minute < 30:
        return 3
    else:
        return -1

def print_to_console_num_in_fuzz_time_interval(crashSet, mode, rnd, proj):
    less_then_1_min = 0
    one_to_ten_min = 0
    ten_to_twenty_min = 0
    twenty_to_thirty_min = 0
    more_then_thirty_min = 0
    for crash_test_name in crashSet:
        index = int(crash_test_name.split("`")[1])
        if index == 0:
            less_then_1_min += 1
        elif index == 1:
            one_to_ten_min += 1
        elif index == 2:
            ten_to_twenty_min += 1
        elif index == 3:
            twenty_to_thirty_min += 1
        else:
            more_then_thirty_min += 1
    global FUZZ_TIME_DICT
    FUZZ_TIME_DICT[mode][rnd][proj][0] += less_then_1_min
    FUZZ_TIME_DICT[mode][rnd][proj][1] += one_to_ten_min
    FUZZ_TIME_DICT[mode][rnd][proj][2] += ten_to_twenty_min
    FUZZ_TIME_DICT[mode][rnd][proj][3] += twenty_to_thirty_min
    FUZZ_TIME_DICT[mode][rnd][proj][4] += more_then_thirty_min
    
    print_to_console(f"{less_then_1_min},{one_to_ten_min},{ten_to_twenty_min},{twenty_to_thirty_min},{more_then_thirty_min}")
    return f"{less_then_1_min},{one_to_ten_min},{ten_to_twenty_min},{twenty_to_thirty_min},{more_then_thirty_min}"


def print_to_console(s, toPrint_To_Console = False):
    if toPrint_To_Console:
        print(s)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_crashed_by_each_tool.py <rounds>")
        exit(1)
    rounds: List[str] = sorted(sys.argv[1].split(","))
    main(rounds)


