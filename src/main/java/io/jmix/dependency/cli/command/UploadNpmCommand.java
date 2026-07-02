package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.upload.NexusRepositoryManager;
import io.jmix.dependency.cli.upload.model.ArtifactNpm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;

@Parameters(commandDescription = "Uploads resolved and exported artifacts to Nexus")
public class UploadNpmCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(UploadNpmCommand.class);

    @Parameter(names = {"--nexus-url"}, description = "Nexus URL, e.g. http://localhost:8081", required = true)
    private String nexusUrl;

    @Parameter(names = {"--nexus-repository"}, description = "Nexus repository name, e.g. jmix", required = true)
    private String repositoryName;

    @Parameter(names = {"--nexus-username"}, description = "Nexus user login", required = true)
    private String username;

    @Parameter(names = {"--nexus-password"}, description = "Nexus user password", required = true)
    private String password;

    @Parameter(names = {"--artifacts-dir"},
            description = "Path to directory with exported artifacts to be uploaded, e.g. /opt/jmix/dependencies",
            required = true)
    private String artifactsDirectoryPath;

    @Override
    public void run() {
        Map<String, ArtifactNpm> artifacts = new TreeMap<>();

        log.info("Artifacts directory: {}", Paths.get(artifactsDirectoryPath).toAbsolutePath().normalize());
        NexusRepositoryManager nexusRepositoryManager = new NexusRepositoryManager(nexusUrl, repositoryName, username, password);
        try {
            Path rootLocalRepoDir = Paths.get(artifactsDirectoryPath);
            Files.walkFileTree(rootLocalRepoDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    log.debug("Processing file: {}", filePath);
                    if (!canProcessFile(filePath)) {
                        log.debug("Cannot process file {}. The file will not be uploaded", filePath.getFileName());
                        return FileVisitResult.CONTINUE;
                    }

                    Path relativeFilePath = rootLocalRepoDir.relativize(filePath);
                    log.debug("relativeFilePath: {}", relativeFilePath);
                    Path relativeParentPath = relativeFilePath.getParent();
                    log.debug("relativeParentPath: {}", relativeParentPath);
                    File parentFile = relativeParentPath.toFile();
                    log.debug("parentFile: {}", parentFile);
                    File file = filePath.toFile();
                    log.debug("file: {}", file);
                    ArtifactNpm artifact = ArtifactNpm.createFromPackage(file);
                    artifacts.putIfAbsent(artifact.getAsset(), artifact);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error on walking through local repository directory", e);
        }

        log.info("Artifact uploading started");

        for (ArtifactNpm artifact : artifacts.values()) {
            log.info("Process artifact '{}'", artifact.getAsset());
            if (!nexusRepositoryManager.isNpmArtifactUploaded(artifact)) {
                log.info("Start uploading artifact '{}'", artifact.getAsset());
                nexusRepositoryManager.uploadNpmArtifacts(artifact);
            } else {
                log.info("Artifact '{}' already uploaded", artifact.getAsset());
            }
        }

        log.info("Upload completed successfully");
    }

    private boolean canProcessFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".tgz");
    }
}
