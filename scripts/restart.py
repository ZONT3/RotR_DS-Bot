#!/usr/local/bin/python
# -*- coding: utf-8 -*-

### DESC

# Перезапустить сервер

### END

import os
import subprocess

ARMA_SERVER = "/home/arma3sw/arma3server"
ARGS = "start"

os.chdir(os.path.split(os.path.realpath(__file__))[0])

# Quit the server
print('Quitting server...', flush=True)
# TODO

# Start server
print('Starting server...', flush=True)
os.chdir(os.path.split(ARMA_SERVER)[0])
subprocess.Popen([ARMA_SERVER, ARGS], creationflags=0x8)
print('Server should be starting now')