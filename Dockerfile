FROM openjdk:21-bullseye
RUN apt update -y && apt install ffmpeg -y 
ADD tgbot-1.0-SNAPSHOT.jar /opt/kuku/tgbot-1.0-SNAPSHOT.jar
ADD application.yml /opt/kuku/application.yml
ADD docker-entrypoint.sh /opt/kuku/docker-entrypoint.sh
WORKDIR /opt/kuku
ENTRYPOINT ["/opt/kuku/docker-entrypoint.sh"]