import sys

# print(sys.argv)

import json

def main():
    with open(sys.argv[1], 'r+') as f:
        data = json.load(f)
        # print(data)
        transit(data)
        y = json.dumps(data)
        print(y)
 
def transit(data):
    if isinstance(data, dict):
        transit_dict(data)
    elif isinstance(data, list):
        transit_list(data)


def transit_dict(data):
    for key in data.keys():
        value = data[key]
        if isinstance(value, str):
            data[key] = "dummy"
        elif isinstance(value, (list, dict)):
            transit(value)


def transit_list(data):
    for item in data:
        transit(item)

main()
