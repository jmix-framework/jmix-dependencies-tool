package io.jmix.dependency.cli.npm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads npm registry metadata (packuments) to resolve a tarball URL + integrity for a specific
 * package version. Used for "variant" versions (exact pins) that were not the resolved version and
 * therefore have no {@code resolved} URL in the lockfile. Packuments are cached per package name.
 */
public class NpmRegistryClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NpmRegistryClient.class);

    private final String registryUrl;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final Map<String, JsonObject> packumentCache = new HashMap<>();

    public NpmRegistryClient(String registryUrl) {
        this.registryUrl = registryUrl.endsWith("/") ? registryUrl.substring(0, registryUrl.length() - 1) : registryUrl;
    }

    public ResolvedRef lookup(String name, String version) {
        JsonObject packument = packument(name);
        if (packument == null) {
            return null;
        }
        if (!packument.has("versions") || !packument.get("versions").isJsonObject()) {
            return null;
        }
        JsonObject versions = packument.getAsJsonObject("versions");
        if (!versions.has(version) || !versions.get(version).isJsonObject()) {
            return null;
        }
        JsonObject dist = versions.getAsJsonObject(version).getAsJsonObject("dist");
        if (dist == null || !dist.has("tarball")) {
            return null;
        }
        String tarball = dist.get("tarball").getAsString();
        String integrity = dist.has("integrity") ? dist.get("integrity").getAsString() : null;
        return new ResolvedRef(tarball, integrity);
    }

    private JsonObject packument(String name) {
        return packumentCache.computeIfAbsent(name, this::fetchPackument);
    }

    private JsonObject fetchPackument(String name) {
        String encoded = name.startsWith("@") ? name.replace("/", "%2f") : name;
        String url = registryUrl + "/" + encoded;
        try {
            HttpGet get = new HttpGet(url);
            return httpClient.execute(get, response -> {
                if (response.getCode() != 200) {
                    log.warn("Registry metadata request failed ({}) for {}", response.getCode(), name);
                    return null;
                }
                String body = EntityUtils.toString(response.getEntity());
                return JsonParser.parseString(body).getAsJsonObject();
            });
        } catch (Exception e) {
            log.warn("Could not fetch registry metadata for {}: {}", name, e.getMessage());
            return null;
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
