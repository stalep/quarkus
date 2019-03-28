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

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@GroupCommandDefinition(name = QuarkusCommand.COMMAND_NAME, groupCommands = { ListExtensionsCommand.class,
        AddExtensionCommand.class,
        CreateProjectCommand.class }, description = "<command> [<args>] \n\nThese are the common quarkus commands used in various situations")
public class QuarkusCommand implements Command<CommandInvocation> {
    public static final String COMMAND_NAME = "quarkus";

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'i', hasValue = false, description = "Starts an interactive Quarkus Shell")
    private boolean interactive;

    public CommandResult execute(CommandInvocation commandInvocation) {
        if (help)
            commandInvocation.println(commandInvocation.getHelpInfo());

        if (interactive)
            startInteractive();

        return CommandResult.SUCCESS;
    }

    private void startInteractive() {
        AeshConsoleRunner.builder()
                .command(CreateProjectCommand.class)
                .command(AddExtensionCommand.class)
                .command(ListExtensionsCommand.class)
                .prompt("[quarkus]$ ")
                .addExitCommand()
                .start();
    }
}
