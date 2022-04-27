import sys
import json

default_number = 3;

def main():
    with open(sys.argv[1], 'r+') as f:
        data = json.load(f)
        transit(data)
        result = json.dumps(data)
        print(result)
 
def transit(data):
    if isinstance(data, dict):
        transit_dict(data)
    elif isinstance(data, list):
        transit_list(data)

def transit_dict(data):
    for key in data.keys():
        if key in ["titleImageUrls", 'contributors']:
            continue
        value = data[key]
        if isinstance(value, str) and not special_key(key):
            #continue
            data[key] = dummy_for_key(key)
        if isinstance(value, int) and not isinstance(value, bool) and not special_key_int(key):
            data[key] = default_number
        elif isinstance(value, (list, dict)):
            transit(value)

def transit_list(data):
    remove_element_from_list_till(data, default_number)
    for item in data:
        transit(item)

def special_key(key):
    #return contain_word(key) or equal_work(key) or endswith_work(key)
    return contain_word(key) or equal_work(key)

def special_key_int(key):
    words = ['date', 'creditStartTimeSeconds']
    for word in words:
        if word in key.lower():
            return True
    return False


def contain_word(key):
    #words = ['type', 'behaviour', 'url']
    words = ['type', 'behaviour']
    for word in words:
        if word in key.lower():
            return True
    return False

def equal_work(key):
    words = ['displayContext', 'videoQuality', 'family', 'updatePolicy']
    for word in words:
        if word == key:
            return True
    return False

def endswith_work(key):
    words = ['id', 'link']
    for word in words:
        if key.lower().endswith(word):
            return True
    return False

def dummy_for_key(key):
    words = ['id']
    for word in words:
        if key.lower().endswith(word):
            return 'amzn1.dummy'
    return 'dummy'


def remove_element_from_list_till(data, left):
    while(len(data) > left):
        data.pop()

main()
