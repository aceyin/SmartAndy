#!/usr/bin/env bash
# check args
if [[ $# == 0 ]]; then
       echo "Usage: smart_andy.sh <start|stop> [debug]"
       exit 1
fi

# check java executable
if type -p java; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
else
    echo "NO Java found, please install Java 8 or later"
    exit 1
fi

# check java version
if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" < "1.8" ]]; then
        echo "Please use JDK which version grater than 1.8"
        exit 1
    fi
fi

# base dir
APP_BASE=`pwd`

# classpath and java.library.path
JAVA_LIB_PATH="$APP_BASE/lib"
CLASS_PATH="$CLASSPATH:.:$APP_BASE/lib:"

# start
nohup "$_java -Djava.library.path=$JAVA_LIB_PATH -cp $CLASS_PATH aceyin.smandy.SmartAndy"