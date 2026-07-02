package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.npm.NpmDownloadPlan;
import io.jmix.dependency.cli.npm.NpmExporter;
import io.jmix.dependency.cli.npm.NpmRegistryClient;
import io.jmix.dependency.cli.npm.NpmVariantCollector;
import io.jmix.dependency.cli.npm.PackageLock;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports npm dependencies as a directory of tgz archives ready for {@code upload-npm}.
 * <p>
 * Pure Java: it reads the resolved {@code package-lock.json}(s), collects the versions to mirror
 * (resolved + exact-pin variants), and downloads each tarball directly — no Node.js, no
 * {@code node-tgz-downloader}, no re-resolution of peerDependencies.
 * <p>
 * By default it unions the two lockfiles {@code resolve-npm} produces in {@code ../npm-work}: the project
 * lock from {@code vaadinBuildFrontend} (Jmix-specific packages) and the {@code vaadin-dev-bundle} jar's lock
 * (the framework's frozen versions, e.g. {@code dompurify 3.4.5}). Pass {@code --package-lock-file} (repeatable)
 * to use specific lockfiles instead.
 */
@Parameters(commandDescription = "Exports resolved npm dependencies as tgz archives")
public class ExportNpmCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ExportNpmCommand.class);

    private static final String DEFAULT_REGISTRY = "https://registry.npmjs.org";

    @Parameter(names = {"--package-lock-file"},
            description = "package-lock.json to mirror (repeatable). Defaults to the lockfiles produced by " +
                    "resolve-npm in ../npm-work (the project lock + the vaadin-dev-bundle lock)", order = 0)
    private List<String> packageLockFiles;

    @Parameter(names = {"--target-dir"}, description = "Export target directory", order = 1)
    private String targetDirectory;

    @Parameter(names = {"--npm-registry"}, description = "Registry used to look up tarballs for variant versions", order = 2)
    private String npmRegistry;

    @Parameter(names = {"--report-file"}, description = "Path to a file listing mirrored (resolved + variant) artifacts", order = 3)
    private String reportFile;

    @Override
    public void run() {
        if (targetDirectory == null) {
            targetDirectory = DefaultPaths.getDefaultExportNpmDir();
        }
        if (npmRegistry == null) {
            npmRegistry = DEFAULT_REGISTRY;
        }

        List<Path> lockPaths = resolveLockPaths();
        Path targetPath = Paths.get(targetDirectory).toAbsolutePath().normalize();
        log.info("Target directory: {}", targetPath);
        log.info("Registry: {}", npmRegistry);

        List<PackageLock> locks = new ArrayList<>();
        List<Path> usedLocks = new ArrayList<>();
        for (Path lp : lockPaths) {
            if (!Files.exists(lp)) {
                log.info("Lockfile not found, skipping: {}", lp);
                continue;
            }
            PackageLock lock = PackageLock.parse(lp);
            log.info("Lockfile {}: lockfileVersion={}, {} package entries", lp, lock.lockfileVersion(), lock.packageCount());
            if (!lock.hasPackagesMap()) {
                log.warn("Lockfile {} has lockfileVersion < 2 (no 'packages' map); peer-dependency variants " +
                        "cannot be collected from it.", lp);
            }
            locks.add(lock);
            usedLocks.add(lp);
        }
        if (locks.isEmpty()) {
            throw new RuntimeException("No package-lock.json found (looked at: " + lockPaths + "). Run resolve-npm first.");
        }

        NpmDownloadPlan plan = NpmVariantCollector.collect(locks);
        log.info("Mirroring {} artifact(s) from {} lockfile(s): {} resolved + {} variant",
                plan.totalArtifacts(), locks.size(), plan.resolvedKeys().size(), plan.variantKeys().size());
        if (!plan.variantKeys().isEmpty()) {
            log.info("Variant versions added (would be missed by a plain resolved-only mirror): {}", plan.variantKeys());
        }

        NpmExporter.Report report;
        try (NpmRegistryClient registry = new NpmRegistryClient(npmRegistry);
             NpmExporter exporter = new NpmExporter(targetPath, registry)) {
            Files.createDirectories(targetPath);
            report = exporter.export(plan);
        } catch (IOException e) {
            throw new RuntimeException("Export failed", e);
        }

        copyLockFiles(usedLocks, targetPath);
        writeReport(plan, report);

        log.info("Export completed: {} downloaded, {} already present, {} missing, {} integrity mismatches",
                report.downloaded, report.skippedExisting, report.missing.size(), report.integrityMismatch.size());
        if (!report.missing.isEmpty()) {
            log.warn("Missing tarballs (not mirrored): {}", report.missing);
        }
    }

    /** The lockfiles to mirror: explicit {@code --package-lock-file}s, else resolve-npm's two outputs in ../npm-work. */
    private List<Path> resolveLockPaths() {
        if (packageLockFiles != null && !packageLockFiles.isEmpty()) {
            List<Path> paths = new ArrayList<>();
            for (String f : packageLockFiles) {
                paths.add(Paths.get(f).toAbsolutePath().normalize());
            }
            return paths;
        }
        Path npmWork = Paths.get(DefaultPaths.getDefaultNpmLockFile()).toAbsolutePath().normalize().getParent();
        return List.of(
                npmWork.resolve("package-lock.json"),
                npmWork.resolve("dev-bundle-package-lock.json"));
    }

    private void copyLockFiles(List<Path> lockPaths, Path targetPath) {
        for (Path lp : lockPaths) {
            try {
                FileUtils.copyFileToDirectory(lp.toFile(), targetPath.toFile());
            } catch (IOException e) {
                log.warn("Unable to copy {} to {}: {}", lp, targetPath, e.getMessage());
            }
        }
    }

    private void writeReport(NpmDownloadPlan plan, NpmExporter.Report report) {
        if (reportFile == null) {
            return;
        }
        Path path = Paths.get(reportFile).toAbsolutePath().normalize();
        List<String> lines = new ArrayList<>();
        lines.add("# Resolved versions (" + plan.resolvedKeys().size() + ")");
        lines.addAll(plan.resolvedKeys());
        lines.add("");
        lines.add("# Variant versions added (" + plan.variantKeys().size() + ")");
        lines.addAll(plan.variantKeys());
        if (!report.missing.isEmpty()) {
            lines.add("");
            lines.add("# Missing (" + report.missing.size() + ")");
            lines.addAll(report.missing);
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
            log.info("Report written to {}", path);
        } catch (IOException e) {
            log.warn("Could not write report file {}: {}", path, e.getMessage());
        }
    }
}
