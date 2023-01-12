package io.jmix.dependencies;

import io.jmix.dependencies.task.ResolveDependenciesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JmixDependenciesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("resolveDependencies", ResolveDependenciesTask.class);
    }
}
