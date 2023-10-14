FROM openjdk:21-bullseye
RUN apt update -y && apt install ffmpeg -y
ADD tgbot-1.0-SNAPSHOT.jar /opt/kuku/tgbot-1.0-SNAPSHOT.jar
ADD application.yml /opt/kuku/application.yml
WORKDIR /opt/kuku
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "tgbot-1.0-SNAPSHOT.jar"]
