FROM java:8

MAINTAINER Andreas Hahn <ahahn@gmx.net>

ENV WKHTML_VERSION 0.12.4

# Builds the wkhtmltopdf download URL based on version numbers above
ENV DOWNLOAD_URL "https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/${WKHTML_VERSION}/wkhtmltox-${WKHTML_VERSION}_linux-generic-amd64.tar.xz"

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    wget $DOWNLOAD_URL && \
    tar vxf wkhtmltox-${WKHTML_VERSION}_linux-generic-amd64.tar.xz && \
    cp wkhtmltox/bin/wk* /usr/local/bin/ && \
    cp wkhtmltox/lib/* /usr/local/lib/ && \
    rm wkhtmltox-${WKHTML_VERSION}_linux-generic-amd64.tar.xz

# Prepare timezone settings to be overriden by runtime variable e.g. -e TZ='Europe/Berlin'
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 3000

# @see https://spring.io/guides/gs/spring-boot-docker/
COPY  target/wkhtmltopdf.jar .
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","wkhtmltopdf.jar"]