package io.quarkus.cli.commands;

import java.io.File;
import java.io.IOException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.aesh.selector.SelectorType;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.BuildFileUtil;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = ListExtensions.NAME, generateHelp = true, description = "List extensions for a project")
public class ListExtensionsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'a', hasValue = false, description = "Display all or just the installable extensions.")
    private boolean all = false;

    @Option(shortName = 'f', selector = SelectorType.SELECT, completer = FormatCompleter.class, converter = FormatConverter.class, description = "Select the output format among: 'name' (display the name only), 'concise' (display name and description) and 'full' (concise format and version related columns).")
    private ExtensionFormat format;

    @Option(shortName = 's', hasValue = true, defaultValue = {
            "*" }, description = "Search filter on extension list. The format is based on Java Pattern.")
    private String searchPattern;

    @Option(shortName = 'p', description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            File projectDirectory = path != null ? new File(path.getAbsolutePath()) : new File(System.getProperty("user.dir"));

            BuildFile buildFile = BuildFileUtil.findExistingBuildFile(projectDirectory);

            new ListExtensions(buildFile).listExtensions(all, format, searchPattern);
        } catch (IOException e) {
            throw new CommandException("Unable to list extensions", e);
        }
        return CommandResult.SUCCESS;
    }

}
