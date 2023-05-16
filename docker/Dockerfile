FROM eclipse-temurin:11-jdk-alpine

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

CMD ["sh", "-c", "/opt/deptool-scripts/entrypoint.sh"]