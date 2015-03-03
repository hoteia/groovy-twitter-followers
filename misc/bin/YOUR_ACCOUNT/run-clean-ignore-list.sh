#!/bin/sh
source /home/twitter/script/bin/env.sh
START=$(date +"%Y-%m-%d %H:%M:%S")
echo "Start $START"
'>/home/twitter/script/workspace/groovy-twitter/datas/YOUR_ACCOUNT/ignore_followers.properties'
END=$(date +"%Y-%m-%d %H:%M:%S")
echo "End $END"

