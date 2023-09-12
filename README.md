## TelegramBot

[SpringBoot](https://spring.io/projects/spring-boot) + [Spring-Data-Mongodb-Reactive](https://spring.io/projects/spring-data-mongodb) + [Java-Telegram-Bot-Api](https://github.com/pengrad/java-telegram-bot-api)

Demo：https://t.me/kukume_bot （可能不稳定）

## Environment

JDK17 + Mongodb

## Commands

* /login - 登陆账号
* /exec - 手动执行签到
* /manager - 管理自动签到状态，默认自动签到全为关

## Docker

https://hub.docker.com/r/kukume/tgbot

## Jar

https://pan.kuku.me/tgbot

## Config

```yaml
kuku:
  telegram:
    # @BotFather获取到的token
    token:
    # 机器人管理员的id
    creatorId: 0
    # 代理地址
    proxyHost:
    # 代理端口
    proxyPort: 0
    # 代理类型，可选 DIRECT（不设置代理）、HTTP、SOCKS
    proxyType: DIRECT
    # 自建的api服务器的地址（包含http://或者https://），如果不填，动态推送将不能推送50M以上的视频
    url:
    # 填写自建api服务器的配置目录，该机器人程序和自建api必须在一台服务器上
    # 如果是https://www.kuku.me/archives/41/的搭建api，且docker-compose.yml在/root/telegram-bot-api目录下
    # 该参数为 /root/telegram-bot-api/data
    # 如果不是使用docker，该参数为 /
    localPath:
```

## Features

* 

## Log

* `/info`可查看发送人的`id` ，设置`creatorId`，`/setting`中可下载日志
* `/log`中可下载失败任务日志

## LICENSE
`AGPLv3`
