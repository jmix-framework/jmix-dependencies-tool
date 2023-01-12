package io.jmix.dependency.cli;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.jmix.dependency.cli.command.*;
import io.jmix.dependency.cli.dependency.JmixDependencies;
import io.jmix.dependency.cli.gradle.JmixGradleClient;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    public static void main(String[] args) {
        Map<String, BaseCommand> commands = new HashMap<>();
        commands.put("resolve-jmix", new ResolveJmixCommand());
        commands.put("resolve-lib", new ResolveLibCommand());
        commands.put("export", new ExportCommand());
        commands.put("upload", new UploadCommand());
        JCommander.Builder commanderBuilder = JCommander.newBuilder();
        for (Map.Entry<String, BaseCommand> entry : commands.entrySet()) {
            commanderBuilder.addCommand(entry.getKey(), entry.getValue());
        }
        JCommander commander = commanderBuilder
//                .defaultProvider(new JmixIDefaultProvider())
                .build();

        try {
            commander.parse(args);
        } catch (ParameterException e) {
            commander.usage();
            return;
        }

        String parsedCommand = commander.getParsedCommand();

        if (parsedCommand == null) {
            commander.usage();
            return;
        }

        BaseCommand command = commands.get(parsedCommand);
        command.run();
    }
}
