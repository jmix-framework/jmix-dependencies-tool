package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Resolves a specific library dependencies")
public class ResolveLibCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ResolveLibCommand.class);

    @Parameter(description = "Dependency to resolve", order = 0)
    private String dependency;

    @Parameter(names = {"--jmix-version"}, description = "Jmix version", order = 2)
    private String jmixVersion;

    @Parameter(names = {"--jmix-plugin-version"}, description = "Jmix plugin version", order = 3)
    private String jmixPluginVersion;

    @Parameter(names = {"--gradle-user-home"}, description = "Gradle user home dir for resolver project", order = 4)
    private String gradleUserHome;

    @Parameter(names = {"--resolver-project"}, description = "Path to dependencies resolver project", order = 5)
    private String resolverProjectPath;

    @Parameter(names = {"--jmix-license-key"}, description = "Jmix license key (required if the project uses commercial add-ons", order = 6)
    private String jmixLicenseKey;

    @Parameter(names = {"--repository"}, description = "Additional Maven repository for dependencies resolution. The format is " +
            "the following: <url>|<username>|<password>, e.g. http://localhost:8081/jmix|admin|admin. " +
            "If credentials are not required then just an URL must be passed", order = 7)
    private List<String> repositories;

    @Parameter(names = {"--gradle-version"}, description = "What version of Gradle installation will be used", order = 8)
    private String gradleVersion;

    @Override
    public void run() {
        if (jmixPluginVersion == null) {
            jmixPluginVersion = jmixVersion;
        }
        if (resolverProjectPath == null) {
            resolverProjectPath = DefaultPaths.getDefaultResolverProjectPath();
        }
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        log.info("Jmix version: {}", jmixVersion);
        log.info("Jmix plugin version: {}", jmixPluginVersion);
        log.info("Resolver project path: {}", Paths.get(resolverProjectPath).toAbsolutePath().normalize());
        log.info("Gradle user home directory: {}", Paths.get(gradleUserHome).toAbsolutePath().normalize());
        log.info("Dependency: {}", dependency);

        JmixGradleClient jmixGradleClient = new JmixGradleClient(resolverProjectPath, gradleUserHome, gradleVersion);
        try (ProjectConnection connection = jmixGradleClient.getProjectConnection()) {
            log.info("Resolving dependency: {}", dependency);
            List<String> taskArguments = new ArrayList<>(List.of(
                    "--dependency", dependency));
            if (jmixVersion != null) {
                taskArguments.add("-PjmixVersion=" + jmixVersion);
                taskArguments.add("-PjmixPluginVersion=" + jmixPluginVersion);
            }
            if (jmixLicenseKey != null) {
                taskArguments.add("-PjmixLicenseKey=" + jmixLicenseKey);
            }
            if (repositories != null) {
                for (String repository : repositories) {
                    taskArguments.add("--repository");
                    taskArguments.add(repository);
                }
            }
            String result = jmixGradleClient.runTask(connection,
                    "resolveDependencies",
                    taskArguments);
            log.debug(result);
        }
        log.info("Resolving a dependency completed successfully");
    }
}
