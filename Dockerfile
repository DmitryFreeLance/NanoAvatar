# ---------- STAGE 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# сначала копируем только pom.xml — кэшируем зависимости
COPY pom.xml .

# подгружаем зависимости в кэш
RUN mvn -B -q dependency:go-offline

# теперь копируем исходники
COPY src ./src

# собираем fat-jar (у нас в pom настроен maven-assembly-plugin)
RUN mvn -B -DskipTests package

# ---------- STAGE 2: runtime ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# SQLite база будет лежать во внешнем томе
VOLUME ["/data"]

# копируем собранный fat-jar из первой стадии
COPY --from=build /build/target/*-jar-with-dependencies.jar /app/app.jar

# доп. JVM-опции можно передавать через -e JAVA_OPTS=...
ENV JAVA_OPTS=""

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]