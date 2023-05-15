## TelegramBot

[SpringBoot](https://spring.io/projects/spring-boot) + [Spring-Data-Mongodb-Reactive](https://spring.io/projects/spring-data-mongodb) + [Java-Telegram-Bot-Api](https://github.com/pengrad/java-telegram-bot-api)

Demo：https://t.me/kukume_bot （搭建在家宽上，可能不稳定）

## Environment

JDK17 + Mongodb

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
    # 自建的tg服务器的地址（包含http://或者https://），如果填了，
    # 上传文件到机器人的功能均会失效，如果不填，动态推送将不能推送50M以上的视频
    url: 
```

## Features

* 

## LICENSE
AGPL
