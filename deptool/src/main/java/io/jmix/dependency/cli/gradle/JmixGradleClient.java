package io.jmix.dependency.cli.gradle;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

public class JmixGradleClient {

    private String projectDir;

    private String gradleUserHomeDir;

    public JmixGradleClient(String projectDir, String gradleUserHomeDir) {
        this.projectDir = projectDir;
        this.gradleUserHomeDir = gradleUserHomeDir;
    }

    public ProjectConnection getProjectConnection() {
        //todo which gradle distribution to use
        return GradleConnector.newConnector()
                .forProjectDirectory(new File(projectDir))
                .connect();
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
