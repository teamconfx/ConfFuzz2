import os
import sys

# find all file that name as "confuzz_identify_ctest.csv" under data/
for root, dirs, files in os.walk("./"):
    for file in files:
        if file == "confuzz_identify_ctest.csv":
            print(os.path.join(root, file))
            # remove the first line of the file and save it to a new file called "ctest_list.txt"
            with open(os.path.join(root, file), 'r') as f:
                lines = f.readlines()
                with open(os.path.join(root, "ctest_list.txt"), 'w') as f:
                    for line in lines[1:]:
                        f.write(line.split(',')[0] + "\n")