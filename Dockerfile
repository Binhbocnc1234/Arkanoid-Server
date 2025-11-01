FROM openjdk:21-jdk
WORKDIR /app
COPY . .
RUN javac -cp kryonet-2.22.0-RC1.jar RelayServer.java
CMD ["java", "-cp", ".:kryonet-2.22.0-RC1.jar", "RelayServer"]
