package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.workspace.CheckpointSelector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CheckpointSelectorTest {

    private static JmixVersion v(String s) {
        return JmixVersion.from(s);
    }

    @Test
    void parse_normalizesAndRejects() {
        assertEquals("2.1.0", CheckpointSelector.parse("2.1").versionString(false));
        assertEquals("2.0.0", CheckpointSelector.parse("2.x").versionString(false)); // .x -> .0
        assertEquals("2.3.1", CheckpointSelector.parse("2.3.1").versionString(false));
        assertEquals("2.0.0", CheckpointSelector.parse("2").versionString(false));
        assertNull(CheckpointSelector.parse("default"));
        assertNull(CheckpointSelector.parse("plain"));
        assertNull(CheckpointSelector.parse(""));
        assertNull(CheckpointSelector.parse(null));
        assertNull(CheckpointSelector.parse("abc"));
    }

    @Test
    void minorCheckpoint_appliesForwardNotBackward() {
        List<String> keys = List.of("2.1");
        assertEquals("2.1", CheckpointSelector.select(keys, v("2.8.0"), false));
        assertNull(CheckpointSelector.select(keys, v("2.0.0"), false)); // 2.1 > 2.0.0 -> no match
    }

    @Test
    void patchCheckpoint_appliesFromThatPatchForward() {
        List<String> keys = List.of("2.1", "2.3.1");
        assertEquals("2.1", CheckpointSelector.select(keys, v("2.3.0"), false));   // before the patch
        assertEquals("2.3.1", CheckpointSelector.select(keys, v("2.3.1"), false)); // exact
        assertEquals("2.3.1", CheckpointSelector.select(keys, v("2.3.5"), false)); // later patch
        assertEquals("2.3.1", CheckpointSelector.select(keys, v("2.4.0"), false)); // later minor
    }

    @Test
    void forwardFallback_picksLatestCheckpointForNewerVersion() {
        List<String> keys = List.of("2.1", "3.0");
        assertEquals("3.0", CheckpointSelector.select(keys, v("4.0.0"), false));
    }

    @Test
    void belowLowest_returnsNullOrDefault() {
        assertNull(CheckpointSelector.select(List.of("2.0", "2.1"), v("1.5.0"), false));
        assertEquals("default", CheckpointSelector.select(List.of("2.0", "2.1", "default"), v("1.5.0"), true));
    }

    @Test
    void picksGreatestAmongMany() {
        List<String> keys = List.of("1.0", "2.0", "2.1", "3.0");
        assertEquals("2.0", CheckpointSelector.select(keys, v("2.0.5"), false));
        assertEquals("2.1", CheckpointSelector.select(keys, v("2.1.0"), false));
        assertEquals("2.1", CheckpointSelector.select(keys, v("2.8.0"), false));
        assertEquals("3.0", CheckpointSelector.select(keys, v("3.4.0"), false));
        assertEquals("1.0", CheckpointSelector.select(keys, v("1.9.0"), false));
    }

    @Test
    void snapshot_matchesStableCheckpoint() {
        assertEquals("2.1", CheckpointSelector.select(List.of("2.0", "2.1"), v("2.8.0-SNAPSHOT"), false));
    }

    @Test
    void xKey_treatedAsMajorZero() {
        assertEquals("2.x", CheckpointSelector.select(List.of("2.x"), v("2.5.0"), false));
        assertNull(CheckpointSelector.select(List.of("3.x"), v("2.5.0"), false)); // 3.x == 3.0 > 2.5.0
    }
}
