FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# База будет храниться вне образа
VOLUME ["/data"]

# копируем jar
COPY target/nano-avatar-bot-1.0.0-jar-with-dependencies.jar app.jar
COPY .env .env

ENV JAVA_OPTS=""

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]