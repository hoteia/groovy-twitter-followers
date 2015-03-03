#!/bin/sh
source /home/twitter/script/bin/env.sh
START=$(date +"%Y-%m-%d %H:%M:%S")
echo "Start $START"
groovy -classpath /home/twitter/script/workspace/groovy-twitter/classpath -DrootScriptDir="/home/twitter/script/workspace/groovy-twitter/" -Dcontext=YOUR_ACCOUNT -Dmode=clean.followers /home/twitter/script/workspace/groovy-twitter/twitter4j-followback-users.groovy
END=$(date +"%Y-%m-%d %H:%M:%S")
echo "End $END"

