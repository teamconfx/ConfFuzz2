import sys
import json
from typing import Dict, List

SUM_TOTAL = 0
SUM_CONFUZZ_SET = 0
SUM_JQF_SET = 0
SUM_RANDOM_SET = 0
SUM_CONFUZZ_ONLY = 0
SUM_JQF_ONLY = 0
SUM_RANDOM_ONLY = 0
SUM_CONFUZZ_JQF = 0
SUM_CONFUZZ_RANDOM = 0
SUM_JQF_RANDOM = 0
SUM_CONFUZZ_JQF_RANDOM = 0


# read from 3 json files
def read_from_json(tag: str) -> Dict[str, list[str]]:
    with open(f"meta_failures/{tag}_bug_id.json") as f:
        return json.load(f)

def compare_three_list(project: str, confuzz_list: list[str], jqf_list: list[str], random_list: list[str], output_bug_id: bool = False) -> str:
    confuzz_set = set(confuzz_list)
    jqf_set = set(jqf_list)
    random_set = set(random_list)
    total = confuzz_set | jqf_set | random_set
        
    # 1. how many bugs are only in list1, list2, list3
    confuzz_only = confuzz_set - jqf_set - random_set
    jqf_only = jqf_set - confuzz_set - random_set
    random_only = random_set - confuzz_set - jqf_set

    # 2. how many bugs are in all 3 lists
    confuzz_jqf_random = confuzz_set & jqf_set & random_set
    
    # 3. how many bugs are only in list1 and list2, list1 and list3, list2 and list3
    confuzz_jqf = confuzz_set & jqf_set - random_set
    confuzz_random = confuzz_set & random_set - jqf_set
    jqf_random = jqf_set & random_set - confuzz_set

    # update global variables
    global SUM_TOTAL
    global SUM_CONFUZZ_SET
    global SUM_JQF_SET
    global SUM_RANDOM_SET
    global SUM_CONFUZZ_ONLY
    global SUM_JQF_ONLY
    global SUM_RANDOM_ONLY
    global SUM_CONFUZZ_JQF
    global SUM_CONFUZZ_RANDOM
    global SUM_JQF_RANDOM
    global SUM_CONFUZZ_JQF_RANDOM

    SUM_TOTAL += len(total)
    SUM_CONFUZZ_SET += len(confuzz_set)
    SUM_JQF_SET += len(jqf_set)
    SUM_RANDOM_SET += len(random_set)
    SUM_CONFUZZ_ONLY += len(confuzz_only)
    SUM_JQF_ONLY += len(jqf_only)
    SUM_RANDOM_ONLY += len(random_only)
    SUM_CONFUZZ_JQF += len(confuzz_jqf)
    SUM_CONFUZZ_RANDOM += len(confuzz_random)
    SUM_JQF_RANDOM += len(jqf_random)
    SUM_CONFUZZ_JQF_RANDOM += len(confuzz_jqf_random)

    if len(total) == 0:
        return f"{project};{len(total)};{len(confuzz_set)};{len(jqf_set)};{len(random_set)};{len(confuzz_only)};{len(jqf_only)};{len(random_only)};{len(confuzz_jqf)};{len(confuzz_random)};{len(jqf_random)};{len(confuzz_jqf_random)}\n"

    
    confuzz_set_percentage = str(round(float(len(confuzz_set))/float(len(total)) * 100 ,2)) + "%"
    jqf_set_percentage = str(round(float(len(jqf_set))/float(len(total)) * 100 ,2)) + "%"
    random_set_percentage = str(round(float(len(random_set))/float(len(total)) * 100 ,2)) + "%"
    confuzz_only_percentage = str(round(float(len(confuzz_only))/float(len(total)) * 100 ,2)) + "%"
    jqf_only_percentage = str(round(float(len(jqf_only))/float(len(total)) * 100 ,2)) + "%"
    random_only_percentage = str(round(float(len(random_only))/float(len(total)) * 100 ,2)) + "%"
    confuzz_jqf_percentage = str(round(float(len(confuzz_jqf))/float(len(total)) * 100 ,2)) + "%"
    confuzz_random_percentage = str(round(float(len(confuzz_random))/float(len(total)) * 100 ,2)) + "%"
    jqf_random_percentage = str(round(float(len(jqf_random))/float(len(total)) * 100 ,2)) + "%"
    confuzz_jqf_random_percentage = str(round(float(len(confuzz_jqf_random))/float(len(total)) * 100 ,2)) + "%"
        

    if output_bug_id:
        return f"{project};{total};{confuzz_set};{jqf_set};{random_set};{confuzz_only};{jqf_only};{random_only};{confuzz_jqf};{confuzz_random};{jqf_random};{confuzz_jqf_random}\n"
    else:
        return f"{project};{len(total)};{len(confuzz_set)} ({confuzz_set_percentage});{len(jqf_set)} ({jqf_set_percentage});{len(random_set)} ({random_set_percentage});{len(confuzz_only)} ({confuzz_only_percentage});{len(jqf_only)} ({jqf_only_percentage});{len(random_only)} ({random_only_percentage});{len(confuzz_jqf)} ({confuzz_jqf_percentage});{len(confuzz_random)} ({confuzz_random_percentage});{len(jqf_random)} ({jqf_random_percentage});{len(confuzz_jqf_random)} ({confuzz_jqf_random_percentage})\n"


def main(output_bug_id: bool = False):
    confuzz_list = read_from_json("confuzz")
    jqf_list = read_from_json("jqf")
    random_list = read_from_json("random")
    # write to csv file
    file_name = "meta_failures/bug_diff_id.csv" if output_bug_id else "meta_failures/bug_diff_cnt.csv"
    with open(file_name, "w") as f:
        f.write("project;total;confuzz_total;jqf_total;random_total;confuzz_only;jqf_only;random_only;confuzz_jqf;confuzz_random;jqf_random;confuzz_jqf_random\n")
        for project in confuzz_list.keys():
            f.write(compare_three_list(project, confuzz_list[project], jqf_list[project], random_list[project], output_bug_id))
        # write the sum of each column
        f.write(f"SUM;{SUM_TOTAL};{SUM_CONFUZZ_SET};{SUM_JQF_SET};{SUM_RANDOM_SET};{SUM_CONFUZZ_ONLY};{SUM_JQF_ONLY};{SUM_RANDOM_ONLY};{SUM_CONFUZZ_JQF};{SUM_CONFUZZ_RANDOM};{SUM_JQF_RANDOM};{SUM_CONFUZZ_JQF_RANDOM}\n")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        main()
    else:
        main(bool(sys.argv[1]))
        

    
    
    



