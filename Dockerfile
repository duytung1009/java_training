# syntax=docker/dockerfile:1

# ---- build: biên dịch và đóng gói jar của module app ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Copy wrapper + pom trước để layer tải dependency được cache khi chỉ đổi source
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY domain/pom.xml domain/
COPY app/pom.xml app/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl app -am dependency:go-offline

COPY domain/src domain/src
COPY app/src app/src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl app -am package -DskipTests \
    && cp app/target/ecommerce-app-*.jar /build/app.jar      # glob không khớp *.jar.original

# ---- run: chỉ JRE + boot jar, chạy bằng user thường ----
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
