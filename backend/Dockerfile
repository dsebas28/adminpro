# syntax=docker/dockerfile:1

# Etapa de compilación. Las dependencias van en su propia capa: solo se descargan si cambia el pom.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Imagen final: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 adminpro \
    && mkdir -p /data/adjuntos \
    && chown -R adminpro:adminpro /data
WORKDIR /app
COPY --from=build /app/target/adminpro-backend.jar app.jar
USER adminpro
ENV TZ=America/Bogota
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
