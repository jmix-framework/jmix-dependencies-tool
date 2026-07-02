package io.jmix.dependency.cli.npm;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Downloads every tarball in an {@link NpmDownloadPlan} into a directory laid out so that
 * {@code upload-npm} can publish it ({@code <target>/[@scope/]<name>/<name>-<version>.tgz}).
 * <p>
 * This replaces the {@code node-tgz-downloader} step: it pulls exactly the versions derived from the
 * lockfile (resolved + exact-pin variants), so it never re-resolves peerDependencies to a different
 * version. No Node.js is required at export time.
 */
public class NpmExporter implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NpmExporter.class);

    public static class Report {
        public int downloaded;
        public int skippedExisting;
        public final List<String> missing = new ArrayList<>();
        public final List<String> integrityMismatch = new ArrayList<>();
    }

    private final Path targetDir;
    private final NpmRegistryClient registry;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public NpmExporter(Path targetDir, NpmRegistryClient registry) {
        this.targetDir = targetDir;
        this.registry = registry;
    }

    public Report export(NpmDownloadPlan plan) {
        Report report = new Report();
        for (Map.Entry<String, TreeSet<String>> e : plan.versionsByName().entrySet()) {
            String name = e.getKey();
            for (String version : e.getValue()) {
                ResolvedRef ref = plan.knownRef(name, version);
                if (ref == null || ref.url() == null) {
                    ref = registry.lookup(name, version);
                }
                if (ref == null || ref.url() == null) {
                    log.warn("No tarball URL for {}@{} — skipping", name, version);
                    report.missing.add(NpmDownloadPlan.key(name, version));
                    continue;
                }
                downloadOne(name, version, ref, report);
            }
        }
        return report;
    }

    private void downloadOne(String name, String version, ResolvedRef ref, Report report) {
        Path file = targetPath(name, version);
        if (Files.exists(file)) {
            report.skippedExisting++;
            return;
        }
        try {
            HttpGet get = new HttpGet(ref.url());
            byte[] bytes = httpClient.execute(get, response -> {
                if (response.getCode() != 200) {
                    throw new IOException("HTTP " + response.getCode() + " for " + ref.url());
                }
                return EntityUtils.toByteArray(response.getEntity());
            });

            if (!verifyIntegrity(bytes, ref.integrity())) {
                log.warn("Integrity mismatch for {}@{}", name, version);
                report.integrityMismatch.add(NpmDownloadPlan.key(name, version));
            }

            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
            report.downloaded++;
            log.info("Downloaded {}@{}", name, version);
        } catch (Exception ex) {
            log.warn("Failed to download {}@{} from {}: {}", name, version, ref.url(), ex.getMessage());
            report.missing.add(NpmDownloadPlan.key(name, version));
        }
    }

    /**
     * Layout: the immediate parent directory is the unscoped package name and the file is
     * {@code <unscoped>-<version>.tgz}, which is what {@code ArtifactNpm.createFromPackage} expects.
     * Scoped packages get an extra {@code @scope} parent to avoid collisions.
     */
    private Path targetPath(String name, String version) {
        String unscoped = name.contains("/") ? name.substring(name.indexOf('/') + 1) : name;
        String scope = name.startsWith("@") && name.contains("/") ? name.substring(0, name.indexOf('/')) : null;
        Path dir = scope != null ? targetDir.resolve(scope).resolve(unscoped) : targetDir.resolve(unscoped);
        return dir.resolve(unscoped + "-" + version + ".tgz");
    }

    private boolean verifyIntegrity(byte[] bytes, String integrity) {
        if (integrity == null || !integrity.startsWith("sha512-")) {
            return true; // nothing to check (e.g. legacy shasum-only entries)
        }
        try {
            String expected = integrity.substring("sha512-".length());
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(bytes);
            String actual = Base64.getEncoder().encodeToString(digest);
            return expected.equals(actual);
        } catch (Exception e) {
            return true; // don't fail the export over a hashing problem
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (Exception ignored) {
        }
    }
}
