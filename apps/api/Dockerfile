FROM eclipse-temurin:21-jre-jammy

WORKDIR /opt/portfolio

COPY target/portfolio-api-*.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/portfolio/app.jar"]
