package io.jmix.dependency.cli.upload.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Class stores information about single NPM artifact.
 */
public class ArtifactNpm {

    private static final Logger log = LoggerFactory.getLogger(ArtifactNpm.class);

    private final String moduleName;
    private final String version;
    private final String asset;
    private final File file;

    private ArtifactNpm(String moduleName, String version, String asset, File file) {
        this.moduleName = moduleName;
        this.version = version;
        this.asset = asset;
        this.file = file;
    }

    public static ArtifactNpm createFromPackage(File packageFile) {
        log.info("Create artifact from package file: {}", packageFile);
        String packageFileName = packageFile.getName();

        File parentDirectory = packageFile.getParentFile();
        if (parentDirectory == null) {
            throw new RuntimeException("Package file doesn't have parent directory");
        }
        String moduleName = parentDirectory.getName(); //name of the parent directory is the name of module

        int extensionDelimiterIndex = packageFileName.lastIndexOf(".");
        String packageNameNoExtension = packageFileName.substring(0, extensionDelimiterIndex);

        // skip one more character - delimiter '-' between module name and version
        String version = packageNameNoExtension.substring(moduleName.length() + 1);
        log.info("Package={}, Module={}, Version={}", packageFileName, moduleName, version);
        return new ArtifactNpm(moduleName, version, packageFileName, packageFile);
    }

    /**
     * Name of the module, without a version
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Version of the module
     */
    public String getVersion() {
        return version;
    }

    /**
     * Artifact name (module & version)
     */
    public String getAsset() {
        return asset;
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
                "moduleName='" + moduleName + '\'' +
                ", version='" + version + '\'' +
                ", asset='" + asset + '\'' +
                '}';
    }
}
