# Usa una base image per Java
FROM openjdk:17-jdk-slim

# Crea una directory di lavoro
WORKDIR /app

# Copia il jar nel container (modifica il nome se diverso)
COPY target/*.jar app.jar

# Espone la porta 8181
EXPOSE 8181

# Comando per avviare l'app
ENTRYPOINT ["java", "-jar", "app.jar"]
