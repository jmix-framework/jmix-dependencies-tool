package io.jmix.dependency.cli.dependency.additional;

import io.jmix.dependency.cli.version.JmixVersion;

import java.io.InputStream;

public enum AdditionalDependencyType {
    NPM("npm");

    private static final String ADDITIONAL_DEPENDENCIES_RESOURCES_DIR = "jmix-dependencies/additional";
    private final String directoryName;

    AdditionalDependencyType(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    InputStream findFileContent(JmixVersion jmixVersion, String fileName) {
        InputStream resource = null;
        try {
            if (!jmixVersion.isStable()) {
                for (int patch = 0; patch <= jmixVersion.patch(); patch++) {
                    if (resource != null) {
                        break;
                    }
                    String versionString = jmixVersion.withPatch(patch).versionString(false);
                    resource = findResourceAsStream(versionString, fileName);
                }
            } else {
                for (int patch = jmixVersion.patch(); patch >= 0; patch--) {
                    if (resource != null) {
                        break;
                    }
                    String versionString = jmixVersion.withPatch(patch).versionString(false);
                    resource = findResourceAsStream(versionString, fileName);
                }
            }
        } catch (NumberFormatException ignored) {
            resource = findResourceAsStream(jmixVersion.versionString(false), fileName);
        }

        return resource;
    }


    private InputStream findResourceAsStream(String jmixVersion, String fileName) {
        return getClassLoader().getResourceAsStream(getFilePath(jmixVersion, fileName));
    }

    private String getFilePath(String jmixVersion, String fileName) {
        return getResourcesDir(jmixVersion) + "/" + fileName;
    }

    private String getResourcesDir(String jmixVersion) {
        return getVersionsDir() + "/" + jmixVersion;
    }

    private String getVersionsDir() {
        return ADDITIONAL_DEPENDENCIES_RESOURCES_DIR + "/" + getDirectoryName();
    }

    private ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }
}
