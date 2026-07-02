package io.jmix.dependency.cli.command;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default locations, all relative to the working directory (typically {@code deptool/bin}).
 * <p>
 * v3 no longer ships {@code resolver}/{@code npm-resolver} projects; instead it generates Gradle
 * workspaces under {@code ../work}. NPM artifacts (generated lockfile + workspace) live under
 * {@code ../npm-work}.
 */
public class DefaultPaths {

    private static Path base() {
        return Paths.get(System.getProperty("user.dir"));
    }

    public static String getDefaultGradleUserHome() {
        return base().resolve("../gradle-home").toAbsolutePath().toString();
    }

    /** Root under which the generated Java/NPM resolution workspaces are created. */
    public static String getDefaultWorkspaceRoot() {
        return base().resolve("../work").toAbsolutePath().toString();
    }

    public static String getDefaultExportDir() {
        return base().resolve("../export").toAbsolutePath().toString();
    }

    public static String getDefaultExportNpmDir() {
        return base().resolve("../export-npm").toAbsolutePath().toString();
    }

    /** Where resolve-npm copies the resolved package-lock.json and export-npm reads it from by default. */
    public static String getDefaultNpmLockFile() {
        return base().resolve("../npm-work/package-lock.json").toAbsolutePath().toString();
    }
}
