package io.jmix.dependency.cli.dependency;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
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

    public static int compareVersions(String version1, String version2) {
        return JmixVersionComparator.compareVersions(version1, version2);
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

    private static class JmixVersionComparator implements Comparator<String> {

        private static final Pattern VERSION_SPLIT_REGEX = Pattern.compile("[.-]");

        public static final JmixVersionComparator INSTANCE = new JmixVersionComparator();

        public static int compareVersions(String v1, String v2) {
            return INSTANCE.compare(v1, v2);
        }

        /**
         <ul>
         *     <li>null, null -> 0</li>
         *     <li>null, !null -> -1</li>
         *     <li>!null, null -> 1</li>
         *     <li>2.0, 1.0 -> 1</li>
         *     <li>1.0, 2.0 -> -1</li>
         *     <li>2.0, 2.0 -> 0</li>
         *     <li>2.0, 2.1 -> -1</li>
         *     <li>2.0.1, 2.0.0 -> 1</li>
         * </ul>
         * @return >0 if v1 is newer than v2, 0 if equals, <0 if v1 is older than v2
         */
        @Override
        public int compare(String v1, String v2) {
            if (v1 == null && v2 != null) {
                return -1;
            }

            if (v2 == null && v1 != null) {
                return 1;
            }

            if (Objects.equals(v1, v2)) {
                return 0;
            }

            String[] strings1 = VERSION_SPLIT_REGEX.split(v1);
            String[] strings2 = VERSION_SPLIT_REGEX.split(v2);

            for (int i = 0; i < strings1.length; i++) {
                if (strings2.length <= i) {
                    return -1;
                }

                String s1 = strings1[i];
                String s2 = strings2[i];

                if (s1.equals(s2)) continue;
                if (s1.equals("SNAPSHOT")) return 1;
                if (s2.equals("SNAPSHOT")) return -1;

                try {
                    Integer n1 = Integer.valueOf(s1);
                    Integer n2 = Integer.valueOf(s2);
                    return n1 - n2;
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            }

            if (strings1.length < strings2.length) {
                return 1;
            }

            return 0;
        }
    }

}
