package io.jmix.dependencies.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.util.List;

public class ResolveNpmDependenciesTask extends DefaultTask {

    private List<String> externalDependencies;

    private List<String> repositories;

    private String dependencyConfiguration;

    @Option(option = "dependency", description = "Maven coordinates of the dependency to be resolved, e.g. io.jmix.core:jmix-core:1.4.1")
    public void setExternalDependencies(List<String> externalDependencies) {
        this.externalDependencies = externalDependencies;
    }

    @Option(option = "dependency-configuration", description = "Gradle configuration name for resolvable dependency, e.g. implementation. " +
            "Implementation is used by default.")
    public void setDependencyConfiguration(String dependencyConfiguration) {
        this.dependencyConfiguration = dependencyConfiguration;
    }

    @Option(option = "repository", description = "Maven repository. URL, username and password are separated by |, e.g. " +
            "http://localhost:8081/myrepo|admin|admin or just an URL in case credentials are not required")
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    @TaskAction
    public void resolveDependencies() {
        addRepositories();
        addExternalDependencies();
        resolveProjectConfigurations();
    }

    private void resolveProjectConfigurations() {
        Project project = getProject();
        project.getConfigurations().stream()
                .filter(Configuration::isCanBeResolved)
                .forEach(Configuration::resolve);
    }

    private void addExternalDependencies() {
        System.out.println("Start 'addDependencies'");
        Project project = getProject();
        if (externalDependencies != null) {
            System.out.println("Dependencies: " + externalDependencies);
            String configurationName = dependencyConfiguration != null ? dependencyConfiguration : "implementation";
            if (project.getConfigurations().findByName(configurationName) == null) {
                project.getConfigurations().create(configurationName);
            }
            externalDependencies.forEach(dependency -> {
                project.getDependencies().add(configurationName, dependency);
            });
        }
        System.out.println("Finish 'addDependencies'");
    }

    private void addRepositories() {
        Project project = getProject();
        Logger logger = getLogger();
        if (repositories != null) {
            logger.info("Repositories: {}", repositories);
            for (String repository : repositories) {
                String[] parts = repository.split("\\|");
                if (parts.length != 1 && parts.length != 3) {
                    throw new RuntimeException("Invalid repository definition: " + repository + ". The string must contain three " +
                            "parts separated by |");
                }
                project.getRepositories().maven(mavenArtifactRepository -> {
                    String repositoryUrl = parts[0];
                    mavenArtifactRepository.setUrl(repositoryUrl);
                    if (parts.length > 1) {
                        mavenArtifactRepository.credentials(passwordCredentials -> {
                            passwordCredentials.setUsername(parts[1]);
                            passwordCredentials.setPassword(parts[2]);
                        });
                    }
                    if (repositoryUrl.startsWith("http://")) {
                        mavenArtifactRepository.setAllowInsecureProtocol(true);
                    }
                });
            }
        }
    }
}
