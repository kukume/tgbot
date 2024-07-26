#!/bin/bash

# mongodb 的 uri
export MONGO_URI=mongodb://localhost
# mongodb 的 数据库名称
export MONGO_DATABASE=tg
# @BotFather获取到的token
export TELEGRAM_TOKEN=
# 机器人管理员的id
export TELEGRAM_CREATOR_ID=0
# 代理地址
export TELEGRAM_PROXY_HOST=
# 代理端口
export TELEGRAM_PROXY_PORT=0
# 代理类型，可选 DIRECT（不设置代理）、HTTP、SOCKS
export TELEGRAM_PROXY_TYPE=direct
# 自建的api服务器的地址（包含http://或者https://），如果不填，动态推送将不能推送50M以上的视频
export TELEGRAM_URL=
# 填写自建telegram的api服务器的配置目录，该机器人程序和自建api必须在一台服务器上
# 如果是https://www.kuku.me/archives/41/的搭建api，且docker-compose.yml在/root/telegram-bot-api目录下
# 该参数为 /root/telegram-bot-api/data
# 如果不是使用docker，该参数为 /
export TELEGRAM_LOCAL_PATH=0
# 自建的api，用于无头浏览器执行签到或者加密参数
export TELEGRAM_API=

JAVA_HOME=~/jdk/jdk-21+35
java=$JAVA_HOME/bin/java

jar_name=tgbot.jar
jar_new_name=tgbot-new.jar
rm -f update.pid

"${java}" -jar $jar_name &

function kill_java {
        pid=`cat application.pid`
        kill $pid
        exit 1
}

trap "kill_java" SIGINT

while true; do
        if [ -e "update.pid" ] ;
        then
                rm -f update.pid
                pid=`cat application.pid`
                kill $pid
                sleep 3
                rm $jar_name
                mv tmp/$jar_new_name $jar_name
                "${java}" -jar $jar_name &
        else
                sleep 5
        fi
done