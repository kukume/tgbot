#!/bin/bash

jar_name=tgbot-1.0-SNAPSHOT.jar
jar_new_name=tgbot-1.0-SNAPSHOT-new.jar
rm -f update.pid

java -Dspring.profiles.active=prod -Duser.timezone=Asia/Shanghai -jar $jar_name &

function kill_java {
        pid=$(cat application.pid)
        kill "$pid"
        exit 1
}

trap "kill_java" SIGINT

while true; do
        if [ -e "update.pid" ] ;
        then
                rm -f update.pid
                pid=$(cat application.pid)
                kill "$pid"
                sleep 3
                rm $jar_name
                mv tmp/$jar_new_name $jar_name
                java -Dspring.profiles.active=prod -Duser.timezone=Asia/Shanghai -jar $jar_name &
        else
                sleep 5
        fi
done