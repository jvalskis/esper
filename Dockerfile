ARG JDK_VERSION=eclipse-temurin-jammy-19.0.1_10
ARG SBT_VERSION=1.9.0
ARG SCALA_VERSION=3.2.2
FROM sbtscala/scala-sbt:${JDK_VERSION}_${SBT_VERSION}_${SCALA_VERSION} AS builder

COPY src /build/src
COPY build.sbt /build/build.sbt
COPY project/build.properties /build/project/build.properties

WORKDIR /build

RUN sbt compile