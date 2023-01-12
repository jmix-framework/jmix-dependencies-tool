package io.jmix.dependency.cli.command;

import java.nio.file.Paths;

public class DefaultPaths {

    public static String getDefaultGradleUserHome() {
        return Paths.get(System.getProperty("user.dir")).resolve("../gradle-home").toAbsolutePath().toString();
    }

    public static String getDefaultResolverProjectPath() {
        return Paths.get(System.getProperty("user.dir")).resolve("../resolver").toAbsolutePath().toString();
    }

    public static String getDefaultExportDir() {
        return Paths.get(System.getProperty("user.dir")).resolve("../jmix-dependencies").toAbsolutePath().toString();
    }

}
