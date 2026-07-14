# Etapa de construccion
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

# Configurar encoding para evitar problemas con caracteres
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

WORKDIR /app

# Copiar pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar codigo y compilar
COPY src ./src

# Compilar con encoding UTF-8
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Etapa de ejecucion
FROM eclipse-temurin:21-jre-alpine

# Configurar encoding
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

WORKDIR /app

# ✅ CREAR CARPETA BOLETAS Y DAR PERMISOS ANTES DE CAMBIAR DE USUARIO
RUN mkdir -p /app/boletas && \
    chmod -R 777 /app/boletas

# Copiar el JAR desde la etapa de construccion
COPY --from=build /app/target/sistema-apollo-*.jar app.jar

# Crear usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ✅ CAMBIAR PROPIETARIO DE LA CARPETA BOLETAS
RUN chown -R appuser:appgroup /app/boletas

# Cambiar al usuario no root
USER appuser

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]