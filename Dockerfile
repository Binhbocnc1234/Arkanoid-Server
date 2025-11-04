FROM maven:3.9.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn package -DskipTests
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.example.RelayServer"]