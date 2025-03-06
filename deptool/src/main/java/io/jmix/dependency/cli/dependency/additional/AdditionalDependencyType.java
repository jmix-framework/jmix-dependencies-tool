package io.jmix.dependency.cli.dependency.additional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;

import static io.jmix.dependency.cli.dependency.JmixDependencies.*;

public enum AdditionalDependencyType {
    NPM("npm");

    private static final String ADDITIONAL_DEPENDENCIES_RESOURCES_DIR = "jmix-dependencies/additional";
    private static final Logger log = LoggerFactory.getLogger(AdditionalDependencyType.class);
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
            return findFileForLatestVersion(jmixVersion, fileName);
        }
    }

    private InputStream findFileForMinorVersion(String jmixVersion, String fileName) {
        String minorVersion = getMinorVersion(jmixVersion);
        if (minorVersion == null || minorVersion.isBlank()) {
            return null;
        }
        return findResourceAsStream(minorVersion, fileName);
    }

    private InputStream findFileForLatestVersion(String jmixVersion, String fileName) {
        String latestVersion = getLatestVersion(jmixVersion);
        if (latestVersion == null || latestVersion.isBlank()) {
            return null;
        }
        return findResourceAsStream(latestVersion, fileName);
    }

    private InputStream findFileForPatchVersion(String jmixVersion, String fileName) {
        InputStream resource = null;
        try {
            int patch = Integer.parseInt(getPatchVersion(jmixVersion));
            for (int i = patch; i >= 0; i--) {
                if (resource != null) {
                    break;
                }
                resource = findResourceAsStream(jmixVersion, fileName);
            }
        } catch (NumberFormatException ignored) {
            resource = findResourceAsStream(jmixVersion, fileName);
        }

        if (resource == null) {
            return findFileForMinorVersion(jmixVersion, fileName);
        }

        return resource;
    }

    private InputStream findResourceAsStream(String jmixVersion, String fileName) {
        return getClassLoader().getResourceAsStream(getFilePath(jmixVersion, fileName));
    }

    private String getFilePath(String jmixVersion, String fileName) {
        return getResourceDir(jmixVersion) + "/" + fileName;
    }

    private String getResourceDir(String jmixVersion) {
        return getResourcesDir() + "/" + jmixVersion;
    }

    private String getResourcesDir() {
        return ADDITIONAL_DEPENDENCIES_RESOURCES_DIR + "/" + getDirectoryName();
    }

    private String getLatestVersion(String jmixVersion) {
        try {
            String minorVersion = getMinorVersion(jmixVersion);
            if (minorVersion == null || minorVersion.isBlank()) {
                return null;
            }

            URL resourcesDirUrl = getClassLoader().getResource(getResourcesDir());
            if (resourcesDirUrl == null) {
                return null;
            }

            File resourcesDir = Paths.get(resourcesDirUrl.toURI()).toFile();
            Collection<File> children = FileUtils.listFilesAndDirs(resourcesDir,
                    new RegexFileFilter("ignoreAll"),
                    new RegexFileFilter("[\\d.-]*"));

            File latestVersion = children.stream()
                    .filter(File::isDirectory)
                    .filter(dir -> dir.getName().startsWith(minorVersion))
                    .min((f1, f2) -> compareVersions(f1.getName(), f2.getName()))
                    .orElse(null);

            return latestVersion == null ? null : latestVersion.getName();
        } catch (Exception e) {
            log.warn("Can not find additional dependencies resources for latest version", e);
            return null;
        }
    }

    private ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }
}
