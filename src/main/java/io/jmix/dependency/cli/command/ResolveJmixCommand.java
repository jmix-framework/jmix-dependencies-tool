package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.dependency.SubscriptionPlan;
import io.jmix.dependency.cli.gradle.GradleArgs;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static io.jmix.dependency.cli.dependency.DependencyScope.JVM;
import static io.jmix.dependency.cli.dependency.JmixDependencies.getVersionSpecificJmixDependencies;

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

    @Parameter(names = {"--workspace-dir"}, description = "Directory where the resolution Gradle project is generated",
            order = 3)
    private String workspaceDir;

    @Parameter(names = {"--resolve-commercial-addons"}, description = "Whether to resolve Jmix commercial add-ons. --jmix-license-key must be provided in this case.", order = 4)
    private boolean resolveCommercialAddons;

    @Parameter(names = {"--jmix-license-key"}, description = "Jmix license key (required for commercial add-ons resolution)", order = 5)
    private String jmixLicenseKey;

    @Parameter(names = {"--gradle-version"}, description = "What version of Gradle installation will be used", order = 6)
    private String gradleVersion;

    @Parameter(names = {"--public-repository"}, description = "Url for repository with public artifacts", order = 7)
    private String publicRepository;

    @Parameter(names = {"--premium-repository"}, description = "Url for repository with premium artifacts", order = 8)
    private String premiumRepository;

    @Parameter(names = {"--repository"}, description = "Additional Maven repository for dependencies resolution. The format is " +
            "the following: <url>|<username>|<password>, e.g. http://localhost:8081/jmix|admin|admin. " +
            "If credentials are not required then just an URL must be passed", order = 9)
    private List<String> repositories;

    @Parameter(names = {"--commercial-subscription-plan"},
            description = "Type of commercial subscription plan - 'enterprise' or 'bpm' (default). Relevant only if '--resolve-commercial-addons' is present",
            order = 10)
    private String commercialSubscriptionPlan;

    @Parameter(names = {"--no-sources"}, description = "Skip downloading -sources jars (faster resolution). Sources are included by default.", order = 11)
    private boolean noSources;

    @Parameter(names = {"--gradle-jvmargs"}, description = "Override the Gradle daemon JVM args (e.g. '-Xmx4g'). Default: "
            + JmixGradleClient.DEFAULT_GRADLE_JVM_ARGS, order = 12)
    private String gradleJvmArgs;

    @Override
    public void run() {
        JmixVersion parsedVersion = JmixVersion.from(jmixVersion);
        if (jmixPluginVersion == null) {
            jmixPluginVersion = jmixVersion;
        }
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        if (workspaceDir == null) {
            workspaceDir = DefaultPaths.getDefaultWorkspaceRoot();
        }
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromId(commercialSubscriptionPlan);

        log.info("Jmix version: {}", jmixVersion);
        log.info("Jmix plugin version: {}", jmixPluginVersion);
        log.info("Resolve commercial addons: {}", resolveCommercialAddons);
        log.info("Jmix commercial subscription plan: {}", subscriptionPlan);
        log.info("Gradle user home directory: {}", Paths.get(gradleUserHome).toAbsolutePath().normalize());

        Set<String> jmixDependencies = getVersionSpecificJmixDependencies(JVM, jmixVersion, resolveCommercialAddons, subscriptionPlan);
        List<String> modules = new ArrayList<>(jmixDependencies);
        modules.sort(Comparator.naturalOrder());
        log.info("Resolving {} Jmix module(s) in a single Gradle build", modules.size());

        // Generate the (single, unified) resolution project for this exact version, then resolve every
        // module in ONE build: each module in its own configuration (isolated transitive closure) plus the
        // "all modules together" classpath. This replaces the v1/v2 loop of ~2N+1 separate builds.
        WorkspaceManager workspaceManager = new WorkspaceManager(Paths.get(workspaceDir));
        String effectiveGradleVersion = workspaceManager.effectiveGradleVersion(parsedVersion, gradleVersion);
        log.info("Gradle version: {}", effectiveGradleVersion);
        Path projectDir = workspaceManager.prepare(parsedVersion, effectiveGradleVersion);

        GradleArgs gradleArgs = GradleArgs.create()
                .jmix(jmixVersion, jmixPluginVersion, jmixLicenseKey, publicRepository, premiumRepository, repositories)
                .modules(modules)
                .gradleJvmArgs(gradleJvmArgs)
                .flag("isolatedResolution")
                .raw("--stacktrace")
                .raw("--info");
        if (noSources) {
            gradleArgs.flag("skipSources");
        }

        new JmixGradleClient(projectDir, gradleUserHome).runTask("resolveAll", gradleArgs);
        log.info("Resolving Jmix dependencies completed successfully");
    }
}
