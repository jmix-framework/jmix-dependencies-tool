package io.jmix.dependency.cli.dependency;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenCoordinates {

    private static final String SEPARATOR = ":";

    private final String groupId;
    private final String artifactId;
    private final String version;

    private MavenCoordinates(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    private MavenCoordinates(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public static MavenCoordinates parse(String definition) {
        if (StringUtils.isBlank(definition)) {
            throw new RuntimeException("Unable to parse empty maven coordinates '" + definition + "'");
        }

        String[] split = definition.split(SEPARATOR);


        if (split.length == 2) {
            return new MavenCoordinates(split[0], split[1]);
        } else if (split.length == 3) {
            return new MavenCoordinates(split[0], split[1], split[2]);
        } else {
            // 'packaging' and 'classifier' are not supported
            throw new RuntimeException("Unable to parse maven coordinates '" + definition + "'");
        }
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

    public String toString() {
        return Stream.of(groupId, artifactId, version)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
    }
}
