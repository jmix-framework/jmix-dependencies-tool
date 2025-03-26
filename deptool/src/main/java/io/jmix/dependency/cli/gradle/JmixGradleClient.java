package io.jmix.dependency.cli.gradle;

import io.jmix.dependency.cli.util.StringUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class JmixGradleClient {

    private final String projectDir;

    private final String gradleUserHomeDir;

    private final String gradleVersion;

    public JmixGradleClient(String projectDir, String gradleUserHomeDir) {
        this(projectDir, gradleUserHomeDir, null);
    }

    public JmixGradleClient(String projectDir, String gradleUserHomeDir, String gradleVersion) {
        this.projectDir = projectDir;
        this.gradleUserHomeDir = gradleUserHomeDir;
        this.gradleVersion = gradleVersion;
    }

    public ProjectConnection getProjectConnection() {
        GradleConnector gradleConnector = GradleConnector.newConnector().forProjectDirectory(new File(projectDir));
        if (StringUtils.isNotBlank(gradleVersion)) {
            gradleConnector.useGradleVersion(gradleVersion);
        }

        return gradleConnector.connect();
    }

    public String runTask(ProjectConnection connection, String taskName,
                          Iterable<String> taskArguments) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        connection.newBuild()
                .withArguments(taskName)
                .addArguments(taskArguments)
                .addArguments("--gradle-user-home", gradleUserHomeDir)
                .setStandardOutput(bos)
                .run();

        return bos.toString();
    }
}
