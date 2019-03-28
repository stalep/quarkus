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

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "list-extensions", description = "List extensions for a project")
public class ListExtensionsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'n', hasValue = false, description = "Only display the extension names")
    private boolean name;

    @Option(shortName = 'a', hasValue = false, description = "Display name, group-id, artifact-id and version (default behaviour)")
    private boolean all;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus list-extensions"));
        } else if (name) {
            for (Extension ext : MojoUtils.loadExtensions()) {
                commandInvocation.println(ext.getName());
            }

        } else {
            for (Extension ext : MojoUtils.loadExtensions()) {
                commandInvocation.println(
                        ext.getName() + " (" + ext.getGroupId() + ":" + ext.getArtifactId() + ":" + ext.getVersion() + ")");
            }
        }
        return CommandResult.SUCCESS;
    }

}
