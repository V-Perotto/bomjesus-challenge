# Estágio 1: build com JDK + Maven Wrapper (dependências em camada própria para aproveitar cache)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes rodam fora da imagem (./mvnw verify); aqui só empacotamos.
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted

# Estágio 2: runtime só com JRE, usuário sem privilégios e JAR em camadas
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
