#!/usr/local/bin/python
# -*- coding: utf-8 -*-
import time as t
import sys

print('Testing regular characters')
print('Testing юникод キャラクターズ, 機能していますか？')

s = ''
ss = 'Testing long exec time'
for i in ss.split(' '):
    t.sleep(1)
    s += i + ' '
    print(s)
    
print('\nTesting long exec time', end=' ')
for i in 'and flush'.split(' '):
    t.sleep(1)
    print(i, end=' ')