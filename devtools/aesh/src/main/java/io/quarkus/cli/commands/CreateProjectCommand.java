/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.io.Resource;
import org.aesh.selector.SelectorType;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "create-project", description = "Creates a base Quarkus project")
public class CreateProjectCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'g', defaultValue = "org.acme")
    private String groupid;

    @Option(shortName = 'a', defaultValue = "quarkus")
    private String artifactid;

    @Option(shortName = 'v', defaultValue = "1.0.0-SNAPSHOT")
    private String version;

    @Option(shortName = 'b', selector = SelectorType.SELECT, description = "Build tool type for the project", completer = ProjectTypeCompleter.class)
    private BuildTool buildTool;

    @OptionList(shortName = 'e', description = "Extensions that will be added to the build file", completer = ExtensionCompleter.class, selector = SelectorType.SELECTIONS)
    private List<String> extensions;

    @Argument(description = "The folder where the new project will be located", required = true)
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus create-project"));
            return CommandResult.SUCCESS;
        }

        if (path != null) {
            try {
                FileProjectWriter projectWriter = new FileProjectWriter(new File(path.getAbsolutePath()));
                boolean status = new CreateProject(projectWriter)
                        .groupId(groupid)
                        .artifactId(artifactid)
                        .version(this.version)
                        .buildTool(buildTool)
                        .doCreateProject(new HashMap<>());

                if (extensions != null && extensions.size() > 0)
                    new AddExtensions(projectWriter)
                            .addExtensions(new HashSet<>(extensions));
                else
                    new AddExtensions(projectWriter)
                            .addExtensions(new HashSet<>(asList("resteasy")));

                if (status) {
                    commandInvocation
                            .println("Project " + artifactid + " created successfully at " + path.getAbsolutePath() + ".");
                } else {
                    commandInvocation.println("Failed to create project");
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            commandInvocation.println("You need to set the path for the project");
        }

        return CommandResult.SUCCESS;
    }
}
