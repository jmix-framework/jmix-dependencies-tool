package io.jmix.dependency.cli.upload.model;

import java.io.File;

/**
 * Class stores information about single maven artifact.
 */
public class Artifact {
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private File file;

    public Artifact(String groupId, String artifactId, String version, String classifier, File file) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.file = file;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        String fileName = file.getName();
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public File getFile() {
        return file;
    }
}
