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
import java.io.IOException;
import java.util.Collections;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "add-extension", description = "Adds extensions to a project")
public class AddExtensionCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false, overrideRequired = true)
    private boolean help;

    @Option(shortName = 'e', required = true, description = "Name of the extension that will be added to the project")
    private String extension;

    @Argument(required = true, description = "Path to the project pom the extension will be added")
    private Resource pom;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus add-extension"));
            return CommandResult.SUCCESS;
        } else {
            if (!findExtension(extension)) {
                commandInvocation.println("Can not find any extension named: " + extension);
                return CommandResult.SUCCESS;
            } else if (pom.isLeaf()) {
                try {
                    AddExtensions project = new AddExtensions(new File(pom.getAbsolutePath()));
                    project.addExtensions(Collections.singleton(extension));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return CommandResult.SUCCESS;
    }

    private boolean findExtension(String name) {
        for (Extension ext : MojoUtils.loadExtensions()) {
            if (ext.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }
}
