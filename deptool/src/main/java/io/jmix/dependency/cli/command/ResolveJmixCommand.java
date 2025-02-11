package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.dependency.JmixDependencies;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Parameters(commandDescription = "Resolves Jmix dependencies")
public class ResolveJmixCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ResolveJmixCommand.class);

    @Parameter(names = {"--jmix-version"}, description = "Jmix version", required = true, order = 0)
    private String jmixVersion;

    @Parameter(names = {"--jmix-plugin-version"}, description = "Jmix plugin version", order = 1)
    private String jmixPluginVersion;

    @Parameter(names = {"--gradle-user-home"}, description = "Directory where Gradle will put resolved dependencies",
            order = 2)
    private String gradleUserHome;

    @Parameter(names = {"--resolver-project"}, description = "Path to dependencies resolver project", order = 3)
    private String resolverProjectPath;

    @Parameter(names = {"--resolve-commercial-addons"}, description = "Whether to resolve Jmix commercial add-ons. --jmix-license-key must be provided in this case.", order = 4)
    private boolean resolveCommercialAddons;

    @Parameter(names = {"--jmix-license-key"}, description = "Jmix license key (required for commercial add-ons resolution)", order = 5)
    private String jmixLicenseKey;

    @Parameter(names = {"--gradle-version"}, description = "What version of Gradle installation will be used", order = 6)
    private String gradleVersion;

    @Parameter(names = {"--repository"}, description = "Additional Maven repository for dependencies resolution. The format is " +
            "the following: <url>|<username>|<password>, e.g. http://localhost:8081/jmix|admin|admin. " +
            "If credentials are not required then just an URL must be passed", order = 7)
    private List<String> repositories;

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

        JmixGradleClient jmixGradleClient = new JmixGradleClient(resolverProjectPath, gradleUserHome, gradleVersion);
        try (ProjectConnection connection = jmixGradleClient.getProjectConnection()) {
            List<String> dependencies = new ArrayList<>();
            dependencies.addAll(JmixDependencies.getVersionSpecificJmixDependencies(jmixVersion, resolveCommercialAddons));
            dependencies.sort(Comparator.naturalOrder());
            for (String dependency : dependencies) {
                log.info("Resolving dependency: {}", dependency);
                List<String> taskArguments = new ArrayList<>(List.of(
                        "--dependency", dependency));
                if (jmixLicenseKey != null) {
                    taskArguments.add("-PjmixLicenseKey=" + jmixLicenseKey);
                }
                if (repositories != null) {
                    for (String repository : repositories) {
                        taskArguments.add("--repository");
                        taskArguments.add(repository);
                    }
                }

                long colonCount = dependency.chars().filter(ch -> ch == ':').count();
                if (colonCount == 2) {
                    //if dependency in the dependencies file is defined with an explicit version then we resolve it
                    //twice: using Jmix BOM and without it
                    String result = jmixGradleClient.runTask(connection,
                            "resolveDependencies",
                            taskArguments);
                    log.debug(result);

                    taskArguments.add("-PjmixVersion=" + jmixVersion);
                    taskArguments.add("-PjmixPluginVersion=" + jmixPluginVersion);
                    result = jmixGradleClient.runTask(connection,
                            "resolveDependencies",
                            taskArguments);
                    log.debug(result);
                } else {
                    //if dependency in the dependencies file is defined without an explicit version then we resolve it
                    //using Jmix BOM
                    taskArguments.add("-PjmixVersion=" + jmixVersion);
                    taskArguments.add("-PjmixPluginVersion=" + jmixPluginVersion);
                    String result = jmixGradleClient.runTask(connection,
                            "resolveDependencies",
                            taskArguments);
                    log.debug(result);
                }
            }

            //some dependencies may include BOM files that affect other dependencies, e.g. elasticsearch includes the
            //newer version of jackson-databind that depends on com.fasterxml.jackson:jackson-bom that in turn may raise
            //versions of all jackson libraries. That's why we do a final resolution when all dependencies
            //from dependencies-X.Y.Z.xml file are passed to the build.gradle
            List<String> taskArguments = new ArrayList<>();
            for (String dependency : dependencies) {
                taskArguments.add("--dependency");
                taskArguments.add(dependency);
            }
            taskArguments.add("-PjmixVersion=" + jmixVersion);
            taskArguments.add("-PjmixPluginVersion=" + jmixPluginVersion);
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
        log.info("Resolving Jmix dependencies completed successfully");
    }
}
