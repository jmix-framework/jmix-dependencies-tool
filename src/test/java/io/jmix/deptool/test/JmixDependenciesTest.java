package io.jmix.deptool.test;

import io.jmix.dependency.cli.dependency.DependencyScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.jmix.dependency.cli.dependency.JmixDependencies.getVersionSpecificJmixDependencies;

public class JmixDependenciesTest {

    @Test
    public void testDependenciesFileParsing() {
        // test 2.5 file
        testDependenciesParsing(new DependenciesParsingConfig("2.5.1", 136, 135, 165, 164));

        // test 1.6 file
        testDependenciesParsing(new DependenciesParsingConfig("1.6.0", 129, 129, 155, 155));
    }

    @Test
    public void unknownVersionFailsInsteadOfFallingBackToDefault() {
        // There is no dependencies-999.999.xml. With the default descriptor removed, the process must fail
        // loudly rather than silently resolving an obviously-incorrect dependency set for the requested version.
        Assertions.assertThrows(IllegalStateException.class,
                () -> getVersionSpecificJmixDependencies(DependencyScope.JVM, "999.999.999", false));
    }

    private static void testDependenciesParsing(DependenciesParsingConfig config) {
        Set<String> openSourceJvmDependencies = getVersionSpecificJmixDependencies(DependencyScope.JVM, config.jmixVersion, false);
        Assertions.assertEquals(config.expectedJvmOpenSourceDependencies, openSourceJvmDependencies.size());

        Set<String> openSourceNpmDependencies = getVersionSpecificJmixDependencies(DependencyScope.NPM, config.jmixVersion, false);
        Assertions.assertEquals(config.expectedNpmOpenSourceDependencies, openSourceNpmDependencies.size());

        Set<String> allJvmDependencies = getVersionSpecificJmixDependencies(DependencyScope.JVM, config.jmixVersion, true);
        Assertions.assertEquals(config.expectedJvmAllDependencies, allJvmDependencies.size());

        Set<String> allNpmDependencies = getVersionSpecificJmixDependencies(DependencyScope.NPM, config.jmixVersion, true);
        Assertions.assertEquals(config.expectedNpmAllDependencies, allNpmDependencies.size());
    }

    private record DependenciesParsingConfig(
            String jmixVersion,

            int expectedJvmOpenSourceDependencies,
            int expectedNpmOpenSourceDependencies,

            int expectedJvmAllDependencies,
            int expectedNpmAllDependencies
    ) {}
}
