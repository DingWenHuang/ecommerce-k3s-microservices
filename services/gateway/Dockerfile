# ========= Build stage =========
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先 copy pom 讓 maven dependency cache 能被重用（加速）
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# 再 copy 原始碼進來編譯
COPY src ./src
# 看 Docker builder 的 Java 版本
RUN java -version && mvn -v
RUN mvn -q -DskipTests clean package

# ========= Run stage =========
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 建議：Spring Boot 在容器內用 8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
