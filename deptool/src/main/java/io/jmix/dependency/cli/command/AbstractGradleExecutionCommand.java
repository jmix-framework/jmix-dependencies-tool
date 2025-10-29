package io.jmix.dependency.cli.command;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public abstract class AbstractGradleExecutionCommand implements BaseCommand {

    protected void addGradleTaskOption(List<String> taskArguments, String optionName, String value) {
        if (!optionName.startsWith("--")) {
            optionName = "--" + optionName;
        }

        taskArguments.add(optionName);
        if (StringUtils.isNotBlank(value)) {
            taskArguments.add(value);
        }
    }

    protected void addProjectParameter(List<String> taskArguments, String parameterName, String value) {
        if (!parameterName.startsWith("-P")) {
            parameterName = "-P" + parameterName;
        }

        if (StringUtils.isNotBlank(value)) {
            taskArguments.add(parameterName + "=" + value);
        }
    }
}
