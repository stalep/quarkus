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

import java.io.File;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "dev-mode", description = "Starts up a development mode process for a Quarkus project.")
public class DevModeCommand implements Command<CommandInvocation> {

    @Option(shortName = 'd', description = "If this server should be started in debug mode. " +
            "The default is to start in debug mode without suspending and listen on port 5005." +
            " It supports the following options:\n" +
            " \"false\" - The JVM is not started in debug mode\n" +
            " \"true\" - The JVM is started in debug mode and suspends until a debugger is attached to port 5005\n" +
            " \"client\" - The JVM is started in client mode, and attempts to connect to localhost:5005\n" +
            "\"{port}\" - The JVM is started in debug mode and suspends until a debugger is attached to {port}")
    private String debug;

    @Option(shortName = 'b', name = "build", description = "Build folder")
    private File buildDir;

    @Option(shortName = 's', name = "source", description = "Source folder")
    private File sourceDir;

    @Argument(required = true, description = "Path to the project, if the project is located in the current directory, use \'.\'.")
    private File projectPath;

    private ProjectType projectType;
    private File projectFile;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        ProjectResolver projectResolver = new ProjectResolver(projectPath);

        projectType = projectResolver.projectType();
        projectFile = projectResolver.projectFile();
        if (buildDir == null)
            buildDir = projectResolver.resolveBuildDir();
        if (sourceDir == null)
            sourceDir = projectResolver.resolveSourceDir();

        return CommandResult.SUCCESS;
    }

}
