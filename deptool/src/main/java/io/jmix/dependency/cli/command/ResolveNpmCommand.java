package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.dependency.SubscriptionPlan;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import io.jmix.dependency.cli.version.JmixVersion;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static io.jmix.dependency.cli.dependency.DependencyScope.NPM;
import static io.jmix.dependency.cli.dependency.JmixDependencies.getVersionSpecificJmixDependencies;
import static io.jmix.dependency.cli.dependency.additional.AdditionalDependencyFileType.PACKAGE_LOCK;

@Parameters(commandDescription = "Resolves Npm dependencies")
public class ResolveNpmCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ResolveNpmCommand.class);

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

    private JmixVersion parsedVersion;
    private SubscriptionPlan subscriptionPlan;

    @Override
    public void run() {
        parsedVersion = JmixVersion.from(jmixVersion);

        if (jmixPluginVersion == null) {
            jmixPluginVersion = jmixVersion;
        }
        if (resolverProjectPath == null) {
            resolverProjectPath = DefaultPaths.getDefaultNpmResolverProjectPath();
        }
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        subscriptionPlan = SubscriptionPlan.fromId(this.commercialSubscriptionPlan);

        log.info("Jmix version: {}", jmixVersion);
        log.info("Jmix plugin version: {}", jmixPluginVersion);
        log.info("Resolve commercial addons: {}", resolveCommercialAddons);
        log.info("Jmix commercial subscription plan: {}", subscriptionPlan);
        log.info("Resolver project path: {}", Paths.get(resolverProjectPath).toAbsolutePath().normalize());
        log.info("Gradle user home directory: {}", Paths.get(gradleUserHome).toAbsolutePath().normalize());

        JmixGradleClient jmixGradleClient = new JmixGradleClient(resolverProjectPath, gradleUserHome, gradleVersion);

        vaadinClean(jmixGradleClient);
        copyStubPackageLock();
        resolveDependencies(jmixGradleClient);
        resolveAdditionalDependencies();
    }

    protected void vaadinClean(JmixGradleClient jmixGradleClient) {
        log.info("Vaadin clean...");

        try (ProjectConnection connection = jmixGradleClient.getProjectConnection()) {
            List<String> taskArguments = new ArrayList<>();
            taskArguments.add("-PjmixVersion=" + jmixVersion);
            taskArguments.add("-PjmixPluginVersion=" + jmixPluginVersion);
            if (jmixLicenseKey != null) {
                taskArguments.add("-PjmixLicenseKey=" + jmixLicenseKey);
            }
            if (publicRepository != null) {
                taskArguments.add("-PjmixPublicRepository=" + publicRepository);
            }
            if (premiumRepository != null) {
                taskArguments.add("-PjmixPremiumRepository=" + premiumRepository);
            }
            if (repositories != null) {
                // Generate additional repositories on build.gradle side because 'vaadinClean' task
                // can't accept --repository parameter like custom tasks
                String joinedRepos = String.join(",", repositories);
                taskArguments.add("-PextraRepositories=" + joinedRepos);
            }
            String result = jmixGradleClient.runTask(connection,
                    "vaadinClean",
                    taskArguments);
            log.info(result);
        }
    }

    protected void copyStubPackageLock() {
        log.info("-= Copy stub package-lock.json =-");
        File source = new File(resolverProjectPath + "/stub/package-lock.json");
        File targetDirectory = new File(resolverProjectPath);
        try {
            FileUtils.copyFileToDirectory(source, targetDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy stub package-lock.json", e);
        }
    }

    protected void resolveDependencies(JmixGradleClient jmixGradleClient) {
        log.info("-= Resolve dependencies =-");
        try (ProjectConnection connection = jmixGradleClient.getProjectConnection()) {
            Set<String> jmixDependencies = getVersionSpecificJmixDependencies(NPM, jmixVersion, resolveCommercialAddons, subscriptionPlan);
            List<String> dependencies = new ArrayList<>(jmixDependencies);
            dependencies.sort(Comparator.naturalOrder());

            log.info("Resolve...");
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
            if (publicRepository != null) {
                taskArguments.add("-PjmixPublicRepository=" + publicRepository);
            }
            if (premiumRepository != null) {
                taskArguments.add("-PjmixPremiumRepository=" + premiumRepository);
            }
            if (repositories != null) {
                for (String repository : repositories) {
                    taskArguments.add("--repository");
                    taskArguments.add(repository);
                }
            }
            String result = jmixGradleClient.runTask(connection,
                    "resolveNpmDependencies",
                    taskArguments);
            log.info(result);
        }
    }

    private void resolveAdditionalDependencies() {
        try {
            File additionalDependenciesDir = Paths.get(resolverProjectPath,
                    "additional-dependencies").toAbsolutePath().normalize().toFile();
            if (additionalDependenciesDir.exists()) {
                FileUtils.cleanDirectory(additionalDependenciesDir);
            } else if (!additionalDependenciesDir.mkdir()) {
                return;
            }

            resolveAdditionalPackageLockFile(additionalDependenciesDir);

        } catch (Exception e) {
            log.info("Error when trying to download additional dependencies", e);
        }
    }

    private void resolveAdditionalPackageLockFile(File additionalDependenciesDir) throws IOException {
        String packageLockFileName = PACKAGE_LOCK.getFileName();

        try (InputStream packageLockContent = PACKAGE_LOCK.findFileContent(parsedVersion)) {
            if (packageLockContent == null) {
                log.info("-= No additional dependencies was found, skipping this step =-");
                return;
            }

            log.info("-= Resolve additional dependencies =-");

            try {
                File packageLockJson = new File(additionalDependenciesDir, packageLockFileName);
                //noinspection ResultOfMethodCallIgnored
                packageLockJson.createNewFile();
                log.info("-= Copy additional dependencies files =-");
                FileUtils.copyInputStreamToFile(packageLockContent, packageLockJson);
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy additional dependencies file: " + packageLockFileName, e);
            }
        }
    }

}
