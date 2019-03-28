package io.quarkus.cli.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.io.Resource;
import org.aesh.selector.MultiSelect;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.BuildFileUtil;
import io.quarkus.cli.commands.legacy.LegacyQuarkusCommandInvocation;
import io.quarkus.dependencies.Extension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "add-extensions", description = "Adds extensions to a project")
public class AddExtensionCommand implements Command<CommandInvocation> {

    @OptionList(shortName = 'e', //selector = SelectorType.SELECTIONS,
            completer = ExtensionCompleter.class, description = "Name of the extension that will be added to the project")
    private Set<String> extensions;

    @Option(shortName = 'p', description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {

        if (extensions.isEmpty()) {
            MultiSelect selector = new MultiSelect(invocation.getShell(),
                    getAllExtensions().stream().map(Extension::getSimplifiedArtifactId).collect(Collectors.toList()),
                    "Select the extensions that will be added to your project");

            extensions = new HashSet<>(selector.doSelect());
        }

        final QuarkusCommandInvocation quarkusInvocation = new LegacyQuarkusCommandInvocation();
        try {
            quarkusInvocation.setValue(AddExtensions.EXTENSIONS, extensions);
            File projectDirectory = path != null ? new File(path.getAbsolutePath()) : new File(System.getProperty("user.dir"));

            BuildFile buildFile = BuildFileUtil.findExistingBuildFile(projectDirectory);
            if (buildFile == null) {

            } else {
                AddExtensions project = new AddExtensions(buildFile);
                QuarkusCommandOutcome result = project.execute(quarkusInvocation);
                if (!result.isSuccess()) {
                    throw new CommandException("Unable to add an extension matching " + extensions);
                }
            }
        } catch (QuarkusCommandException e) {
            throw new CommandException("Unable to add an extension matching " + extensions, e);
        }

        return CommandResult.SUCCESS;
    }

    private boolean findExtension(String name, List<Extension> extensions) {
        for (Extension ext : extensions) {
            if (ext.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private List<Extension> getAllExtensions() {
        try {
            return new ListExtensions(null).loadedExtensions(new LegacyQuarkusCommandInvocation());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

}
