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
import org.aesh.terminal.utils.Config;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@GroupCommandDefinition(name = QuarkusBaseCommand.COMMAND_NAME, generateHelp = true, groupCommands = {
        ListExtensionsCommand.class,
        AddExtensionCommand.class,
        CreateProjectCommand.class }, description = "<command> [<args>] \n\nThese are the common quarkus commands used in various situations")
public class QuarkusBaseCommand implements Command<CommandInvocation> {
    public static final String COMMAND_NAME = "quarkus";

    @Option(shortName = 'i', hasValue = false, description = "Starts an interactive Quarkus Shell")
    private boolean interactive;

    public CommandResult execute(CommandInvocation commandInvocation) {
        if (interactive) {
            commandInvocation.println("Starting interactive CLI....");
            //we need to stop first since the QuarkusCli starts the runner with interactive = true
            commandInvocation.stop();
            startInteractive();
        }

        return CommandResult.SUCCESS;
    }

    private void startInteractive() {
        //we start the CLI in a new thread since the caller has a TerminalConnection open
        Runnable runnable = () -> {
            AeshConsoleRunner.builder()
                    .command(CreateProjectCommand.class)
                    .command(AddExtensionCommand.class)
                    .command(ListExtensionsCommand.class)
                    .command(DevModeCommand.class)
                    .prompt("[quarkus@" + Config.getUserDir() + "]$ ")
                    .addExitCommand()
                    .start();
        };
        new Thread(runnable).start();
    }
}
