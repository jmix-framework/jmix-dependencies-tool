package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Parameters(commandDescription = "Exports resolved dependencies")
public class ExportCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ExportCommand.class);

    @Parameter(names = {"--target-dir"}, description = "Export target directory")
    private String targetDirectory;

    @Parameter(names = {"--gradle-user-home"}, description = "Gradle user home directory for resolver project")
    private String gradleUserHome;

    @Override
    public void run() {
        if (gradleUserHome == null) {
            gradleUserHome = DefaultPaths.getDefaultGradleUserHome();
        }
        if (targetDirectory == null) {
            targetDirectory = DefaultPaths.getDefaultExportDir();
        }

        log.info("Target directory: {}", Paths.get(targetDirectory).toAbsolutePath().normalize());
        log.info("Gradle user home directory: {}", Paths.get(gradleUserHome).toAbsolutePath().normalize());

        Path gradleUserHomeDir = Paths.get(gradleUserHome);
        Path cachedGradleArtifactsDir = gradleUserHomeDir.resolve("caches/modules-2/files-2.1");
        Path targetDirectoryPath = Paths.get(targetDirectory);
        try {
            Files.walkFileTree(cachedGradleArtifactsDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(file) &&
                            (file.getFileName().toString().endsWith(".jar") ||
                                    file.getFileName().toString().endsWith(".pom") ||
                                    file.getFileName().toString().endsWith(".module"))) {
                        Path versionDir = file.getParent().getParent();
                        Path artifactDir = versionDir.getParent();
                        Path moduleDir = artifactDir.getParent();

                        String nestedOutputDirPath = moduleDir.getFileName().toString().replace(".", "/") + "/" +
                                artifactDir.getFileName().toString() + "/" + versionDir.getFileName().toString();

                        try {
                            Path outputDirectory = targetDirectoryPath.resolve(nestedOutputDirPath);
                            Path outputFile = outputDirectory.resolve(file.getFileName());
                            if (!Files.exists(outputFile)) {
                                log.debug("Copying file: {}", file.getFileName());
                                Files.createDirectories(outputDirectory);
                                Files.copy(file, outputFile);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Error while copying a file " + file.getFileName(), e);
                        }

                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error on copying files from gradle caches", e);
        }
        log.info("Export completed successfully");
    }
}
