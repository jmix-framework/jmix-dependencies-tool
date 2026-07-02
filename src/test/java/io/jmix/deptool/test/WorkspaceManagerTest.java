package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins checkpoint selection against the actually-bundled templates
 * ({@code build-1.0/2.0/2.1/2.4/2.4.2/3.0/plain}) and the real {@code gradle-versions.properties}.
 * Also exercises the classpath enumeration of templates.
 */
class WorkspaceManagerTest {

    private final WorkspaceManager wm = new WorkspaceManager(Paths.get("build", "test-workspace-unused"));

    private static JmixVersion v(String s) {
        return JmixVersion.from(s);
    }

    @Test
    void templateKey_selectsCheckpointAmongBundledTemplates() {
        assertEquals("plain", wm.selectTemplateKey(null)); // resolve-lib without a version
        assertEquals("1.0", wm.selectTemplateKey(v("1.5.0")));
        assertEquals("2.0", wm.selectTemplateKey(v("2.0.0")));
        assertEquals("2.0", wm.selectTemplateKey(v("2.0.5")));
        assertEquals("2.1", wm.selectTemplateKey(v("2.1.0")));
        assertEquals("2.1", wm.selectTemplateKey(v("2.3.0"))); // no build-2.3 -> nearest earlier checkpoint
        assertEquals("2.4", wm.selectTemplateKey(v("2.4.0")));
        assertEquals("2.4", wm.selectTemplateKey(v("2.4.1"))); // patch checkpoint 2.4.2 not yet reached
        assertEquals("2.4.2", wm.selectTemplateKey(v("2.4.2")));
        assertEquals("2.4.2", wm.selectTemplateKey(v("2.8.0"))); // no build-2.8 -> nearest earlier checkpoint
        assertEquals("3.0", wm.selectTemplateKey(v("3.0.0")));
        assertEquals("3.0", wm.selectTemplateKey(v("3.9.0"))); // forward-fallback to latest checkpoint
    }

    @Test
    void gradleVersion_followsCheckpointSelection() {
        // plain (no version), and versions below all checkpoints (no "default" key) -> the hardcoded default
        assertEquals(WorkspaceManager.DEFAULT_GRADLE_VERSION, wm.recommendedGradleVersion(null));
        assertEquals(WorkspaceManager.DEFAULT_GRADLE_VERSION, wm.recommendedGradleVersion(v("0.5.0")));
        // forward within a line: every version of a line maps to the same checkpoint value
        assertEquals(wm.recommendedGradleVersion(v("1.0.0")), wm.recommendedGradleVersion(v("1.9.0")));
        assertEquals(wm.recommendedGradleVersion(v("2.0.0")), wm.recommendedGradleVersion(v("2.8.0")));
        // distinct lines (1.0 / 2.0 / 3.0) map to distinct checkpoints
        assertNotEquals(wm.recommendedGradleVersion(v("1.5.0")), wm.recommendedGradleVersion(v("2.8.0")));
        assertNotEquals(wm.recommendedGradleVersion(v("2.8.0")), wm.recommendedGradleVersion(v("3.0.0")));
    }
}
