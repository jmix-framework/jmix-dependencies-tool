package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jmix.dependency.cli.upload.NexusRepositoryManager;
import io.jmix.dependency.cli.upload.model.Artifact;
import io.jmix.dependency.cli.upload.model.ArtifactsBundle;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;

@Parameters(commandDescription = "Uploads resolved and exported artifacts to Nexus")
public class UploadCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(UploadCommand.class);

    @Parameter(names = {"--nexus-url"}, description = "Nexus URL, e.g. http://localhost:8081", required = true)
    private String nexusUrl;

    @Parameter(names = {"--nexus-repository"}, description = "Nexus repository name, e.g. jmix", required = true)
    private String repositoryName;

    @Parameter(names = {"--nexus-username"}, description = "Nexus user login", required = true)
    private String username;

    @Parameter(names = {"--nexus-password"}, description = "Nexus user password", required = true)
    private String password;

    //todo rename parameter?
    @Parameter(names = {"--artifacts-dir"},
            description = "Path to directory with exported artifacts to be uploaded, e.g. /opt/jmix/dependencies",
            required = true)
    private String artifactsDirectoryPath;

    @Override
    public void run() {
        Map<String, ArtifactsBundle> artifactBundles = new TreeMap<>();

        log.info("Artifacts directory: {}", Paths.get(artifactsDirectoryPath).toAbsolutePath().normalize());

        NexusRepositoryManager nexusRepositoryManager = new NexusRepositoryManager(nexusUrl, repositoryName, username, password);
        try {
            Path rootLocalRepoDir = Paths.get(artifactsDirectoryPath);
            Files.walkFileTree(rootLocalRepoDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.debug("Processing file: {}", file);
                    if (!canProcessFile(file)) {
                        log.debug("Cannot process file {}. The file will not be uploaded", file.getFileName());
                        return FileVisitResult.CONTINUE;
                    }
                    Path versionDir = file.getParent();
                    Path artifactDir = versionDir.getParent();
                    Path relativizedGroupPath = rootLocalRepoDir.relativize(artifactDir).getParent();
                    String groupId = relativizedGroupPath.toString().replace(File.separator, ".");
                    String artifactId = artifactDir.getFileName().toString();
                    String version = versionDir.getFileName().toString();
                    String mavenCoordinates = groupId + ":" + artifactId + ":" + version;
                    ArtifactsBundle artifactsBundle = artifactBundles.computeIfAbsent(mavenCoordinates,
                            (key) -> new ArtifactsBundle(groupId, artifactId, version));
                    Artifact artifact = new Artifact(groupId, artifactId, version, extractClassifier(file, artifactId, version), file.toFile());
                    artifactsBundle.addArtifact(artifact);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error on walking through local repository directory", e);
        }

        log.info("Artifact uploading started");

        for (ArtifactsBundle artifactsBundle : artifactBundles.values()) {
            Artifact pomArtifact = artifactsBundle.getArtifacts().stream().filter(artifact -> "pom".equals(artifact.getExtension()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Cannot find POM artifact for " + artifactsBundle.getMavenCoordinates()));
            if (!nexusRepositoryManager.isArtifactUploaded(pomArtifact)) {
                nexusRepositoryManager.uploadArtifacts(artifactsBundle);
            } else {
                log.debug("Artifact {} already uploaded", artifactsBundle.getMavenCoordinates());
            }
        }

        log.info("Upload completed successfully");
    }

    private boolean canProcessFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".pom") || fileName.endsWith(".jar") || fileName.endsWith(".module");
    }

    private String extractClassifier(Path path, String artifactId, String version) {
        String fileName = path.getFileName().toString();
        String fileNameNoExt = FilenameUtils.removeExtension(fileName);

        String prefix = artifactId + "-" + version + "-";
        return fileNameNoExt.startsWith(prefix)
                ? fileNameNoExt.substring(prefix.length())
                : null;
    }
}
