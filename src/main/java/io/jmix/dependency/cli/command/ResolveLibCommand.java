package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.gradle.GradleArgs;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Parameter(names = {"--workspace-dir"}, description = "Directory where the resolution Gradle project is generated", order = 5)
    private String workspaceDir;

    @Parameter(names = {"--jmix-license-key"}, description = "Jmix license key (required if the project uses commercial add-ons", order = 6)
    private String jmixLicenseKey;

    @Parameter(names = {"--gradle-version"}, description = "What version of Gradle installation will be used", order = 7)
    private String gradleVersion;

    @Parameter(names = {"--public-repository"}, description = "Url for repository with public artifacts", order = 8)
    private String publicRepository;

    @Parameter(names = {"--premium-repository"}, description = "Url for repository with premium artifacts", order = 9)
    private String premiumRepository;

    @Parameter(names = {"--repository"}, description = "Additional Maven repository for dependencies resolution. The format is " +
            "the following: <url>|<username>|<password>, e.g. http://localhost:8081/jmix|admin|admin. " +
            "If credentials are not required then just an URL must be passed", order = 10)
    private List<String> repositories;

    @Parameter(names = {"--no-sources"}, description = "Skip downloading -sources jars (faster resolution). Sources are included by default.", order = 11)
    private boolean noSources;

    @Parameter(names = {"--gradle-jvmargs"}, description = "Override the Gradle daemon JVM args (e.g. '-Xmx4g'). Default: "
            + JmixGradleClient.DEFAULT_GRADLE_JVM_ARGS, order = 12)
    private String gradleJvmArgs;

    @Override
    public void run() {
        if (jmixPluginVersion == null) {
            jmixPluginVersion = jmixVersion;
        }
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        if (workspaceDir == null) {
            workspaceDir = DefaultPaths.getDefaultWorkspaceRoot();
        }

        // jmixVersion is optional for resolve-lib. When present the Jmix BOM is applied during resolution;
        // when absent the dependency is resolved with its own explicit version and transitive closure.
        JmixVersion parsedVersion = jmixVersion != null ? JmixVersion.from(jmixVersion) : null;

        log.info("Jmix version: {}", jmixVersion);
        log.info("Gradle user home directory: {}", Paths.get(gradleUserHome).toAbsolutePath().normalize());
        log.info("Dependency: {}", dependency);

        WorkspaceManager workspaceManager = new WorkspaceManager(Paths.get(workspaceDir));
        String effectiveGradleVersion = workspaceManager.effectiveGradleVersion(parsedVersion, gradleVersion);
        log.info("Gradle version: {}", effectiveGradleVersion);
        Path projectDir = workspaceManager.prepare(parsedVersion, effectiveGradleVersion);

        GradleArgs gradleArgs = GradleArgs.create()
                .jmix(jmixVersion, jmixPluginVersion, jmixLicenseKey, publicRepository, premiumRepository, repositories)
                .modules(List.of(dependency))
                .gradleJvmArgs(gradleJvmArgs)
                .flag("isolatedResolution")
                .raw("--stacktrace")
                .raw("--info");
        if (noSources) {
            gradleArgs.flag("skipSources");
        }

        new JmixGradleClient(projectDir, gradleUserHome).runTask("resolveAll", gradleArgs);
        log.info("Resolving a dependency completed successfully");
    }
}
