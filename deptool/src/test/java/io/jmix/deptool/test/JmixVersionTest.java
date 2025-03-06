package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class JmixVersionTest {

    @Test
    public void testVersionParsing() {
        String snapshotVersion = "2.5.999-SNAPSHOT";
        String rcVersion = "2.5.0-RC";
        String releaseVersion = "2.5.0";

        var supportedVersions = List.of(
                "1.5.0",
                releaseVersion,
                "2.5.0.1",
                snapshotVersion,
                rcVersion,
                "2.5.0.1-RC",
                "2.5.0-RC2"
        );
        assertDoesNotThrow(() -> supportedVersions.forEach(JmixVersion::from));

        JmixVersion snapshot = JmixVersion.from(snapshotVersion);
        assertEquals(snapshotVersion, snapshot.versionString());
        assertEquals("2.5.999", snapshot.versionString(false));
        assertNotEquals(snapshotVersion, snapshot.versionString(false));
        assertTrue(snapshot.isSnapshot() && !snapshot.isStable() && !snapshot.isRC());

        JmixVersion rc = JmixVersion.from(rcVersion);
        assertEquals(rcVersion, rc.versionString());
        assertEquals("2.5.0", rc.versionString(false));
        assertNotEquals(rcVersion, rc.versionString(false));
        assertTrue(rc.isRC() && !rc.isStable() && !rc.isSnapshot());

        JmixVersion release = JmixVersion.from(releaseVersion);
        assertEquals(releaseVersion, release.versionString());
        assertEquals(releaseVersion, release.versionString(false));
        assertTrue(release.isStable() && !release.isSnapshot() && !release.isSnapshot());

        var unsupportedVersions = List.of(
                "abc",
                "a.b.c",
                "1-2-3",
                "1.2.3b",
                "1.2.3.",
                "2.5"
        );
        assertThrows(Throwable.class, () -> unsupportedVersions.forEach(JmixVersion::from));
    }

    @Test
    public void testVersionComparing() {
        JmixVersion fixedVersion = JmixVersion.from("1.5.0");
        Function<JmixVersion, Integer> compareVersions = jmixVersion -> jmixVersion.compareTo(fixedVersion);

        JmixVersion dynamicVersion = JmixVersion.from("2.5.0");

        assertTrue(compareVersions.apply(dynamicVersion) > 0);
        assertTrue(compareVersions.apply(dynamicVersion.withMajor(1).withMinor(4)) < 0);
        assertEquals(0, compareVersions.apply(dynamicVersion.withMajor(1)));
    }
}
