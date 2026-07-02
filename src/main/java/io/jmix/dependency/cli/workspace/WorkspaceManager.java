package io.jmix.dependency.cli.workspace;

import io.jmix.dependency.cli.util.StringUtils;
import io.jmix.dependency.cli.version.JmixVersion;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Generates the throw-away Gradle project used to resolve dependencies, instead of shipping frozen
 * stub projects ({@code resolver} / {@code npm-resolver}) as in v1/v2.
 * <p>
 * There is a <b>single</b> project template group ({@code templates/project/}), not one per scope: a real
 * Jmix app is one project that carries both its Java and (via the Vaadin plugin) its npm dependencies.
 * The same generated project is used for every resolution command — {@code resolve-jmix}/{@code resolve-lib}
 * run its {@code resolveAll} task, {@code resolve-npm} runs {@code vaadinBuildFrontend}.
 * <p>
 * <b>Template selection is checkpoint-based.</b> A template named {@code build-<X.Y>.gradle} is a checkpoint
 * meaning "this project structure was introduced at Jmix X.Y". For a requested version V the chosen template
 * is the one with the <em>greatest checkpoint &le; V</em>:
 * <pre>
 *   checkpoints {1.0, 2.0, 2.1, 3.0}
 *   2.0.5 -> build-2.0   2.1.0 -> build-2.1   2.8.0 -> build-2.1   3.4.0 -> build-3.0
 * </pre>
 * So you only add a template when the structure actually changes; it then applies to that version and all
 * later ones until the next checkpoint (a new major with no checkpoint yet falls back to the latest one,
 * logged). Use precise versions for template file names ({@code build-2.2.gradle}); do not use {@code .x}.
 * The no-version case ({@code resolve-lib} for a plain library) always uses {@code build-plain.gradle}.
 * <p>
 * Each workspace also gets a generated Gradle <b>wrapper</b> whose {@code distributionUrl} is selected the
 * same checkpoint way from {@code templates/gradle-versions.properties}; deptool runs the wrapper as a
 * subprocess, decoupling the daemon Gradle from the tool. Per-run values arrive as {@code -P} properties.
 */
