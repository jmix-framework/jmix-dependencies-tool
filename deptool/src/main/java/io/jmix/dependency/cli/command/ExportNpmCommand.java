package io.jmix.dependency.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandDescription = "Exports resolved dependencies")
public class ExportNpmCommand implements BaseCommand {

    private static final Logger log = LoggerFactory.getLogger(ExportNpmCommand.class);

    @Parameter(names = {"--package-lock-file"}, description = "Location of package-lock.json file with npm dependencies", order = 0)
    private String packageLockJson;

    @Parameter(names = {"--target-dir"}, description = "Export target directory", order = 1)
    private String targetDirectory;

    @Parameter(names = {"--resolver-project"}, description = "Path to dependencies resolver project", order = 2)
    private String resolverProjectPath;

    private String downloaderDir;

    @Override
    public void run() {
        if (targetDirectory == null) {
            targetDirectory = DefaultPaths.getDefaultExportNpmDir();
        }
        if (resolverProjectPath == null) {
            resolverProjectPath = DefaultPaths.getDefaultNpmResolverProjectPath();
        }
        if (packageLockJson == null) {
            packageLockJson = resolverProjectPath + "/package-lock.json";
        }

        downloaderDir = resolverProjectPath + "/downloader";

        Path targetDirectoryPath = Paths.get(targetDirectory).toAbsolutePath().normalize();
        Path packageLockJsonPath = Paths.get(packageLockJson).toAbsolutePath().normalize();

        log.info("Target directory: {}", targetDirectoryPath);
        log.info("package-lock.json file: {}", packageLockJsonPath);

        installNodeTgzDownloader();
        downloadNpmArchives(packageLockJsonPath.toString(), targetDirectoryPath.toString());
        copyPackageLock();

        log.info("Export completed successfully");
    }

    protected void installNodeTgzDownloader() {
        log.info("-= Install node-tgz-downloader =-");
        executeCommand(downloaderDir, "npm install node-tgz-downloader");
    }

    protected void downloadNpmArchives(String packageLockFileLocation, String directory) {
        log.info("-= Download tgz-archives =-");
        String command = "npx download-tgz package-lock " + packageLockFileLocation + " --directory=" + directory;
        executeCommand(downloaderDir, command);
    }

    protected void copyPackageLock() {
        log.info("-= Copy package-lock.json =-");
        File source = new File(packageLockJson);
        File target = new File(targetDirectory);
        try {
            FileUtils.copyFileToDirectory(source, target);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy package-lock.json", e);
        }
    }

    protected void executeCommand(String workingDirectory, String command) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        Path workingDirectoryPath = Paths.get(workingDirectory);
        String commandString = String.join(" ", builder.command());
        log.info("Working directory: {}, command: {}", workingDirectoryPath, commandString);
        if (!Files.exists(workingDirectoryPath)) {
            try {
                Files.createDirectories(workingDirectoryPath);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create working directory " + workingDirectoryPath, e);
            }
        }
        File workingDirectoryFile = workingDirectoryPath.toAbsolutePath().normalize().toFile();
        builder.directory(workingDirectoryFile);

        builder.inheritIO();
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException("Command failed", e);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("Exit code = " + exitCode);
        }
    }
}
