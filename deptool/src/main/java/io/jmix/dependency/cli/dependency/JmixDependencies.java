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

    public static Set<String> getVersionSpecificJmixDependencies(String jmixVersion) {
        String minorJmixVersion = getMinorVersion(jmixVersion);
        Set<String> dependencies = _getVersionSpecificDependencies(minorJmixVersion);
        dependencies.addAll(_getVersionSpecificDependencies(jmixVersion));
        return dependencies;
    }

    private static Set<String> _getVersionSpecificDependencies(String version) {
        try (InputStream is = JmixDependencies.class.getResourceAsStream("/jmix-dependencies/" + version + ".xml")) {
            if (is == null) {
                log.debug("Dependencies file for version {} not found", version);
                return new HashSet<>();
            }
            log.debug("Parsing dependencies file: {}", version + ".xml");
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            List<Node> nodes = document.selectNodes("/dependencies/dependency");
            return nodes.stream()
                    .map(node -> node.getText())
                    .collect(Collectors.toSet());
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
