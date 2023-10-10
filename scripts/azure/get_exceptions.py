import sys
import config
def read_from_file(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    return lines


def parse_exceptions(lines):
    # dict = {unique_string: counts}
    dict = {}
    for line in lines[1:]:
        #print(line)
        split_line = line.split(config.DELIMITER)
        try:
            exception_name = split_line[4]
        except:
            raise AssertionError(line)

        if exception_name not in dict:
            dict[exception_name] = 1
        else:
            dict[exception_name] += 1
    return dict

def write_dict_to_file(intput_file, output_file):
    dict = parse_exceptions(read_from_file(intput_file))
    with open(output_file, 'w') as f:
        for exception_name in dict:
            times = dict[exception_name]
            line = f"{exception_name}{config.DELIMITER}{times}\n"
            f.write(line)

def main(input_file, output_file):
    write_dict_to_file(input_file, output_file)
