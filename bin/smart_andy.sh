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

# app base dir
APP_BASE=`pwd`

# system variables
system_properties="-Dapp.base.dir=$APP_BASE\
-Dapp.access.key=LTAIsr2SrukJKTh1\
-Dapp.access.secret=2IyzOJKDUm1phkCBou9T6ZWBiCTGpR\
"

# classpath and java.library.path
java_lib_path="$APP_BASE/lib"

class_path=${CLASSPATH}:${APP_BASE}
jars=`ls -1 lib/*.jar`
for j in ${jars}; do
    class_path="$j:$class_path"
done

# start
nohup "$_java -Djava.library.path=$java_lib_path -cp $class_path aceyin.smandy.SmartAndy"