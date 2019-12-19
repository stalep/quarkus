package io.quarkus.cli.commands;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.dev.DevModeContext;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DevModeRunner {
    private final List<String> args;
    private final String debug;
    private final String suspend;
    private final List<String> compilerArgs;
    private Process process;
    private Set<Path> buildFiles = new HashSet<>();

    public DevModeRunner(List<String> args, String debug, String suspend, List<String> compilerArgs) {
        this.args = new ArrayList<>(args);
        this.debug = debug;
        this.suspend = suspend;
        this.compilerArgs = compilerArgs;
    }

    /**
     * Attempts to prepare the dev mode runner.
     */
    public void prepare() throws Exception {
        if (debug == null) {
            boolean useDebugMode = true;
            // debug mode not specified
            // make sure 5005 is not used, we don't want to just fail if something else is using it
            try (Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 5005)) {
                getLog().error("Port 5005 in use, not starting in debug mode");
                useDebugMode = false;
            } catch (IOException e) {
            }
            if (useDebugMode) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=" + suspend);
            }
        } else if (debug.toLowerCase().equals("client")) {
            args.add("-Xdebug");
            args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=n,suspend=" + suspend);
        } else if (debug.toLowerCase().equals("true")) {
            args.add("-Xdebug");
            args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=y,suspend=" + suspend);
        } else if (!debug.toLowerCase().equals("false")) {
            try {
                int port = Integer.parseInt(debug);
                if (port <= 0) {
                    throw new MojoFailureException("The specified debug port must be greater than 0");
                }
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=" + port + ",server=y,suspend=" + suspend);
            } catch (NumberFormatException e) {
                throw new MojoFailureException(
                        "Invalid value for debug parameter: " + debug + " must be true|false|client|{port}");
            }
        }
        //build a class-path string for the base platform
        //this stuff does not change
        // Do not include URIs in the manifest, because some JVMs do not like that
        StringBuilder classPathManifest = new StringBuilder();
        final DevModeContext devModeContext = new DevModeContext();
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            devModeContext.getSystemProperties().put(e.getKey().toString(), (String) e.getValue());
        }
        devModeContext.getBuildSystemProperties().putAll((Map) project.getProperties());
        devModeContext.setSourceEncoding(getSourceEncoding());
        devModeContext.setSourceJavaVersion(source);
        devModeContext.setTargetJvmVersion(target);

        // Set compilation flags.  Try the explicitly given configuration first.  Otherwise,
        // refer to the configuration of the Maven Compiler Plugin.
        if (compilerArgs != null) {
            devModeContext.setCompilerOptions(compilerArgs);
        } else {
            for (Plugin plugin : project.getBuildPlugins()) {
                if (!plugin.getKey().equals("org.apache.maven.plugins:maven-compiler-plugin")) {
                    continue;
                }
                Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
                if (compilerPluginConfiguration == null) {
                    continue;
                }
                Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.getChild("compilerArgs");
                if (compilerPluginArgsConfiguration == null) {
                    continue;
                }
                List<String> compilerPluginArgs = new ArrayList<>();
                for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                    compilerPluginArgs.add(argConfiguration.getValue());
                }
                devModeContext.setCompilerOptions(compilerPluginArgs);
                break;
            }
        }

        final AppModel appModel;
        try {
            final LocalProject localProject;
            if (noDeps) {
                localProject = LocalProject.load(outputDirectory.toPath());
                addProject(devModeContext, localProject);
            } else {
                localProject = LocalProject.loadWorkspace(outputDirectory.toPath());
                for (LocalProject project : localProject.getSelfWithLocalDeps()) {
                    if (project.getClassesDir() != null) {
                        //if this project also contains Quarkus extensions we do no want to include these in the discovery
                        //a bit of an edge case, but if you try and include a sample project with your extension you will
                        //run into problems without this
                        if (Files.exists(project.getClassesDir().resolve("META-INF/quarkus-extension.properties")) ||
                                Files.exists(project.getClassesDir().resolve("META-INF/quarkus-build-steps.list"))) {
                            continue;
                        }
                    }
                    addProject(devModeContext, project);
                }
            }
            for (LocalProject i : localProject.getSelfWithLocalDeps()) {
                buildFiles.add(i.getDir().resolve("pom.xml"));
            }

            /*
             * TODO: support multiple resources dirs for config hot deployment
             * String resources = null;
             * for (Resource i : project.getBuild().getResources()) {
             * resources = i.getDirectory();
             * break;
             * }
             */

            appModel = new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setWorkspace(localProject.getWorkspace())
                    .build())
                    .setDevMode(true)
                    .resolveModel(localProject.getAppArtifact());
            if (appModel.getAllDependencies().isEmpty()) {
                throw new RuntimeException("Unable to resolve application dependencies");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve Quarkus application model", e);
        }
        for (AppDependency appDep : appModel.getAllDependencies()) {
            addToClassPaths(classPathManifest, devModeContext, appDep.getArtifact().getPath().toFile());
        }

        args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        //wiring devmode is used for CDI beans that are not part of the user application (i.e. beans in 3rd party jars)
        //we need this because these beans cannot be loaded by the runtime class loader, they must be loaded by the platform
        //class loader
        File wiringClassesDirectory = new File(buildDir, "wiring-devmode");
        wiringClassesDirectory.mkdirs();

        addToClassPaths(classPathManifest, devModeContext, wiringClassesDirectory);

        //we also want to add the maven plugin jar to the class path
        //this allows us to just directly use classes, without messing around copying them
        //to the runner jar
        URL classFile = DevModeMain.class.getClassLoader()
                .getResource(DevModeMain.class.getName().replace('.', '/') + ".class");
        File path;
        if (classFile == null) {
            throw new MojoFailureException("No DevModeMain class found");
        }
        URI classUri = classFile.toURI();
        if (classUri.getScheme().equals("jar")) {
            String jarPath = classUri.getRawSchemeSpecificPart();
            final int marker = jarPath.indexOf('!');
            if (marker != -1) {
                jarPath = jarPath.substring(0, marker);
            }
            URI jarUri = new URI(jarPath);
            path = Paths.get(jarUri).toFile();
        } else if (classUri.getScheme().equals("file")) {
            path = Paths.get(classUri).toFile();
        } else {
            throw new MojoFailureException("Unsupported DevModeMain artifact URL:" + classFile);
        }
        addToClassPaths(classPathManifest, devModeContext, path);

        //now we need to build a temporary jar to actually run

        File tempFile = new File(buildDir, project.getArtifactId() + "-dev.jar");
        tempFile.delete();
        // Only delete the -dev.jar on exit if requested
        if (deleteDevJar) {
            tempFile.deleteOnExit();
        }
        getLog().debug("Executable jar: " + tempFile.getAbsolutePath());

        devModeContext.getClassesRoots().add(outputDirectory.getAbsoluteFile());
        devModeContext.setFrameworkClassesDir(wiringClassesDirectory.getAbsoluteFile());
        devModeContext.setCacheDir(new File(buildDir, "transformer-cache").getAbsoluteFile());

        // this is the jar file we will use to launch the dev mode main class
        devModeContext.setDevModeRunnerJarFile(tempFile);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
            out.putNextEntry(new ZipEntry("META-INF/"));
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPathManifest.toString());
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, DevModeMain.class.getName());
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            manifest.write(out);

            out.putNextEntry(new ZipEntry(DevModeMain.DEV_MODE_CONTEXT));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream obj = new ObjectOutputStream(new DataOutputStream(bytes));
            obj.writeObject(devModeContext);
            obj.close();
            out.write(bytes.toByteArray());
        }

        outputDirectory.mkdirs();

        args.add("-jar");
        args.add(tempFile.getAbsolutePath());

    }

    public Set<Path> getBuildFiles() {
        return buildFiles;
    }

    public void run() throws Exception {
        // Display the launch command line in dev mode
        getLog().info("Launching JVM with command line: " + args.toString());
        ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[0]));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.directory(workingDir);
        process = pb.start();

        //https://github.com/quarkusio/quarkus/issues/232
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                process.destroy();
            }
        }, "Development Mode Shutdown Hook"));
    }

    public void stop() throws InterruptedException {
        process.destroy();
        process.waitFor();
    }

    private void addToClassPaths(StringBuilder classPathManifest, DevModeContext classPath, File file) {
        URI uri = file.toPath().toAbsolutePath().toUri();
        try {
            classPath.getClassPath().add(uri.toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String path = uri.getRawPath();
        if (PropertyUtils.isWindows()) {
            if (path.length() > 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                path = "/" + path;
            }
        }
        classPathManifest.append(path);
        if (file.isDirectory() && path.charAt(path.length() - 1) != '/') {
            classPathManifest.append("/");
        }
        classPathManifest.append(" ");
    }
}
