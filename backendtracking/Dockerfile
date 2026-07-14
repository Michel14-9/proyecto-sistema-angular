# Etapa de construccion
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

# Configurar encoding
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Etapa de ejecucion
FROM eclipse-temurin:21-jre-alpine

# Configurar encoding
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Crear usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 5002

CMD ["java", "-jar", "app.jar"]