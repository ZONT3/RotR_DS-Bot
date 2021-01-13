#!/usr/local/bin/python
# -*- coding: utf-8 -*-

### DESC

# Обновить миссию из git и перезапустить сервер.
# Ключ -f - обновить принудительно.

### END

import os
import sys
import time
import git
import subprocess

# Constants
TARGET_PBO = "/home/arma3sw/serverfiles/mpmissions/MP_RotR_tst.egl_laghisola.pbo"
REPO = "/home/arma3sw/.zontpy/rotr_repo"
ARMA_SERVER = "/home/arma3sw/arma3server"
ARGS = "start"
# ARGS = "-server -port=2402 -config=server_test.cfg -cfg=basic_test.cfg " \
#       "-profiles=E:\\armaprofiles -name=server_test -serverMod=@extDB3 -loadMissionToMemory -enableHT -exThreads=7 " \
#       "-malloc=cma_x64 -hugePages -par=params_test.txt "

os.chdir(os.path.split(os.path.realpath(__file__))[0])

# Options
force = '-f' in sys.argv

# Git pulling
print('Git pull...', flush=True)
g = git.cmd.Git(REPO)
r = g.pull() == 'Already up to date.'

# Exit if up-to-date and not forcing update
if not force and r:
    print('Миссия на сервере имеет последнюю версию.')
    exit(0)

# Quit the server
print('Quitting server...', flush=True)
# TODO

print('Waiting for mission to be unlocked...', flush=True)
# Wait until PBO will unlock
# start = time.time()
# while not os.access(TARGET_PBO, os.W_OK):
#     if time.time() - start > 10:
#         print('Не могу заменить файл миссии: таймаут ожидания разблокировки.', file=sys.stderr)
#         exit(1)
#     time.sleep(0.1)
time.sleep(10)

# Making new PBO
print('Making PBO...', flush=True)
sys.argv = [sys.argv[0], REPO, os.path.split(TARGET_PBO)[0]]
with open('mkmission.py') as s:
    exec(s.read())

# Start server
print('Starting server...', flush=True)
os.chdir(os.path.split(ARMA_SERVER)[0])
# subprocess.Popen([ARMA_SERVER, ARGS], creationflags=0x8)
print('Server should be starting now.')
