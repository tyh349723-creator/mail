# 后端镜像：一次构建，产出网关 / 业务服务 / 支付服务三个可执行 jar
# 运行时可指定 command 启动对应服务
# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=build /build/mall-gateway/target/mall-gateway-*.jar /app/mall-gateway.jar
COPY --from=build /build/mall-mgr/target/mall-mgr-*.jar /app/mall-mgr.jar
COPY --from=build /build/mall-alipay/target/mall-alipay-*.jar /app/mall-alipay.jar
EXPOSE 10010 10011 10012
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/mall-gateway.jar"]
