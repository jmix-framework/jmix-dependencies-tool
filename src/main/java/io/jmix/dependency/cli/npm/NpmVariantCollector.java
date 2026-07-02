package io.jmix.dependency.cli.npm;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds an {@link NpmDownloadPlan} from one or more parsed lockfiles.
 * <p>
 * For every installed package it records the resolved version (with its tarball ref), and additionally
 * collects every <em>exact</em> version referenced as a dependency/peerDependency/optionalDependency pin.
 * This is the automated form of the per-version {@code additional/npm/.../package-lock.json} patches that
 * v1/v2 maintained by hand to recover versions the single resolution missed.
 * <p>
 * Multiple lockfiles are unioned. {@code resolve-npm} produces two: the project lock from
 * {@code vaadinBuildFrontend} (Jmix-specific packages + a fresh, possibly floating resolution) and the
 * {@code vaadin-dev-bundle} jar's lock (the framework's frozen versions, e.g. {@code dompurify 3.4.5}). The
 * union covers both — the Jmix packages and the stable framework versions a project actually runs on.
 */
public class NpmVariantCollector {

    // A single concrete semver, optionally prefixed with '=' or 'v' — NOT a range (^ ~ > < x * || space -).
    private static final Pattern EXACT = Pattern.compile("\\d+\\.\\d+\\.\\d+([-+][0-9A-Za-z-.]+)?");

    public static NpmDownloadPlan collect(PackageLock lock) {
        return collect(List.of(lock));
    }

    /** Unions all lockfiles into one download plan. */
    public static NpmDownloadPlan collect(List<PackageLock> locks) {
        NpmDownloadPlan plan = new NpmDownloadPlan();

        for (PackageLock lock : locks) {
            for (PackageLock.Entry e : lock.entries()) {
                if (e.name == null || e.version == null) {
                    continue;
                }
                if (e.resolved != null) {
                    plan.addResolved(e.name, e.version, new ResolvedRef(e.resolved, e.integrity));
                } else {
                    // installed but without a tarball URL (e.g. workspace/link) — still record the version
                    plan.addVariant(e.name, e.version);
                }
            }
        }
        // Second pass (across all locks) so exact pins are compared against the full resolved set.
        for (PackageLock lock : locks) {
            for (PackageLock.Entry e : lock.entries()) {
                collectExact(plan, e.dependencies);
                collectExact(plan, e.peerDependencies);
                collectExact(plan, e.optionalDependencies);
            }
        }
        return plan;
    }

    private static void collectExact(NpmDownloadPlan plan, Map<String, String> constraints) {
        if (constraints == null) {
            return;
        }
        for (Map.Entry<String, String> c : constraints.entrySet()) {
            String exact = asExactVersion(c.getValue());
            if (exact != null) {
                plan.addVariant(c.getKey(), exact);
            }
        }
    }

    /** Returns the concrete version if {@code range} pins exactly one version, otherwise {@code null}. */
    static String asExactVersion(String range) {
        if (range == null) {
            return null;
        }
        String r = range.trim();
        if (r.startsWith("=")) {
            r = r.substring(1).trim();
        }
        if (r.startsWith("v")) {
            r = r.substring(1);
        }
        return EXACT.matcher(r).matches() ? r : null;
    }
}
