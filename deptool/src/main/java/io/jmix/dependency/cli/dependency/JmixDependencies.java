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

    private static final List<String> DATABASE_DRIVER_DEPENDENCIES = List.of(
            "org.hsqldb:hsqldb",
            "org.postgresql:postgresql",
            "mysql:mysql-connector-java",
            "org.mariadb.jdbc:mariadb-java-client",
            "com.microsoft.sqlserver:mssql-jdbc",
            "com.oracle.database.jdbc:ojdbc8"
    );

    public static Set<String> getVersionSpecificJmixDependencies(String jmixVersion, boolean resolveCommercialAddons) {
        String minorJmixVersion = getMinorVersion(jmixVersion);
        Set<String> dependencies = _getVersionSpecificDependencies(minorJmixVersion, resolveCommercialAddons);
        dependencies.addAll(_getVersionSpecificDependencies(jmixVersion, resolveCommercialAddons));
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getDatabaseDriverDependencies() {
        return DATABASE_DRIVER_DEPENDENCIES;
    }

    /**
     * Extracts minor version from version string, e.g. 1.4.2 -> 1.4
     */
    private static String getMinorVersion(String jmixVersion) {
        String[] parts = jmixVersion.split("\\.");
        return parts[0] + "." + parts[1];
    }

}
