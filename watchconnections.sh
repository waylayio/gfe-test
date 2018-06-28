#!/bin/sh
sudo watch -n 1 "netstat -an | grep  ESTABLISHED | wc -l"
