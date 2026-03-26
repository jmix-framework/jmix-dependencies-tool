package io.jmix.dependencies.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.util.List;

public class ResolveNpmDependenciesTask extends DefaultTask {

    @Deprecated
    private List<String> repositories;

    private String dependencyConfiguration;

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
        resolveProjectConfigurations();
    }

    private void resolveProjectConfigurations() {
        Project project = getProject();
        project.getConfigurations().stream()
                .filter(Configuration::isCanBeResolved)
                .forEach(Configuration::resolve);
    }
}
