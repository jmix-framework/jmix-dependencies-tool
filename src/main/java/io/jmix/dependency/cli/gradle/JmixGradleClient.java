package io.jmix.dependency.cli.gradle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a Gradle task by invoking the workspace's generated Gradle <b>wrapper</b> as a subprocess.
 * <p>
 * v3 deliberately does NOT use the Gradle Tooling API: deptool only ever runs a task (it never fetches
 * Tooling models), and an embedded Tooling API would tie the tool to a single client version that cannot
 * span every framework line's Gradle (1.x→7.x, 2.x→8.x, 3.x→9.5). Driving the wrapper as a separate
 * process makes the daemon Gradle version whatever the wrapper's {@code distributionUrl} pins, decoupling
 * it from the tool entirely. The only requirement is that deptool's JVM can launch the daemon — JDK 17
 * covers Gradle 7.x through 9.x.
 */
public class JmixGradleClient {

    private static final Logger log = LoggerFactory.getLogger(JmixGradleClient.class);

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private final Path projectDir;
    private final String gradleUserHomeDir;

    public JmixGradleClient(Path projectDir, String gradleUserHomeDir) {
        this.projectDir = projectDir;
        this.gradleUserHomeDir = gradleUserHomeDir;
    }

    /**
     * Runs a single task via the wrapper, streaming output to the console. {@code taskArguments} should
     * contain task options and {@code -P} project properties; {@code --gradle-user-home} is added here.
     */
    public void runTask(String taskName, List<String> taskArguments) {
        List<String> command = new ArrayList<>();
        if (WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(projectDir.resolve("gradlew.bat").toString());
        } else {
            command.add(projectDir.resolve("gradlew").toString());
        }
        command.add(taskName);
        command.addAll(taskArguments);
        command.add("--gradle-user-home");
        command.add(gradleUserHomeDir);
        command.add("--console=plain");
        // One-shot batch tool: no lingering daemon to hold locks on the gradle-user-home that
        // `export` later walks, and re-runs with a different home are safe.
        command.add("--no-daemon");

        log.info("Running: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .inheritIO();

        int exitCode;
        try {
            Process process = builder.start();
            exitCode = process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Gradle wrapper in " + projectDir, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gradle task '" + taskName + "' was interrupted", e);
        }

        if (exitCode != 0) {
            throw new RuntimeException("Gradle task '" + taskName + "' failed with exit code " + exitCode);
        }
    }
}
