ARG JDK_VERSION=eclipse-temurin-jammy-20.0.2_9
ARG SBT_VERSION=1.9.6
ARG SCALA_VERSION=3.3.1
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

ARG VERSION
LABEL version=${VERSION}