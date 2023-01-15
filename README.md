## TelegramBot

[SpringBoot](https://spring.io/projects/spring-boot) + [Spring-Data-Mongodb-Reactive](https://spring.io/projects/spring-data-mongodb) + [Telegram-Bots](https://github.com/rubenlagus/TelegramBots)

Demo：https://t.me/kukume_bot

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
    # bot的用户名
    username:
    # 机器人管理员的id
    creatorId:
    # 代理地址
    proxyHost:
    # 代理端口
    proxyPort: 0
    # 代理类型，可选 NO_PROXY（不设置代理）、HTTP、SOCKS4、SOCKS5
    proxyType: NO_PROXY
    # 自建的tg服务器的地址（包含http://或者https://），如果填了，
    # 上传文件到机器人的功能均会失效，如果不填，动态推送将不能推送50M以上的视频
    url: 
```

## Features

* 

## LICENSE
AGPL
