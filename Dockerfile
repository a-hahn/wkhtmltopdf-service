FROM ubuntu:18.04
MAINTAINER ahahn@gmx.net

# wkhtmltopdf installation is from https://github.com/oberonamsterdam/docker-wkhtmltopdf/blob/master/Dockerfile
ENV DEBIAN_FRONTEND noninteractive

RUN sed 's/main$/main universe/' -i /etc/apt/sources.list

RUN apt-get update
RUN apt-get upgrade -y

# Download and install wkhtmltopdf
RUN apt-get install -y build-essential xorg libssl-dev libxrender-dev wget

# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends xvfb libfontconfig libjpeg-turbo8 xfonts-75dpi fontconfig

RUN wget --no-check-certificate https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN dpkg -i wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN rm wkhtmltox_0.12.5-1.bionic_amd64.deb

RUN apt-get install -y openjdk-8-jdk

# Prepare timezone settings to be overriden by runtime variable e.g. -e TZ='Europe/Berlin'
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 3000

# @see https://spring.io/guides/gs/spring-boot-docker/
COPY  target/wkhtmltopdf.jar .
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","wkhtmltopdf.jar"]
