#!/bin/sh

cd ${DEPTOOL_DIR}/deptool-${DEPTOOL_VERSION}/bin

./deptool resolve-jmix --jmix-version ${JMIX_VERSION} --jmix-plugin-version ${JMIX_PLUGIN_VERSION}
./deptool export
cd ../export
zip -rq ${DEPTOOL_EXPORT_DIR}/jmix-dependencies-${JMIX_VERSION}.zip . *

# ./deptool resolve-jmix --jmix-version ${JMIX_VERSION} --resolve-commercial-addons --jmix-license-key ${JMIX_LICENSE_KEY}
# ./deptool export
# zip -r ${DEPTOOL_EXPORT_DIR}/jmix-dependencies-commercial-${JMIX_VERSION}.zip ../export
