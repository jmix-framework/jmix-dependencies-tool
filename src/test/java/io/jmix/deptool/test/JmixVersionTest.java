package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import io.jmix.dependency.cli.version.JmixVersionUtils;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JmixVersionTest {

    @Test
    public void testVersionParsing() {
        String snapshotVersion = "2.5.999-SNAPSHOT";
        String rcVersion = "2.5.0-RC";
        String rc2Version = "2.5.0-RC2";
        String releaseVersion = "2.5.0";

        var supportedVersions = List.of(
                "1.5.0",
                releaseVersion,
                "2.5.0.1",
                snapshotVersion,
                rcVersion,
                "2.5.0.1-RC",
                rc2Version
        );
        assertDoesNotThrow(() -> supportedVersions.forEach(JmixVersion::from));

        JmixVersion snapshot = JmixVersion.from(snapshotVersion);
        assertEquals(snapshotVersion, snapshot.versionString());
        assertEquals("2.5.999", snapshot.versionString(false));
        assertNotEquals(snapshotVersion, snapshot.versionString(false));
        assertEquals("2.5.999", JmixVersionUtils.getStableVersion(snapshotVersion));
        assertTrue(snapshot.isSnapshot() && !snapshot.isStable() && !snapshot.isRC());

        JmixVersion rc = JmixVersion.from(rcVersion);
        assertEquals(rcVersion, rc.versionString());
        assertEquals("2.5.0", rc.versionString(false));
        assertEquals("-RC", rc.suffix());
        assertNotEquals(rcVersion, rc.versionString(false));
        assertTrue(rc.isRC() && !rc.isStable() && !rc.isSnapshot());

        JmixVersion rc2 = JmixVersion.from(rc2Version);
        assertEquals(rc2Version, rc2.versionString());
        assertEquals("2.5.0", rc2.versionString(false));
        assertEquals("-RC2", rc2.suffix());
        assertNotEquals(rc2Version, rc2.versionString(false));
        assertTrue(rc2.isRC() && !rc2.isStable() && !rc2.isSnapshot());

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

        var unsortedVersions = new ArrayList<>(List.of(
                "2.2.2",
                "1.5.0",
                "1.4.5",
                "3.1.0",
                "2.1.0"
        ));

        var sortedVersions = List.of(
                "3.1.0",
                "2.2.2",
                "2.1.0",
                "1.5.0",
                "1.4.5"
        );

        List<JmixVersion> unsortedJmixVersions = unsortedVersions.stream().map(JmixVersion::from).toList();
        List<JmixVersion> unsortedJmixVersions2 = new ArrayList<>(unsortedJmixVersions);

        assertIterableEquals(unsortedJmixVersions, unsortedJmixVersions2);

        List<JmixVersion> sortedJmixVersions = unsortedJmixVersions2.stream().sorted().collect(Collectors.toList());
        assertThrows(AssertionFailedError.class, () -> assertIterableEquals(sortedJmixVersions, unsortedJmixVersions));

        assertThrows(AssertionFailedError.class, () -> assertIterableEquals(sortedVersions, unsortedVersions));
        JmixVersionUtils.sort(unsortedVersions);
        assertIterableEquals(sortedVersions, unsortedVersions);
    }
}
