package io.jmix.dependency.cli.workspace;

import io.jmix.dependency.cli.version.JmixVersion;

import java.util.Collection;

/**
 * Checkpoint selection used for both build templates and the Gradle-version table.
 * <p>
 * A "checkpoint" key is a version (e.g. {@code 2.1}, {@code 2.3.1}, or {@code 2.x} which is read as
 * {@code 2.0}) marking where some structure was introduced. For a requested version V the selected key is
 * the one with the <b>greatest checkpoint &le; V</b>; a key therefore applies to its version and every later
 * one until the next checkpoint. Comparison uses the stable version (suffix stripped), so pre-release
 * builds like {@code 2.8.0-SNAPSHOT} still match the {@code 2.0}/{@code 2.1} checkpoints.
 */
public final class CheckpointSelector {

    private CheckpointSelector() {
    }

    /**
     * @return the key with the greatest checkpoint version &le; {@code version}; otherwise {@code "default"}
     * when {@code allowDefault} is set and such a key exists; otherwise {@code null}
     */
    public static String select(Collection<String> keys, JmixVersion version, boolean allowDefault) {
        JmixVersion stable = stable(version);
        String bestKey = null;
        JmixVersion bestVersion = null;
        boolean hasDefault = false;
        for (String key : keys) {
            if (key != null && "default".equalsIgnoreCase(key.trim())) {
                hasDefault = true;
                continue;
            }
            JmixVersion checkpoint = parse(key);
            if (checkpoint == null || checkpoint.compareTo(stable) > 0) {
                continue;
            }
            if (bestVersion == null || checkpoint.compareTo(bestVersion) > 0) {
                bestVersion = checkpoint;
                bestKey = key;
            }
        }
        if (bestKey != null) {
            return bestKey;
        }
        return (allowDefault && hasDefault) ? "default" : null;
    }

    /**
     * Parses a checkpoint key to a suffix-less {@link JmixVersion}, or {@code null} if it is not a version
     * ({@code "default"}, {@code "plain"}, blank, or non-numeric). {@code .x} is treated as {@code .0} and
     * one/two-part keys are padded to three parts.
     */
    public static JmixVersion parse(String key) {
        if (key == null) {
            return null;
        }
        String k = key.trim();
        if (k.isEmpty() || k.equalsIgnoreCase("default") || k.equalsIgnoreCase("plain")) {
            return null;
        }
        k = k.replace(".x", ".0"); // a major.x checkpoint is the major's .0
        String[] parts = k.split("\\.");
        if (parts.length == 1) {
            k = k + ".0.0";
        } else if (parts.length == 2) {
            k = k + ".0";
        }
        try {
            return JmixVersion.from(k);
        } catch (Exception e) {
            return null;
        }
    }

    private static JmixVersion stable(JmixVersion version) {
        return new JmixVersion(version.major(), version.minor(), version.patch(), "");
    }
}
