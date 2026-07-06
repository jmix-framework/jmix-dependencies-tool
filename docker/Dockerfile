# Base JDK image for the resolution build, driven by the target Jmix line: 3.x requires JDK 21 (the io.jmix
# Gradle plugin 3.0 refuses to run below 21), 2.x also works on 21, but 1.x needs <=17 (its Gradle 7.6 cannot
# run on JDK 21). The default covers current lines; for the 1.x line pass
# --build-arg BASE_IMAGE=eclipse-temurin:17-jdk-alpine.
# The whole reference is parameterized (not just the version) because Temurin's Alpine tags are not uniform:
# 21 is 'eclipse-temurin:21-alpine' while 17 is 'eclipse-temurin:17-jdk-alpine'.
ARG BASE_IMAGE=eclipse-temurin:21-alpine
FROM ${BASE_IMAGE}

ARG JMIX_VERSION=1.5.0
ARG JMIX_PLUGIN_VERSION=${JMIX_VERSION}
ARG DEPTOOL_VERSION=1.0.0
ARG JMIX_LICENSE_KEY

ENV JMIX_VERSION=${JMIX_VERSION}
ENV JMIX_PLUGIN_VERSION=${JMIX_PLUGIN_VERSION}
ENV DEPTOOL_VERSION=${DEPTOOL_VERSION}
ENV DEPTOOL_DIR=/opt/deptool
ENV DEPTOOL_EXPORT_DIR=/opt/deptool/export
ENV JMIX_LICENSE_KEY=${JMIX_LICENSE_KEY}

COPY scripts/* /opt/deptool-scripts/

RUN mkdir $DEPTOOL_DIR

RUN wget https://github.com/jmix-framework/jmix-dependencies-tool/releases/download/$DEPTOOL_VERSION/deptool-$DEPTOOL_VERSION.zip -q -O $DEPTOOL_DIR/deptool.zip \
    && apk update \
    && apk add zip \
    && apk add unzip \
    && unzip -q $DEPTOOL_DIR/deptool.zip -d $DEPTOOL_DIR \
    && rm $DEPTOOL_DIR/deptool.zip \
    && chmod +x /opt/deptool-scripts/*.sh

RUN chmod +x ${DEPTOOL_DIR}/deptool-${DEPTOOL_VERSION}/bin/deptool

RUN apk add --update nodejs-current npm

CMD ["sh", "-c", "/opt/deptool-scripts/entrypoint.sh"]