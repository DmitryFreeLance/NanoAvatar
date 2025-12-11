FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Копируем собранный fat-jar
COPY target/nano-avatar-bot-1.0.0-jar-with-dependencies.jar app.jar

# SQLite база будет лежать снаружи
VOLUME ["/data"]

ENV JAVA_OPTS=""

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]