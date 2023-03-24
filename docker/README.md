# Docker Image For Jmix Dependencies Resolution

The container will download the `deptool` from GitHub releases, resolve Jmix dependencies and export them into zip archive.

After running the following command the mounted `./export` directory will contain the `jmix-dependencies-<JMIX_VERSION>.zip` archive.

```
docker run --rm -v $(pwd)/export:/opt/deptool/export \
    -it $(docker build -q --build-arg JMIX_VERSION=1.5.0 \
    --build-arg JMIX_PLUGIN_VERSION=1.5.0 \
    --build-arg DEPTOOL_VERSION=1.0.0 .)
```

If you also need to resolve commercial add-ons then pass the JMIX_LICENSE_KEY build argument. In this case the `./export` directory will additionally contain the `jmix-commercial-dependencies-<JMIX_VERSION>.zip` archive.

```
docker run --rm -v $(pwd)/export:/opt/deptool/export \
    -it $(docker build -q --build-arg JMIX_VERSION=1.5.0 \
    --build-arg JMIX_PLUGIN_VERSION=1.5.0 \
    --build-arg DEPTOOL_VERSION=1.0.1 \
    --build-arg JMIX_LICENSE_KEY=<your-license-key> .)
```