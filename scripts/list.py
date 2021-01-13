#!/usr/local/bin/python
# -*- coding: utf-8 -*-

import os
import re


def get_desc(file):
    in_area = False
    res = ''
    for line in file:
        strip = line.strip()
        if re.match('####* *DESC.*', strip):
            in_area = True
            continue
        if re.match('####* *END.*', strip):
            break
        if in_area and not strip == '':
            res += line.removeprefix('#')
    return res


path = os.path.split(os.path.realpath(__file__))[0]

for filename in os.listdir(path):
    if not filename.endswith('.py'):
        continue
    with open(os.path.join(path, filename), encoding='UTF-8') as f:
        desc = get_desc(f)
        if not desc == '':
            print(os.path.split(filename)[1].removesuffix('.py') + ':')
            print(desc)
