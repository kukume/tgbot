## TelegramBot

[SpringBoot](https://spring.io/projects/spring-boot) + [Spring-Data-Mongodb-Reactive](https://spring.io/projects/spring-data-mongodb) + [Java-Telegram-Bot-Api](https://github.com/pengrad/java-telegram-bot-api)

Demo：https://t.me/kukume_bot （可能不稳定）

## Environment

JDK21 + Mongodb

## Commands

* /login - 登陆账号
* /exec - 手动执行签到
* /manager - 管理自动签到状态，默认自动签到全为关
* /log -自动签到日志
* /update - 更新程序
* /updatelog - 查看github提交消息
* /bv {bvid}
* /x {id}

## Beg

| price  | link                                      |
|--------|-------------------------------------------|
| 0.5EUR | https://buy.stripe.com/bIYaG75j0ci1bte28a |
| 1EUR   | https://buy.stripe.com/4gw4hJdPwci1apa6or |
| 2EUR   | https://buy.stripe.com/7sIaG7fXE5TDapacMQ |
| 10EUR  | https://buy.stripe.com/28ocOffXE2Hr8h2aEF |


## Docker

https://hub.docker.com/r/kukume/tgbot

## Jar

https://pan.kuku.me/tgbot

## Config

### application.yml

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
    # 填写自建telegram的api服务器的配置目录，该机器人程序和自建api必须在一台服务器上
    # 如果是https://www.kuku.me/archives/41/的搭建api，且docker-compose.yml在/root/telegram-bot-api目录下
    # 该参数为 /root/telegram-bot-api/data
    # 如果不是使用docker，该参数为 /
    localPath:
    # 填写自建api，用于无头浏览器执行的签到，见custom api项
    api:
```

### docker-compose.yml

```yaml
version: "3"
services:
  tgbot:
    image: kukume/tgbot
    container_name: tgbot
    ports: 
      - 8080:8080
    environment:
      # @BotFather获取到的token
      KUKU_TELEGRAM_TOKEN: 
      # 机器人管理员的id
      KUKU_TELEGRAM_CREATOR_ID: 0
      # 代理地址
      KUKU_TELEGRAM_PROXY_HOST:
      # 代理端口
      KUKU_TELEGRAM_PROXY_PORT: 0
      # 代理类型，可选 DIRECT（不设置代理）、HTTP、SOCKS
      KUKU_TELEGRAM_PROXY_TYPE: DIRECT
      # 自建的tg服务器的地址（包含http://或者https://），如果填了，
      # 上传文件到机器人的功能均会失效，如果不填，动态推送将不能推送50M以上的视频
      KUKU_TELEGRAM_URL:
      # 自建的api，用于无头浏览器执行签到或者加密参数
      KUKU_API:
    depends_on:
      - mongo

  mongo:
    image: mongo:4
    volumes:
      - ./db:/data/db
      - ./dump:/dump
```

## Custom api

https://hub.docker.com/r/kukume/sk

## Features

* 

## Log

* `/info`可查看发送人的`id` ，设置`creatorId`，`/setting`中可下载日志
* `/log`中可查看失败任务日志

## LICENSE
`AGPLv3`
