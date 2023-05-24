FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

# * Default packages
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# * Dependencies
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    firefox \
    tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/tesseract-ocr/tessdata/archive/refs/tags/4.1.0.zip
RUN unzip 4.1.0.zip
RUN rm 4.1.0.zip

WORKDIR /app
COPY target/rltb.jar /app/rltb.jar

ENTRYPOINT [ "java", "-jar", "/app/rltb.jar" ]
