package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validates version-to-template selection and the parallel Gradle-version lookup.
 * <p>
 * These tests run against a <b>stable fixture set</b> of templates under {@code src/test/resources/test-templates}
 * (checkpoints {@code 1.0, 2.0, 2.1, 2.3.1, 3.0} + {@code plain}, and a fixture {@code gradle-versions.properties}
 * with distinctive values {@code 1.1.1 / 2.2.2 / 3.3.3}), <em>not</em> the shipped templates. So adding or
 * changing a production {@code build-<x>.gradle} never breaks these assertions - the test only checks that file
 * selection by version and the classpath enumeration behind it are correct. The pure selection algorithm is
 * covered separately by {@code CheckpointSelectorTest}.
 */
class WorkspaceManagerTest {

    private final WorkspaceManager wm =
            new WorkspaceManager(Paths.get("build", "test-workspace-unused"), "test-templates");

    private static JmixVersion v(String s) {
        return JmixVersion.from(s);
    }

    @Test
    void templateKey_selectsGreatestCheckpointNotAboveVersion() {
        assertEquals("plain", wm.selectTemplateKey(null));          // resolve-lib without a version
        assertEquals("1.0", wm.selectTemplateKey(v("1.5.0")));
        assertEquals("2.0", wm.selectTemplateKey(v("2.0.0")));
        assertEquals("2.0", wm.selectTemplateKey(v("2.0.5")));
        assertEquals("2.1", wm.selectTemplateKey(v("2.1.0")));
        assertEquals("2.1", wm.selectTemplateKey(v("2.3.0")));      // before the patch checkpoint 2.3.1
        assertEquals("2.3.1", wm.selectTemplateKey(v("2.3.1")));    // exact patch checkpoint
        assertEquals("2.3.1", wm.selectTemplateKey(v("2.3.5")));    // later patch
        assertEquals("2.3.1", wm.selectTemplateKey(v("2.8.0")));    // patch checkpoint applies forward
        assertEquals("3.0", wm.selectTemplateKey(v("3.0.0")));
        assertEquals("3.0", wm.selectTemplateKey(v("3.9.0")));      // forward-fallback to latest checkpoint
    }

    @Test
    void templateKey_failsWhenNoCheckpointMatches() {
        // below the lowest fixture checkpoint (1.0) and there is no "default" -> hard failure, no silent fallback
        assertThrows(IllegalStateException.class, () -> wm.selectTemplateKey(v("0.5.0")));
    }

    @Test
    void enumeration_ignoresPlainAndNonVersionTemplates() {
        // 'plain' and the 'experimental' decoy are not version checkpoints, so a version at/above the lowest
        // real checkpoint must never resolve to them.
        assertEquals("1.0", wm.selectTemplateKey(v("1.0.0")));
    }

    @Test
    void gradleVersion_followsCheckpointSelectionUsingFixtureValues() {
        // plain (no version), and versions below all checkpoints (no "default" key) -> the hardcoded default
        assertEquals(WorkspaceManager.DEFAULT_GRADLE_VERSION, wm.recommendedGradleVersion(null));
        assertEquals(WorkspaceManager.DEFAULT_GRADLE_VERSION, wm.recommendedGradleVersion(v("0.5.0")));
        // distinctive fixture values prove the lookup reads test-templates/gradle-versions.properties
        assertEquals("1.1.1", wm.recommendedGradleVersion(v("1.5.0")));
        assertEquals("2.2.2", wm.recommendedGradleVersion(v("2.0.0")));
        assertEquals("2.2.2", wm.recommendedGradleVersion(v("2.8.0"))); // forward within the 2.x line
        assertEquals("3.3.3", wm.recommendedGradleVersion(v("3.0.0")));
        // distinct lines map to distinct values
        assertNotEquals(wm.recommendedGradleVersion(v("1.5.0")), wm.recommendedGradleVersion(v("2.8.0")));
        assertNotEquals(wm.recommendedGradleVersion(v("2.8.0")), wm.recommendedGradleVersion(v("3.0.0")));
    }

    @Test
    void effectiveGradleVersion_prefersExplicitOverride() {
        assertEquals("9.9.9", wm.effectiveGradleVersion(v("2.8.0"), "9.9.9"));
        assertEquals("2.2.2", wm.effectiveGradleVersion(v("2.8.0"), null)); // falls back to the recommendation
    }
}
