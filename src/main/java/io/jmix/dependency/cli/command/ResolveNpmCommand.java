package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.dependency.SubscriptionPlan;
import io.jmix.dependency.cli.gradle.GradleArgs;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import io.jmix.dependency.cli.npm.PackageLock;
import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.WorkspaceManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jmix.dependency.cli.dependency.DependencyScope.NPM;
import static io.jmix.dependency.cli.dependency.JmixDependencies.getVersionSpecificJmixDependencies;

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

    @Parameter(names = {"--workspace-dir"}, description = "Directory where the npm resolution Gradle project is generated",
            order = 3)
    private String workspaceDir;

    @Parameter(names = {"--package-lock-output"}, description = "Where the resolved package-lock.json is copied to",
            order = 4)
    private String packageLockOutput;

    @Parameter(names = {"--resolve-commercial-addons"}, description = "Whether to resolve Jmix commercial add-ons. --jmix-license-key must be provided in this case.", order = 5)
    private boolean resolveCommercialAddons;

    @Parameter(names = {"--jmix-license-key"}, description = "Jmix license key (required for commercial add-ons resolution)", order = 6)
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

    @Parameter(names = {"--commercial-subscription-plan"},
            description = "Type of commercial subscription plan - 'enterprise' or 'bpm' (default). Relevant only if '--resolve-commercial-addons' is present",
            order = 11)
    private String commercialSubscriptionPlan;

    @Override
    public void run() {
        JmixVersion parsedVersion = JmixVersion.from(jmixVersion);
        if (parsedVersion.major() < 2) {
            throw new RuntimeException("NPM resolution is only supported for Jmix 2.x and later "
                    + "(Jmix 1.x has no Vaadin frontend). Requested version: " + jmixVersion);
        }
        if (jmixPluginVersion == null) {
            jmixPluginVersion = jmixVersion;
        }
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        if (workspaceDir == null) {
            workspaceDir = DefaultPaths.getDefaultWorkspaceRoot();
        }
        if (packageLockOutput == null) {
            packageLockOutput = DefaultPaths.getDefaultNpmLockFile();
        }
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromId(commercialSubscriptionPlan);

        log.info("Jmix version: {}", jmixVersion);
        log.info("Jmix plugin version: {}", jmixPluginVersion);
        log.info("Resolve commercial addons: {}", resolveCommercialAddons);

        Set<String> npmModules = getVersionSpecificJmixDependencies(NPM, jmixVersion, resolveCommercialAddons, subscriptionPlan);
        List<String> modules = new ArrayList<>(npmModules);
        modules.sort(Comparator.naturalOrder());

        WorkspaceManager workspaceManager = new WorkspaceManager(Paths.get(workspaceDir));
        String effectiveGradleVersion = workspaceManager.effectiveGradleVersion(parsedVersion, gradleVersion);
        log.info("Gradle version: {}", effectiveGradleVersion);
        Path projectDir = workspaceManager.prepare(parsedVersion, effectiveGradleVersion);
        seedPackageLockStub(projectDir);

        GradleArgs args = GradleArgs.create()
                .jmix(jmixVersion, jmixPluginVersion, jmixLicenseKey, publicRepository, premiumRepository, repositories)
                .modules(modules)
                .raw("--stacktrace")
                .raw("--info");

        // Vaadin generates package.json, runs npm install (which populates the seeded package-lock.json),
        // builds the bundle, then deletes the generated frontend files — but it leaves the pre-existing lock.
        new JmixGradleClient(projectDir, gradleUserHome).runTask("vaadinBuildFrontend", args);

        copyResolvedLock(projectDir);
        extractDevBundleLock();
        log.info("Resolving npm dependencies completed successfully");
    }

    /**
     * Extracts {@code package-lock.json} from the resolved {@code vaadin-dev-bundle} jar (the framework's
     * FROZEN npm versions — e.g. dompurify 3.4.5) into a sibling of the project lock, so export-npm mirrors
     * those alongside the freshly resolved set. Best effort: a missing jar only warns (the project lock from
     * vaadinBuildFrontend still drives the export). The jar version follows whatever the Vaadin/Jmix BOM
     * resolved — found by name, not hard-coded — so it stays correct across framework versions.
     */
    private void extractDevBundleLock() {
        Path cacheDir = Paths.get(gradleUserHome, "caches", "modules-2", "files-2.1");
        if (!Files.isDirectory(cacheDir)) {
            log.warn("Gradle cache not found at {}; skipping vaadin-dev-bundle lock extraction", cacheDir);
            return;
        }
        Path jar;
        try (Stream<Path> walk = Files.walk(cacheDir)) {
            jar = walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("vaadin-dev-bundle-") && n.endsWith(".jar")
                                && !n.endsWith("-sources.jar") && !n.endsWith("-javadoc.jar");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("Error searching for the vaadin-dev-bundle jar: {}", e.getMessage());
            return;
        }
        if (jar == null) {
            log.warn("vaadin-dev-bundle jar not found in the Gradle cache — the framework's frozen npm versions "
                    + "will not be collected; only the freshly resolved project lock will be used.");
            return;
        }

        Path out = Paths.get(packageLockOutput).toAbsolutePath().normalize()
                .resolveSibling("dev-bundle-package-lock.json");
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry("vaadin-dev-bundle/package-lock.json");
            if (entry == null) {
                log.warn("No vaadin-dev-bundle/package-lock.json inside {}; skipping", jar.getFileName());
                return;
            }
            Files.createDirectories(out.getParent());
            try (InputStream is = jarFile.getInputStream(entry)) {
                Files.copy(is, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Extracted frozen npm versions from {} to {}", jar.getFileName(), out);
        } catch (IOException e) {
            log.warn("Could not extract vaadin-dev-bundle package-lock.json from {}: {}", jar, e.getMessage());
        }
    }

    /**
     * Pre-creates an (empty) package-lock.json at the project root before the frontend build.
     * <p>
     * Vaadin's frontend build generates the lockfile during the task but then deletes its generated frontend
     * files — UNLESS the file already existed. So we seed a stub: {@code npm install} populates it in place,
     * and Vaadin leaves it because it pre-existed. (This is the same trick v1/v2 used with its
     * stub/package-lock.json.) npm rewrites the contents and upgrades the lockfile version on install, so the
     * stub's own version is just a placeholder.
     */
    private void seedPackageLockStub(Path projectDir) {
        String stub = "{\n"
                + "  \"name\": \"deptool-resolution-workspace\",\n"
                + "  \"lockfileVersion\": 3,\n"
                + "  \"requires\": true,\n"
                + "  \"packages\": {},\n"
                + "  \"dependencies\": {}\n"
                + "}\n";
        Path lock = projectDir.resolve("package-lock.json");
        try {
            Files.writeString(lock, stub, StandardCharsets.UTF_8);
            log.info("Seeded package-lock.json stub at {} (so the frontend build's lockfile survives cleanup)", lock);
        } catch (IOException e) {
            throw new RuntimeException("Unable to seed package-lock.json stub at " + lock, e);
        }
    }

    /**
     * Vaadin may emit the lockfile at the project root or under a (version-specific) bundle directory.
     * Rather than hard-coding a path, locate every package-lock.json in the workspace (ignoring
     * node_modules) and keep the richest one, then copy it to the deterministic output location.
     */
    private void copyResolvedLock(Path projectDir) {
        Path best = null;
        int bestCount = -1;
        List<Path> pnpmLocks = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir)) {
            List<Path> candidates = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().replace('\\', '/').contains("/node_modules/"))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.equals("package-lock.json") || name.equals("pnpm-lock.yaml");
                    })
                    .toList();
            for (Path lock : candidates) {
                if (lock.getFileName().toString().equals("pnpm-lock.yaml")) {
                    pnpmLocks.add(lock);
                    continue;
                }
                int count = PackageLock.parse(lock).packageCount();
                log.info("Found package-lock.json: {} ({} package entries)", lock, count);
                if (count > bestCount) {
                    bestCount = count;
                    best = lock;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while locating the generated package-lock.json", e);
        }

        if (best == null) {
            if (!pnpmLocks.isEmpty()) {
                throw new RuntimeException("The frontend build produced a pnpm lockfile (" + pnpmLocks.get(0)
                        + ") but no package-lock.json. The npm exporter needs npm's package-lock.json — configure the "
                        + "Vaadin build to use npm (set 'pnpmEnable = false' in the vaadin {} block of the "
                        + "build-<checkpoint>.gradle template) and re-run.");
            }
            throw new RuntimeException("No package-lock.json (or pnpm-lock.yaml) was produced by the frontend build in "
                    + projectDir + ". " + describeWorkspace(projectDir)
                    + " The build most likely skipped the npm install (Vaadin reused a precompiled bundle, or the "
                    + "production frontend build did not run). Check the vaadinBuildFrontend output and verify "
                    + "'optimizeBundle = false' (and that 'productionMode = false' is not disabling the bundle build) "
                    + "in the build-<checkpoint>.gradle template.");
        }

        if (bestCount <= 0) {
            throw new RuntimeException("The resolved package-lock.json (" + best + ") has no package entries — "
                    + "npm install did not populate the seeded stub (the frontend build likely skipped the install). "
                    + "Check the vaadinBuildFrontend output.");
        }

        try {
            Path out = Paths.get(packageLockOutput).toAbsolutePath().normalize();
            Files.createDirectories(out.getParent());
            Files.copy(best, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Resolved package-lock.json ({} entries) copied to {}", bestCount, out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy resolved package-lock.json", e);
        }
    }

    /** Lists the workspace's top-level entries, to make a missing-lockfile failure diagnosable. */
    private String describeWorkspace(Path projectDir) {
        try (Stream<Path> top = Files.list(projectDir)) {
            String entries = top.map(p -> p.getFileName().toString() + (Files.isDirectory(p) ? "/" : ""))
                    .sorted()
                    .collect(Collectors.joining(", "));
            return "Workspace top-level entries: [" + entries + "].";
        } catch (IOException e) {
            return "(could not list workspace contents: " + e.getMessage() + ")";
        }
    }
}
