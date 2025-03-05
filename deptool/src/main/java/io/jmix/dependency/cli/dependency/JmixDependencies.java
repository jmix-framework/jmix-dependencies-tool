package io.jmix.dependency.cli.dependency;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class JmixDependencies {

    private static final Logger log = LoggerFactory.getLogger(JmixDependencies.class);

    public static Set<String> getVersionSpecificJmixDependencies(String jmixVersion, boolean resolveCommercialAddons) {
        String minorJmixVersion = getMinorVersion(jmixVersion);
        Set<String> dependencies = _getVersionSpecificDependencies(minorJmixVersion, resolveCommercialAddons);
        dependencies.addAll(_getVersionSpecificDependencies(jmixVersion, resolveCommercialAddons));
        //if there are no files for the given dependency version then use the default file (dependencies-default.xml)
        if (dependencies.isEmpty()) {
            dependencies.addAll(_getVersionSpecificDependencies("default", resolveCommercialAddons));
        }
        return dependencies;
    }

    private static Set<String> _getVersionSpecificDependencies(String version, boolean resolveCommercialAddons) {
        try (InputStream is = JmixDependencies.class.getResourceAsStream("/jmix-dependencies/dependencies-" + version + ".xml")) {
            if (is == null) {
                log.debug("Dependencies file for version {} not found", version);
                return new HashSet<>();
            }
            log.debug("Parsing dependencies file: {}", version + ".xml");
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            Set<String> dependencies = new HashSet<>();
            List<Node> nodes = document.selectNodes("/dependencies/open-source-dependencies/dependency");
            Set<String> openSourceDependencies = nodes.stream()
                    .map(Node::getText)
                    .collect(Collectors.toSet());
            dependencies.addAll(openSourceDependencies);
            if (resolveCommercialAddons) {
                List<Node> commercialNodes = document.selectNodes("/dependencies/commercial-dependencies/dependency");
                Set<String> commercialDependencies = commercialNodes.stream()
                        .map(Node::getText)
                        .collect(Collectors.toSet());
                dependencies.addAll(commercialDependencies);
            }
            return dependencies;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts a minor version from version string.
     * Examples:
     * <ul>
     *     <li>null -> null</li>
     *     <li>"" -> ""</li>
     *     <li>abc -> ""</li>
     *     <li>1.4.2 -> 1.4</li>
     *     <li>1.4 -> 1.4</li>
     *     <li>1 -> 1.0</li>
     * </ul>
     */
    public static String getMinorVersion(String jmixVersion) {
        if (jmixVersion == null) {
            return null;
        }

        String[] parts = jmixVersion.split("\\.");
        if (parts.length < 2) {
            if (parts.length == 0) {
                return "";
            } else {
                return parts[0] + "." + 0;
            }
        }

        return parts[0] + "." + parts[1];
    }

    /**
     * Extracts a patch from version string.
     * Examples:
     * <ul>
     *     <li>null -> null</li>
     *     <li>"" -> ""</li>
     *     <li>abc -> ""</li>
     *     <li>1.4.2 -> 2</li>
     *     <li>1.4 -> 0</li>
     *     <li>1 -> 0</li>
     * </ul>
     */
    public static String getPatchVersion(String jmixVersion) {
        if (jmixVersion == null) {
            return null;
        }

        String[] parts = jmixVersion.split("\\.");
        if (parts.length < 3) {
            if (parts.length == 0) {
                return "";
            } else {
                return "0";
            }
        }

        return parts[2];
    }

    public static boolean hasPatch(String jmixVersion) {
        if (jmixVersion == null) {
            return false;
        }
        String[] parts = jmixVersion.split("\\.");
        return parts.length > 2;
    }

}
