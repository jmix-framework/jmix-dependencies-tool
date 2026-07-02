package io.jmix.dependency.cli.npm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The set of npm tarballs to mirror: every package name mapped to all versions to download.
 * <p>
 * Versions come from two sources:
 * <ul>
 *   <li><b>resolved</b> — the version npm actually installed (from the lockfile's {@code resolved} field);
 *       its tarball URL and integrity are known up front;</li>
 *   <li><b>variant</b> — a version named as an <em>exact</em> pin somewhere in the graph
 *       (typically a {@code peerDependencies} pin like {@code "dompurify": "3.4.0"}) that differs from the
 *       resolved one; its tarball is looked up from the registry at download time.</li>
 * </ul>
 * Mirroring both covers the realistic candidates a real project may resolve to, without brute-forcing
 * whole semver ranges.
 */
public class NpmDownloadPlan {

    private final Map<String, TreeSet<String>> versionsByName = new LinkedHashMap<>();
    private final Map<String, ResolvedRef> resolvedRefs = new LinkedHashMap<>();
    private final Set<String> resolvedKeys = new TreeSet<>();
    private final Set<String> variantKeys = new TreeSet<>();

    public static String key(String name, String version) {
        return name + "@" + version;
    }

    public void addResolved(String name, String version, ResolvedRef ref) {
        addVersion(name, version);
        resolvedRefs.put(key(name, version), ref);
        resolvedKeys.add(key(name, version));
    }

    public void addVariant(String name, String version) {
        // Only count as a variant if it is not already a resolved version.
        if (!resolvedKeys.contains(key(name, version))) {
            variantKeys.add(key(name, version));
        }
        addVersion(name, version);
    }

    private void addVersion(String name, String version) {
        versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
    }

    public Map<String, TreeSet<String>> versionsByName() {
        return versionsByName;
    }

    public ResolvedRef knownRef(String name, String version) {
        return resolvedRefs.get(key(name, version));
    }

    public int totalArtifacts() {
        return versionsByName.values().stream().mapToInt(Set::size).sum();
    }

    /** name@version entries that came from the lockfile's resolved set. */
    public Set<String> resolvedKeys() {
        return resolvedKeys;
    }

    /** name@version entries added because they are an exact pin different from the resolved version. */
    public Set<String> variantKeys() {
        return variantKeys;
    }
}
