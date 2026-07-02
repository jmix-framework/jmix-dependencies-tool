package io.jmix.dependency.cli.upload.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class store artifacts of the same dependency but with different classifiers (pom, jar, sources, etc.)
 */
public class ArtifactsBundle {
    private String groupId;
    private String artifactId;
    private String version;
    private List<Artifact> artifacts = new ArrayList<>();

    public ArtifactsBundle(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void addArtifact(Artifact artifact) {
        artifacts.add(artifact);
    }

    public String getMavenCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public String toString() {
        return getMavenCoordinates();
    }
}
