package io.jmix.dependency.cli.dependency;

import io.jmix.dependency.cli.version.JmixVersionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
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

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public class JmixDependencies {

    private static final Logger log = LoggerFactory.getLogger(JmixDependencies.class);

    public static Set<String> getVersionSpecificJmixDependencies(DependencyScope scope,
                                                                 String jmixVersion,
                                                                 boolean resolveCommercialAddons) {
        return getVersionSpecificJmixDependencies(scope, jmixVersion, resolveCommercialAddons, null);
    }

    public static Set<String> getVersionSpecificJmixDependencies(DependencyScope scope,
                                                                 String jmixVersion,
                                                                 boolean resolveCommercialAddons,
                                                                 SubscriptionPlan plan) {
        String minorJmixVersion = JmixVersionUtils.getMinorVersion(jmixVersion);

        // A dependencies descriptor for the target version MUST be present. There is deliberately NO fallback:
        // silently resolving against a generic/default descriptor would produce a result that is obviously
        // incorrect for the requested version and hide the real problem (an unsupported version). Fail loudly
        // instead - see "Supporting a new framework version" in the README.
        boolean minorPresent = dependenciesDescriptorExists(minorJmixVersion);
        boolean patchPresent = dependenciesDescriptorExists(jmixVersion);
        if (!minorPresent && !patchPresent) {
            throw new IllegalStateException(
                    "No dependencies descriptor found for Jmix version '" + jmixVersion + "'. Expected classpath " +
                    "resource '/jmix-dependencies/dependencies-" + minorJmixVersion + ".xml'" +
                    " (or '/jmix-dependencies/dependencies-" + jmixVersion + ".xml'). " +
                    "Add 'dependencies-" + minorJmixVersion + ".xml' for this version and retry.");
        }

        Set<String> dependencies = new HashSet<>();
        if (minorPresent) {
            dependencies.addAll(_getVersionSpecificDependencies(scope, minorJmixVersion, resolveCommercialAddons, plan));
        }
        if (patchPresent) {
            dependencies.addAll(_getVersionSpecificDependencies(scope, jmixVersion, resolveCommercialAddons, plan));
        }
        return dependencies;
    }

    private static boolean dependenciesDescriptorExists(String version) {
        return JmixDependencies.class.getResource("/jmix-dependencies/dependencies-" + version + ".xml") != null;
    }

    private static Set<String> _getVersionSpecificDependencies(DependencyScope scope,
                                                               String version,
                                                               boolean resolveCommercialAddons,
                                                               SubscriptionPlan plan) {
        try (InputStream is = JmixDependencies.class.getResourceAsStream("/jmix-dependencies/dependencies-" + version + ".xml")) {
            if (is == null) {
                log.debug("Dependencies file for version {} not found", version);
                return new HashSet<>();
            }

            log.debug("Parsing dependencies file: {}", version + ".xml");
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            List<Node> nodes = document.selectNodes("/dependencies/open-source-dependencies/dependency");
            Set<String> dependencies = nodes.stream()
                    .filter(node -> filterByScope(node, scope))
                    .map(Node::getText)
                    .collect(Collectors.toSet());

            if (resolveCommercialAddons) {
                SubscriptionPlan effectivePlan = plan == null ? SubscriptionPlan.BPM : plan;

                List<Node> commercialNodes = document.selectNodes("/dependencies/commercial-dependencies/dependency");
                Set<String> commercialDependencies = commercialNodes.stream()
                        .filter(node -> filterByScope(node, scope))
                        .map(Node::getText)
                        .filter(dependency -> {
                            MavenCoordinates mavenCoordinates = MavenCoordinates.parse(dependency);
                            return effectivePlan.isDependencyAllowed(mavenCoordinates);
                        })
                        .collect(Collectors.toSet());
                dependencies.addAll(commercialDependencies);
            }

            return dependencies;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean filterByScope(Node node, DependencyScope scope) {
        if (scope == DependencyScope.ALL) {
            return true;
        }

        if (node instanceof Element element) {
            String nodeScope = element.attributeValue("scope");
            if (StringUtils.isBlank(nodeScope)) {
                return true;
            }

            return switch (scope) {
                case JVM -> equalsIgnoreCase(DependencyScope.JVM.name(), nodeScope);
                case NPM -> equalsIgnoreCase(DependencyScope.NPM.name(), nodeScope);
                default -> true;
            };
        }

        return true;
    }

}
