package io.jmix.dependency.cli.dependency.additional;

import java.io.InputStream;

import static io.jmix.dependency.cli.dependency.JmixDependencies.*;

public enum AdditionalDependencyType {
    NPM("npm");

    private final String directoryName;

    AdditionalDependencyType(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    InputStream findFileContent(String jmixVersion, String fileName) {
        if (jmixVersion == null || jmixVersion.isBlank()) {
            return null;
        }

        if (hasPatch(jmixVersion)) {
            return findFileForPatchVersion(jmixVersion, fileName);
        } else {
            return findFileForMinorVersion(jmixVersion, fileName);
        }
    }

    private InputStream findFileForMinorVersion(String jmixVersion, String fileName) {
        String minorVersion = getMinorVersion(jmixVersion);
        if (minorVersion == null || minorVersion.isBlank()) {
            return null;
        }
        return findResourceAsStream(jmixVersion, fileName);
    }

    private InputStream findFileForPatchVersion(String jmixVersion, String fileName) {
        InputStream resource = null;
        try {
            int patch = Integer.parseInt(getPatchVersion(jmixVersion));
            for (int i = patch; i > 0; i--) {
                if (resource != null) {
                    break;
                }
                resource = findResourceAsStream(jmixVersion, fileName);
            }
        } catch (NumberFormatException ignored) {
            resource = findResourceAsStream(jmixVersion, fileName);
        }
        return resource;
    }

    private InputStream findResourceAsStream(String jmixVersion, String fileName) {
        return getClassLoader().getResourceAsStream(getFilePath(jmixVersion, fileName));
    }

    private String getFilePath(String jmixVersion, String fileName) {
        return "jmix-dependencies/additional/%s/%s/%s"
                .formatted(getDirectoryName(), jmixVersion, fileName);
    }

    private ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }
}
