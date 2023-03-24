#!/bin/sh

cd ${DEPTOOL_DIR}/deptool-${DEPTOOL_VERSION}/bin

./deptool resolve-jmix --jmix-version ${JMIX_VERSION} --jmix-plugin-version ${JMIX_PLUGIN_VERSION}
./deptool export --report-file ${DEPTOOL_EXPORT_DIR}/jmix-dependencies-${JMIX_VERSION}.txt
cd ../export
zip -rq ${DEPTOOL_EXPORT_DIR}/jmix-dependencies-${JMIX_VERSION}.zip . *

cd ../bin

if [ -n "$JMIX_LICENSE_KEY" ]; then
  ./deptool resolve-jmix --jmix-version ${JMIX_VERSION} \
    --jmix-plugin-version ${JMIX_PLUGIN_VERSION} \
    --resolve-commercial-addons \
    --jmix-license-key ${JMIX_LICENSE_KEY}
  ./deptool export --report-file ${DEPTOOL_EXPORT_DIR}/jmix-commercial-dependencies-${JMIX_VERSION}.txt
  cd ../export
  zip -rq ${DEPTOOL_EXPORT_DIR}/jmix-commercial-dependencies-${JMIX_VERSION}.zip . *
fi