package io.jmix.dependency.cli.dependency;

import io.jmix.dependency.cli.version.JmixVersionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JmixDependencies {

    private static final Logger log = LoggerFactory.getLogger(JmixDependencies.class);

    public static Set<String> getVersionSpecificJmixDependencies(String jmixVersion, boolean resolveCommercialAddons) {
        String minorJmixVersion = JmixVersionUtils.getMinorVersion(jmixVersion);
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

}
