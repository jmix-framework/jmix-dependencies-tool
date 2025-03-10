package io.jmix.dependency.cli.dependency.additional;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.version.JmixVersionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum AdditionalDependencyType {
    NPM("npm");

    private static final Logger log = LoggerFactory.getLogger(AdditionalDependencyType.class);

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
            if (jmixVersion.isSnapshot()) {
                String latestVersion = findLatestVersionForMinor(jmixVersion);
                if (latestVersion != null) {
                    resource = findResourceAsStream(latestVersion, fileName);
                }
            } else {
                for (int patch = jmixVersion.patch(); patch >= 0; patch--) {
                    String versionString = jmixVersion.withPatch(patch).versionString(false);
                    resource = findResourceAsStream(versionString, fileName);
                    if (resource != null) {
                        break;
                    }
                }
            }
        } catch (NumberFormatException ignored) {
            resource = findResourceAsStream(jmixVersion.versionString(false), fileName);
        }

        return resource;
    }

    /**
     * Find the latest version in minor range.
     * For example, we have 2.5.0, 2.5.5, 2.6.0, 3.0.0 versions, then:
     * <ul>
     *     <li>1.5.0 -> null</li>
     *     <li>2.0.0 -> null</li>
     *     <li>2.5.0 -> 2.5.5</li>
     *     <li>2.5.2 -> 2.5.5</li>
     *     <li>2.5.9 -> 2.5.5</li>
     *     <li>3.0.0-> 3.0.0</li>
     *     <li>3.1.0 -> null</li>
     * <ul/>
     */
    private String findLatestVersionForMinor(JmixVersion jmixVersion) {
        try {
            URL resourcesDirUrl = getClassLoader().getResource(getVersionsDir());
            if (resourcesDirUrl == null) {
                return null;
            }

            File resourcesDir = Paths.get(resourcesDirUrl.toURI()).toFile();
            Collection<File> children = FileUtils.listFilesAndDirs(resourcesDir,
                    new RegexFileFilter("ignoreAll"),
                    new RegexFileFilter("[\\d.-]*"));

            List<String> latestVersion = children.stream()
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .filter(name -> name.startsWith(jmixVersion.majorMinor()))
                    .collect(Collectors.toList());

            JmixVersionUtils.sort(latestVersion);

            return latestVersion.stream().findFirst().orElse(null);
        } catch (Exception e) {
            log.warn("Can not find additional dependencies resources for latest version", e);
            return null;
        }
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
