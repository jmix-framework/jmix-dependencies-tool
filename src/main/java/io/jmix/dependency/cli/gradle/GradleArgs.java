package io.jmix.dependency.cli.gradle;

import io.jmix.dependency.cli.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fluent builder for Gradle invocation arguments shared by all resolution commands.
 * <p>
 * In v1/v2 each command duplicated the same blocks of {@code if (x != null) args.add("-Px=" + x)} and had
 * two different mechanisms for passing dependencies (a {@code --dependency} task option for Java vs a
 * {@code -PexternalDependencies} project property for npm). Here everything is a {@code -P} project property,
 * which keeps the generated build scripts runnable standalone for debugging.
 */
public class GradleArgs {

    private final List<String> args = new ArrayList<>();

    public static GradleArgs create() {
        return new GradleArgs();
    }

    /** Adds {@code -P<name>=<value>} when the value is not blank. */
    public GradleArgs prop(String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            args.add("-P" + name + "=" + value);
        }
        return this;
    }

    /** Adds a raw Gradle argument (e.g. {@code --stacktrace}). */
    public GradleArgs raw(String arg) {
        args.add(arg);
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

    public List<String> build() {
        return new ArrayList<>(args);
    }
}
