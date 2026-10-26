package io.jmix.dependency.cli.upload.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Class stores information about single NPM artifact.
 */
public class ArtifactNpm {

    private static final Logger log = LoggerFactory.getLogger(ArtifactNpm.class);

    private static final String EXTENSION = ".tgz";

    private final String name;
    private final String version;
    private final File file;

    private ArtifactNpm(String name, String version, File file) {
        this.name = name;
        this.version = version;
        this.file = file;
    }

    /**
     * Creates an artifact from a tarball in the export-npm layout: {@code <name>/<name>-<version>.tgz} for an
     * unscoped package and {@code @<scope>/<name>/<name>-<version>.tgz} for a scoped one. The scope is part of the
     * package identity - {@code react} and {@code @types/react} can share the file name {@code react-19.3.0.tgz}.
     *
     * @param artifactsDir root directory of the exported npm artifacts
     * @param packageFile  tarball inside {@code artifactsDir}
     * @throws IllegalArgumentException if the tarball does not follow the layout above
     */
    public static ArtifactNpm createFromPackage(Path artifactsDir, Path packageFile) {
        log.debug("Create artifact from package file: {}", packageFile);
        Path relative = artifactsDir.relativize(packageFile);
        int depth = relative.getNameCount();
        String scope = depth == 3 ? relative.getName(0).toString() : null;
        String unscopedName = depth == 2 || depth == 3 ? relative.getName(depth - 2).toString() : null;
        String fileName = relative.getFileName().toString();

        String prefix = unscopedName + "-";
        String version = unscopedName != null && fileName.startsWith(prefix) && fileName.endsWith(EXTENSION)
                ? fileName.substring(prefix.length(), fileName.length() - EXTENSION.length())
                : null;
        // A version starts with a digit; otherwise the prefix matched a longer name, e.g. react/react-dom-19.3.0.tgz
        if (version == null || version.isEmpty() || !Character.isDigit(version.charAt(0))
                || (scope != null && !scope.startsWith("@"))) {
            throw new IllegalArgumentException("Unexpected npm tarball location '" + relative + "', expected "
                    + "<name>/<name>-<version>.tgz or @<scope>/<name>/<name>-<version>.tgz");
        }

        String name = scope != null ? scope + "/" + unscopedName : unscopedName;
        log.debug("Package={}, Name={}, Version={}", fileName, name, version);
        return new ArtifactNpm(name, version, packageFile.toFile());
    }

    /**
     * Full package name, including the scope for a scoped package, e.g. {@code @types/react}
     */
    public String getName() {
        return name;
    }

    /**
     * Package name without the scope, e.g. {@code react} for {@code @types/react}
     */
    public String getUnscopedName() {
        return name.substring(name.indexOf('/') + 1);
    }

    /**
     * Version of the module
     */
    public String getVersion() {
        return version;
    }

    /**
     * Unique package identity, e.g. {@code @types/react@19.3.0}
     */
    public String getKey() {
        return name + "@" + version;
    }

    /**
     * Path of the tarball in a Nexus npm repository, e.g. {@code @types/react/-/react-19.3.0.tgz}
     */
    public String getRepositoryPath() {
        return name + "/-/" + getUnscopedName() + "-" + version + EXTENSION;
    }

    /**
     * Whether an asset path reported by Nexus (with or without a leading slash) is this tarball
     */
    public boolean matchesRepositoryPath(String path) {
        if (path == null) {
            return false;
        }
        return (path.startsWith("/") ? path.substring(1) : path).equals(getRepositoryPath());
    }

    /**
     * Content
     */
    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "ArtifactNpm{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", file='" + file + '\'' +
                '}';
    }
}
