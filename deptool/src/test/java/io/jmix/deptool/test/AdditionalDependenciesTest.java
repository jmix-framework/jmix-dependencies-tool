package io.jmix.deptool.test;

import io.jmix.dependency.cli.version.JmixVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.jmix.dependency.cli.dependency.additional.AdditionalDependencyFileType.PACKAGE_LOCK;

public class AdditionalDependenciesTest {

    @Test
    public void testNpmDependencies() throws IOException {
        JmixVersion jmixVersion = JmixVersion.from("2.5.0");
        try (var fileContent = PACKAGE_LOCK.findFileContent(jmixVersion)) {
            byte[] package_lock_jmix_2_5 = fileContent.readAllBytes();
            Assertions.assertNotNull(package_lock_jmix_2_5);
            testPackageJson(jmixVersion.withPatch(5), package_lock_jmix_2_5);
            testPackageJson(jmixVersion.withPatch(999).withSuffix("-SNAPSHOT"), package_lock_jmix_2_5);
            testPackageJson(jmixVersion.withSuffix("-RC1"), package_lock_jmix_2_5);
            Assertions.assertThrows(NullPointerException.class, () ->
                    testPackageJson(jmixVersion.withMajor(1), package_lock_jmix_2_5));
        }
    }

    private void testPackageJson(JmixVersion jmixVersion, byte[] expected) throws IOException {
        try (var content = PACKAGE_LOCK.findFileContent(jmixVersion)) {
            byte[] actual = content.readAllBytes();
            Assertions.assertNotNull(actual);
            Assertions.assertArrayEquals(expected, actual);
        }
    }
}
