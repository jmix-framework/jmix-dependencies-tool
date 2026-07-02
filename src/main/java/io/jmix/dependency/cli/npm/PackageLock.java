package io.jmix.dependency.cli.npm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, defensive parser for npm's {@code package-lock.json}.
 * <p>
 * Prefers the modern {@code packages} map (lockfileVersion 2 and 3, npm &gt;= 7), which records every
 * installed package - regular deps, auto-installed peers and optionals - each with its exact
 * {@code version}, {@code resolved} tarball URL and {@code integrity}. Falls back to the legacy v1
 * {@code dependencies} tree when {@code packages} is absent (peer pins are not available there).
 */
public class PackageLock {

    private static final String NODE_MODULES = "node_modules/";

    public static class Entry {
        public final String name;
        public final String version;
        public final String resolved;
        public final String integrity;
        public final Map<String, String> dependencies;
        public final Map<String, String> peerDependencies;
        public final Map<String, String> optionalDependencies;

        Entry(String name, String version, String resolved, String integrity,
              Map<String, String> dependencies, Map<String, String> peerDependencies,
              Map<String, String> optionalDependencies) {
            this.name = name;
            this.version = version;
            this.resolved = resolved;
            this.integrity = integrity;
            this.dependencies = dependencies;
            this.peerDependencies = peerDependencies;
            this.optionalDependencies = optionalDependencies;
        }
    }

    private final int lockfileVersion;
    private final List<Entry> entries;

    private PackageLock(int lockfileVersion, List<Entry> entries) {
        this.lockfileVersion = lockfileVersion;
        this.entries = entries;
    }

    public int lockfileVersion() {
        return lockfileVersion;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int packageCount() {
        return entries.size();
    }

    public boolean hasPackagesMap() {
        return lockfileVersion >= 2;
    }

    public static PackageLock parse(Path file) {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            int version = root.has("lockfileVersion") ? root.get("lockfileVersion").getAsInt() : 0;

            List<Entry> entries = new ArrayList<>();
            if (root.has("packages") && root.get("packages").isJsonObject()) {
                JsonObject packages = root.getAsJsonObject("packages");
                for (Map.Entry<String, JsonElement> e : packages.entrySet()) {
                    String key = e.getKey();
                    if (key.isEmpty() || !e.getValue().isJsonObject()) {
                        continue; // "" is the root project itself
                    }
                    JsonObject o = e.getValue().getAsJsonObject();
                    String name = optString(o, "name");
                    if (name == null) {
                        name = nameFromPath(key);
                    }
                    entries.add(toEntry(name, o));
                }
            } else if (root.has("dependencies") && root.get("dependencies").isJsonObject()) {
                collectLegacy(root.getAsJsonObject("dependencies"), entries);
            }
            return new PackageLock(version, entries);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read package-lock.json: " + file, ex);
        }
    }

    private static Entry toEntry(String name, JsonObject o) {
        return new Entry(
                name,
                optString(o, "version"),
                optString(o, "resolved"),
                optString(o, "integrity"),
                optMap(o, "dependencies"),
                optMap(o, "peerDependencies"),
                optMap(o, "optionalDependencies"));
    }

    private static void collectLegacy(JsonObject deps, List<Entry> entries) {
        for (Map.Entry<String, JsonElement> e : deps.entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject o = e.getValue().getAsJsonObject();
            entries.add(new Entry(
                    e.getKey(),
                    optString(o, "version"),
                    optString(o, "resolved"),
                    optString(o, "integrity"),
                    optMap(o, "requires"),
                    Map.of(),
                    Map.of()));
            if (o.has("dependencies") && o.get("dependencies").isJsonObject()) {
                collectLegacy(o.getAsJsonObject("dependencies"), entries);
            }
        }
    }

    private static String nameFromPath(String key) {
        int idx = key.lastIndexOf(NODE_MODULES);
        return idx >= 0 ? key.substring(idx + NODE_MODULES.length()) : key;
    }

    private static String optString(JsonObject o, String field) {
        return o.has(field) && o.get(field).isJsonPrimitive() ? o.get(field).getAsString() : null;
    }

    private static Map<String, String> optMap(JsonObject o, String field) {
        Map<String, String> result = new LinkedHashMap<>();
        if (o.has(field) && o.get(field).isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(field).entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    result.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }
        return result;
    }
}
