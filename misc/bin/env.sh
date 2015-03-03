#!/bin/sh
export JAVA_HOME="/usr/lib/jvm/jdk1.7.0_25"
export JRE_HOME=$JAVA_HOME/jre
export GROOVY_HOME="/home/twitter/script/tools/groovy-2.4.1"
export PATH=$PATH:$JAVA_HOME/bin:$GROOVY_HOME/bin
echo "Import env.sh"
