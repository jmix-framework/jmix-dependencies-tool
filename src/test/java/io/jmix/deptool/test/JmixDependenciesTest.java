package io.jmix.deptool.test;

import io.jmix.dependency.cli.dependency.DependencyScope;
import io.jmix.dependency.cli.dependency.SubscriptionPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.jmix.dependency.cli.dependency.JmixDependencies.getVersionSpecificJmixDependencies;

/**
 * Validates the dependency-descriptor parsing logic: scope filtering (JVM / NPM / both), commercial-add-on
 * inclusion and subscription-plan filtering, and the minor+patch merge with de-duplication.
 * <p>
 * These tests run against a <b>stable fixture set</b> under {@code src/test/resources/test-jmix-dependencies}
 * (a made-up {@code 9.9} line), <em>not</em> the shipped {@code dependencies-*.xml}. So editing a real
 * descriptor (e.g. adding compileOnly deps to a version) never breaks these assertions - the fixture pins
 * exactly what the logic should count. A separate smoke test confirms a shipped descriptor still parses.
 */
public class JmixDependenciesTest {

    /** Test fixture resource base (mirrors production "/jmix-dependencies"). */
    private static final String FIXTURE = "/test-jmix-dependencies";

    private static Set<String> deps(DependencyScope scope, String version, boolean commercial, SubscriptionPlan plan) {
        return getVersionSpecificJmixDependencies(FIXTURE, scope, version, commercial, plan);
    }

    @Test
    public void scopeFiltering_andMinorPatchMerge() {
        // "9.9.1" = minor 9.9 + patch 9.9.1 merged, de-duplicated.
        // JVM open-source: both-a, both-b, jvm-a, jvm-b, jvm-c (from 9.9) + patch-jvm (9.9.1); jvm-a dup dropped.
        Assertions.assertEquals(6, deps(DependencyScope.JVM, "9.9.1", false, null).size());
        // NPM open-source: both-a, both-b, example-npm-a (the patch adds only JVM entries).
        Assertions.assertEquals(3, deps(DependencyScope.NPM, "9.9.1", false, null).size());
    }

    @Test
    public void minorOnly_whenNoPatchDescriptor() {
        // "9.9.5" -> only minor 9.9 exists (no dependencies-9.9.5.xml).
        Assertions.assertEquals(5, deps(DependencyScope.JVM, "9.9.5", false, null).size());
        Assertions.assertEquals(3, deps(DependencyScope.NPM, "9.9.5", false, null).size());
    }

    @Test
    public void commercialAddons_respectSubscriptionPlan() {
        // BPM (default) allows all commercial: JVM open-source (6) + bpm-a + tab-a = 8.
        Assertions.assertEquals(8, deps(DependencyScope.JVM, "9.9.1", true, null).size());
        Assertions.assertEquals(8, deps(DependencyScope.JVM, "9.9.1", true, SubscriptionPlan.BPM).size());
        // ENTERPRISE excludes the io.jmix.bpm group: 6 + tab-a = 7.
        Assertions.assertEquals(7, deps(DependencyScope.JVM, "9.9.1", true, SubscriptionPlan.ENTERPRISE).size());
        // Commercial deps here are JVM-scoped, so NPM is unaffected by commercial resolution.
        Assertions.assertEquals(3, deps(DependencyScope.NPM, "9.9.1", true, null).size());
    }

    @Test
    public void unknownVersionFailsInsteadOfFallingBackToDefault() {
        // No dependencies-7.7.xml in the fixture: must fail loudly rather than resolve an incorrect set.
        Assertions.assertThrows(IllegalStateException.class,
                () -> deps(DependencyScope.JVM, "7.7.7", false, null));
    }

    @Test
    public void shippedDescriptorStillParses() {
        // Smoke test against a real descriptor - non-brittle (no exact count), just confirms it is readable.
        Assertions.assertFalse(
                getVersionSpecificJmixDependencies(DependencyScope.JVM, "3.0.0", false).isEmpty());
    }
}
