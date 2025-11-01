FROM openjdk:21-jdk
WORKDIR /app
COPY . .
RUN javac RelayServer.java
CMD ["java", "RelayServer"]