public class WorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    /** Final fallback Gradle version when no checkpoint and no {@code default} key matches. */
    public static final String DEFAULT_GRADLE_VERSION = "8.14.4";

    /** Default classpath base under which the bundled templates live. */
    private static final String DEFAULT_TEMPLATES_ROOT = "templates";
    private static final String DISTRIBUTION_URL_TEMPLATE =
            "https\\://services.gradle.org/distributions/gradle-%s-bin.zip";

    private final Path workspaceRoot;
    private final String templatesRoot;
    private Properties gradleVersions;
    private Set<String> templateCheckpoints;

    public WorkspaceManager(Path workspaceRoot) {
        this(workspaceRoot, DEFAULT_TEMPLATES_ROOT);
    }

    /**
     * @param workspaceRoot where the throw-away project is generated
     * @param templatesRoot classpath base under which the templates live (normally {@code "templates"}).
     *                      Overridable so tests can point at a fixed fixture set
     *                      ({@code test-templates}) and stay independent of the shipped templates.
     */
    public WorkspaceManager(Path workspaceRoot, String templatesRoot) {
        this.workspaceRoot = workspaceRoot;
        this.templatesRoot = templatesRoot;
    }

    private String templateDir() {
        return templatesRoot + "/project";
    }

    /** The effective Gradle version: an explicit override wins, otherwise the per-checkpoint recommendation. */
    public String effectiveGradleVersion(JmixVersion version, String override) {
        if (StringUtils.isNotBlank(override)) {
            return override;
        }
        return recommendedGradleVersion(version);
    }

    /** The Gradle version recommended for the Jmix line (highest checkpoint key &le; version). */
    public String recommendedGradleVersion(JmixVersion version) {
        if (version == null) {
            return DEFAULT_GRADLE_VERSION; // plain library resolution
        }
        Properties props = loadGradleVersions();
        String key = CheckpointSelector.select(props.stringPropertyNames(), version, true);
        if (key != null) {
            String value = props.getProperty(key);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return DEFAULT_GRADLE_VERSION;
    }

    /**
     * Creates a clean workspace for the given version, writing {@code settings.gradle}, {@code build.gradle},
     * a frontend entry point and a Gradle wrapper pinned to {@code gradleVersion}.
     *
     * @param version the Jmix version, or {@code null} for a plain library resolution with no Jmix BOM
     */
    public Path prepare(JmixVersion version, String gradleVersion) {
        Path dir = workspaceRoot.resolve("project");
        try {
            if (Files.exists(dir)) {
                FileUtils.deleteDirectory(dir.toFile());
            }
            Files.createDirectories(dir);

            writeResource(templatesRoot + "/settings.gradle", dir.resolve("settings.gradle"));
            Files.writeString(dir.resolve("build.gradle"), readBuildTemplate(version), StandardCharsets.UTF_8);
            // Shared resolution mechanism applied by every build-<checkpoint>.gradle via `apply from`.
            writeResource(templateDir() + "/resolve-support.gradle", dir.resolve("resolve-support.gradle"));

            Path frontend = dir.resolve("frontend");
            Files.createDirectories(frontend);
            writeResource(templateDir() + "/frontend-index.html", frontend.resolve("index.html"));

            writeWrapper(dir, gradleVersion);

            log.info("Generated workspace at {} (Gradle {})", dir.toAbsolutePath().normalize(), gradleVersion);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate workspace at " + dir, e);
        }
    }

    private void writeWrapper(Path dir, String gradleVersion) throws IOException {
        Path wrapperDir = dir.resolve("gradle/wrapper");
        Files.createDirectories(wrapperDir);
        writeResource(templatesRoot + "/wrapper/gradle-wrapper.jar", wrapperDir.resolve("gradle-wrapper.jar"));

        Path gradlew = dir.resolve("gradlew");
        writeResource(templatesRoot + "/wrapper/gradlew", gradlew);
        writeResource(templatesRoot + "/wrapper/gradlew.bat", dir.resolve("gradlew.bat"));
        // Restore the executable bit lost when shipping the script as a resource (no-op on Windows).
        gradlew.toFile().setExecutable(true, false);

        String props = "distributionBase=GRADLE_USER_HOME\n"
                + "distributionPath=wrapper/dists\n"
                + "distributionUrl=" + String.format(DISTRIBUTION_URL_TEMPLATE, gradleVersion) + "\n"
                + "networkTimeout=10000\n"
                + "validateDistributionUrl=true\n"
                + "zipStoreBase=GRADLE_USER_HOME\n"
                + "zipStorePath=wrapper/dists\n";
        Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), props, StandardCharsets.UTF_8);
    }

    /**
     * The build template key chosen for the version: {@code "plain"} for a null (plain library) resolution,
     * otherwise the greatest checkpoint &le; the version. Throws if no checkpoint matches.
     */
    public String selectTemplateKey(JmixVersion version) {
        if (version == null) {
            return "plain";
        }
        String key = CheckpointSelector.select(templateCheckpoints(), version, false);
        if (key == null) {
            throw new IllegalStateException("No build template found for Jmix version " + version
                    + ". Available template checkpoints: " + templateCheckpoints()
                    + ". Add templates/project/build-<version>.gradle for this line.");
        }
        return key;
    }

    private String readBuildTemplate(JmixVersion version) throws IOException {
        String key = selectTemplateKey(version);
        String resource = templateDir() + "/build-" + key + ".gradle";
        try (InputStream is = classpath(resource)) {
            if (is == null) {
                throw new IllegalStateException("Missing bundled template: " + resource);
            }
            log.info("Using build template build-{}.gradle for {}", key, version == null ? "plain library" : version);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Checkpoint versions for which a {@code templates/project/build-<v>.gradle} exists (excludes plain). */
    private Set<String> templateCheckpoints() {
        if (templateCheckpoints == null) {
            templateCheckpoints = enumerateTemplateCheckpoints();
        }
        return templateCheckpoints;
    }

    private Set<String> enumerateTemplateCheckpoints() {
        Set<String> versions = new TreeSet<>();
        String templateDir = templateDir();
        try {
            URL url = getClass().getClassLoader().getResource(templateDir);
            if (url == null) {
                return versions;
            }
            if ("file".equals(url.getProtocol())) {
                try (Stream<Path> entries = Files.list(Paths.get(url.toURI()))) {
                    entries.forEach(p -> addTemplateCheckpoint(p.getFileName().toString(), versions));
                }
            } else if ("jar".equals(url.getProtocol())) {
                String path = url.getPath(); // file:/.../deptool.jar!/templates/project
                File jarFile = new File(new URI(path.substring(0, path.indexOf('!'))));
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> en = jar.entries();
                    while (en.hasMoreElements()) {
                        String name = en.nextElement().getName();
                        if (name.startsWith(templateDir + "/") && name.endsWith(".gradle")) {
                            addTemplateCheckpoint(name.substring(name.lastIndexOf('/') + 1), versions);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not enumerate project templates: {}", e.getMessage());
        }
        return versions;
    }

    private void addTemplateCheckpoint(String fileName, Set<String> out) {
        if (!fileName.startsWith("build-") || !fileName.endsWith(".gradle")) {
            return;
        }
        String key = fileName.substring("build-".length(), fileName.length() - ".gradle".length());
        if (CheckpointSelector.parse(key) != null) { // skips build-plain.gradle and any non-version name
            out.add(key);
        }
    }

    private Properties loadGradleVersions() {
        if (gradleVersions == null) {
            gradleVersions = new Properties();
            try (InputStream is = classpath(templatesRoot + "/gradle-versions.properties")) {
                if (is != null) {
                    gradleVersions.load(is);
                }
            } catch (IOException e) {
                log.warn("Could not read gradle-versions.properties, using default {}", DEFAULT_GRADLE_VERSION);
            }
        }
        return gradleVersions;
    }

    private void writeResource(String resource, Path target) throws IOException {
        try (InputStream is = classpath(resource)) {
            if (is == null) {
                throw new IllegalStateException("Missing bundled resource: " + resource);
            }
            Files.createDirectories(target.getParent());
            Files.copy(is, target);
        }
    }

    private InputStream classpath(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
