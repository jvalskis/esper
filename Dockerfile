ARG JDK_VERSION=eclipse-temurin-jammy-19.0.1_10
ARG SBT_VERSION=1.9.0
ARG SCALA_VERSION=3.2.2
FROM sbtscala/scala-sbt:${JDK_VERSION}_${SBT_VERSION}_${SCALA_VERSION} AS builder

COPY src /build/src
COPY build.sbt /build/build.sbt
COPY project/*.properties /build/project/
COPY project/*.sbt /build/project/

WORKDIR /build

RUN sbt assembly


FROM sbtscala/scala-sbt:${JDK_VERSION}_${SBT_VERSION}_${SCALA_VERSION}
RUN mkdir /app
WORKDIR /app

COPY --from=builder /build/target/scala-${SCALA_VERSION}/ESPer-*.jar esper.jar

CMD ["java", "-jar", "esper.jar"]