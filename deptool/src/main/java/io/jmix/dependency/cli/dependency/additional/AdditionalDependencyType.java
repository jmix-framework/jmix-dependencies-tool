package io.jmix.dependency.cli.dependency.additional;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.version.JmixVersionUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.filter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
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
     *     <li>1.5.0 and earlier -> null</li>
     *     <br>
     *     <li>2.0.0 -> null</li>
     *     <br>
     *     <li>2.5.0 -> 2.5.5</li>
     *     <li>2.5.2 -> 2.5.5</li>
     *     <li>2.5.9 -> 2.5.5</li>
     *     <li>2.5.999-SNAPSHOT -> 2.5.5</li>
     *     <br>
     *     <li>3.0.0 -> 3.0.0</li>
     *     <li>3.0.1 -> 3.0.0</li>
     *     <li>3.0.999-SNAPSHOT -> 3.0.0</li>
     *     <br>
     *     <li>3.1.0 and later -> null</li>
     * <ul/>
     */
    private String findLatestVersionForMinor(JmixVersion jmixVersion) {
        try {
            String versionsDir = getVersionsDir();
            URL resourcesDirUrl = getClassLoader().getResource(versionsDir);
            if (resourcesDirUrl == null) {
                log.warn("Directory {} not found", versionsDir);
                return null;
            }

            FileSystemManager fsManager = VFS.getManager();
            if (fsManager == null) {
                log.warn("Can not resolve {}, work with file system is unavailable", FileSystemManager.class.getCanonicalName());
                return null;
            }

            FileObject resourceDirFileObject = fsManager.resolveFile(resourcesDirUrl);
            if (resourceDirFileObject == null) {
                log.warn("Can not resolve {} to {}", resourcesDirUrl, FileObject.class.getCanonicalName());
                return null;
            }

            FileFilterSelector selector = new FileFilterSelector(new RegexFileFilter("[\\d.-]*"));

            List<String> latestVersion = Arrays.stream(resourceDirFileObject.findFiles(selector))
                    .filter(it -> {
                        try {
                            return it.isFolder();
                        } catch (FileSystemException e) {
                            return false;
                        }
                    })
                    .map(it -> it.getName().getBaseName())
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
