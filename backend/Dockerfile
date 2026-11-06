# syntax=docker/dockerfile:1

# Etapa de compilación. La caché de Maven evita descargar todo en cada build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

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
