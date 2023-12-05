package io.jmix.dependencies;

import io.jmix.dependencies.task.ResolveNpmDependenciesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class JmixNpmDependenciesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("resolveNpmDependencies", ResolveNpmDependenciesTask.class);

        Task resolveNpmDependenciesTask = project.getTasks().getByName("resolveNpmDependencies");
        resolveNpmDependenciesTask.finalizedBy("vaadinBuildFrontend");
    }
}
