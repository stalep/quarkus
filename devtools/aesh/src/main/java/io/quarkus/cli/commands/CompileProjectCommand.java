package io.quarkus.cli.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.BuildFileUtil;
import io.quarkus.generators.BuildTool;

@CommandDefinition(name = "compile-project", description = "Compiles the targeted project")
public class CompileProjectCommand implements Command<CommandInvocation> {
    @Option(name = "clean", hasValue = false, shortName = 'c', description = "Clean the project before compiling")
    private boolean clean;

    @Argument(description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {

        File projectDirectory = path != null ? new File(path.getAbsolutePath()) : new File(System.getProperty("user.dir"));

        BuildFile buildFile = BuildFileUtil.findExistingBuildFile(projectDirectory);
        //lets do maven
        if (buildFile.getBuildTool().equals(BuildTool.MAVEN)) {
            if (buildFile.hasWrapper()) {
                executeWrapper(invocation, buildFile.getWrapper(), "install");
            } else {
                executeMaven(projectDirectory, invocation);
            }

        }
        //do gradle
        else {
            if (buildFile.hasWrapper()) {
                executeWrapper(invocation, buildFile.getWrapper(), "build");
            } else {
                executeGradle(projectDirectory, invocation);
            }
        }

        return CommandResult.SUCCESS;
    }

    private void executeGradle(File projectDirectory, CommandInvocation invocation) {
        String gradleExecutable = findExecutable("gradle");
        if (gradleExecutable == null) {
            invocation.println("unable to find the gradle executable, is it in your path?");
        } else {
            gradleExecutable += File.separator + "bin" + File.separator + "gradle";

            try {
                Process process = new ProcessBuilder()
                        .command(gradleExecutable, "build")
                        .directory(projectDirectory)
                        .start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    invocation.println(line);
                }

                int exit = process.waitFor();
                if (exit != 0)
                    invocation.println("Build failed.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String findExecutable(String exec) {
        Optional<Path> mvnPath = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                .map(Paths::get)
                .filter(path -> Files.exists(path.resolve(exec))).findFirst();

        return mvnPath.map(value -> value.getParent().toString()).orElse(null);
    }

    private void executeWrapper(CommandInvocation invocation, File wrapper, String target) {

        try {
            Process process = new ProcessBuilder()
                    .command("./" + wrapper.getName(), target)
                    .directory(wrapper.getParentFile())
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                invocation.println(line);
            }

            int exit = process.waitFor();
            if (exit != 0)
                invocation.println("Build failed.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void executeMaven(File projectDirectory, CommandInvocation invocation) {
        String mvnPath = findExecutable("mvn");
        System.setProperty("maven.home", mvnPath);

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectDirectory.getAbsolutePath() + File.separatorChar + "pom.xml"));
        request.setGoals(Collections.singletonList("install"));

        Invoker invoker = new DefaultInvoker();

        InvocationResult result = null;
        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }

        if (result.getExitCode() != 0) {
            invocation.println("Build failed.");
        }
    }
}
