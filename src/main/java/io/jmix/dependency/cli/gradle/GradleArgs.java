package io.jmix.dependency.cli.gradle;

import io.jmix.dependency.cli.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for a Gradle invocation shared by all resolution commands.
 * <p>
 * In v1/v2 each command duplicated the same blocks of {@code if (x != null) args.add("-Px=" + x)} and had
 * two different mechanisms for passing dependencies (a {@code --dependency} task option for Java vs a
 * {@code -PexternalDependencies} project property for npm). Here everything is a project property, which
 * keeps the generated build scripts runnable standalone for debugging.
 * <p>
 * Project properties are kept separate from raw command-line arguments: {@link JmixGradleClient} writes the
 * properties into the workspace's {@code gradle.properties} rather than passing them as {@code -P} on the
 * command line. The Jmix module list ({@code jmixModules}) can hold hundreds of coordinates when commercial
 * add-ons are resolved, which would otherwise overflow the OS command-line length limit. gradle.properties
 * has no such limit, so resolution works for any number of modules.
 */
public class GradleArgs {

    private final Map<String, String> properties = new LinkedHashMap<>();
    private final List<String> rawArgs = new ArrayList<>();

    public static GradleArgs create() {
        return new GradleArgs();
    }

    /** Adds project property {@code <name>=<value>} when the value is not blank. */
    public GradleArgs prop(String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            properties.put(name, value);
        }
        return this;
    }

    /** Adds a presence-only project property (read via {@code project.hasProperty(name)}), e.g. {@code skipSources}. */
    public GradleArgs flag(String name) {
        properties.put(name, "true");
        return this;
    }

    /** Adds a raw Gradle command-line argument (e.g. {@code --stacktrace}). */
    public GradleArgs raw(String arg) {
        rawArgs.add(arg);
        return this;
    }

    /**
     * Adds the standard Jmix coordinates and repository configuration as project properties.
     * The generated build scripts read these via {@code project.findProperty(...)}.
     */
    public GradleArgs jmix(String jmixVersion,
                           String jmixPluginVersion,
                           String jmixLicenseKey,
                           String publicRepository,
                           String premiumRepository,
                           Collection<String> extraRepositories) {
        prop("jmixVersion", jmixVersion);
        prop("jmixPluginVersion", jmixPluginVersion);
        prop("jmixLicenseKey", jmixLicenseKey);
        prop("jmixPublicRepository", publicRepository);
        prop("jmixPremiumRepository", premiumRepository);
        if (extraRepositories != null && !extraRepositories.isEmpty()) {
            prop("extraRepositories", String.join(",", extraRepositories));
        }
        return this;
    }

    /** The comma-separated list of dependencies (GAV) to resolve. */
    public GradleArgs modules(Collection<String> modules) {
        if (modules != null && !modules.isEmpty()) {
            prop("jmixModules", String.join(",", modules));
        }
        return this;
    }

    /** Project properties to be written into the workspace's gradle.properties. */
    public Map<String, String> properties() {
        return new LinkedHashMap<>(properties);
    }

    /** Raw arguments to pass on the Gradle command line. */
    public List<String> rawArgs() {
        return new ArrayList<>(rawArgs);
    }
}
