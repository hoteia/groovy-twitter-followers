#!/bin/sh
START=$(date +"%Y-%m-%d %H:%M:%S")
source /home/twitter/script/bin/env.sh
echo "Start clean logs $START"
rm -f /home/twitter/script/workspace/groovy-twitter/logs/**/*.log
END=$(date +"%Y-%m-%d %H:%M:%S")
echo "End clean logs $END"

