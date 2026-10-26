package io.jmix.deptool.test;

import io.jmix.dependency.cli.command.UploadNpmCommand;
import io.jmix.dependency.cli.upload.model.ArtifactNpm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates how upload-npm identifies a tarball from the export-npm layout: {@code <name>/<name>-<version>.tgz}
 * for unscoped packages and {@code @<scope>/<name>/<name>-<version>.tgz} for scoped ones.
 * <p>
 * Regression: {@code react@19.3.0} and {@code @types/react@19.3.0} share the file name {@code react-19.3.0.tgz};
 * the scope must be kept, otherwise one of them is never uploaded.
 */
class ArtifactNpmTest {

    private static final Path ROOT = Paths.get("export-npm");

    @Test
    void createFromPackage_unscoped() {
        ArtifactNpm artifact = ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("react/react-19.3.0.tgz"));

        assertEquals("react", artifact.getName());
        assertEquals("19.3.0", artifact.getVersion());
        assertEquals("react@19.3.0", artifact.getKey());
        assertEquals("react/-/react-19.3.0.tgz", artifact.getRepositoryPath());
    }

    @Test
    void createFromPackage_scoped() {
        ArtifactNpm artifact = ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("@types/react/react-19.3.0.tgz"));

        assertEquals("@types/react", artifact.getName());
        assertEquals("19.3.0", artifact.getVersion());
        assertEquals("@types/react@19.3.0", artifact.getKey());
        assertEquals("@types/react/-/react-19.3.0.tgz", artifact.getRepositoryPath());
    }

    @Test
    void createFromPackage_hyphenatedNameAndPrereleaseVersion() {
        ArtifactNpm artifact = ArtifactNpm.createFromPackage(ROOT,
                ROOT.resolve("@vaadin/vaadin-lumo-styles/vaadin-lumo-styles-25.3.0-beta1.tgz"));

        assertEquals("@vaadin/vaadin-lumo-styles", artifact.getName());
        assertEquals("25.3.0-beta1", artifact.getVersion());
    }

    @Test
    void createFromPackage_rejectsUnexpectedLayout() {
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("react-19.3.0.tgz")));
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("types/react/react-19.3.0.tgz")));
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("react/react-dom-19.3.0.tgz")));
        // An npm name has at most one scope level, so nothing deeper than @scope/name/<file> is valid
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("@scope/group/react/react-19.3.0.tgz")));
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("@types/react/node_modules/csstype/csstype-3.1.3.tgz")));
    }

    @Test
    void matchesRepositoryPath_distinguishesScope() {
        ArtifactNpm react = ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("react/react-19.3.0.tgz"));
        ArtifactNpm typesReact = ArtifactNpm.createFromPackage(ROOT, ROOT.resolve("@types/react/react-19.3.0.tgz"));

        // Nexus search by name=react&version=19.3.0 returns this path for @types/react
        assertFalse(react.matchesRepositoryPath("/@types/react/-/react-19.3.0.tgz"));
        assertTrue(typesReact.matchesRepositoryPath("/@types/react/-/react-19.3.0.tgz"));
        assertTrue(react.matchesRepositoryPath("/react/-/react-19.3.0.tgz"));
        assertTrue(react.matchesRepositoryPath("react/-/react-19.3.0.tgz"));
        assertFalse(react.matchesRepositoryPath(null));
    }

    @Test
    void collectArtifacts_keepsPackagesWithSameFileName(@TempDir Path dir) throws IOException {
        createFile(dir.resolve("@types/react/react-19.3.0.tgz"));
        createFile(dir.resolve("react/react-19.3.0.tgz"));
        createFile(dir.resolve("@types/react-dom/react-dom-19.3.0.tgz"));
        createFile(dir.resolve("react-dom/react-dom-19.3.0.tgz"));
        createFile(dir.resolve("package-lock.json"));

        Map<String, ArtifactNpm> artifacts = UploadNpmCommand.collectArtifacts(dir);

        assertEquals(Set.of("@types/react@19.3.0", "react@19.3.0", "@types/react-dom@19.3.0", "react-dom@19.3.0"),
                artifacts.keySet());
    }

    @Test
    void collectArtifacts_skipsFilesOutsideTheLayout(@TempDir Path dir) throws IOException {
        createFile(dir.resolve("react/react-19.3.0.tgz"));
        createFile(dir.resolve("stray-1.0.0.tgz"));

        assertEquals(Set.of("react@19.3.0"), UploadNpmCommand.collectArtifacts(dir).keySet());
    }

    private static void createFile(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");
    }
}
