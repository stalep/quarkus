package io.quarkus.cli.commands.file;

import io.quarkus.cli.commands.writer.FileProjectWriter;

import java.io.File;

public class BuildFileUtil {

    public static BuildFile findExistingBuildFile(File path) {
        if (path != null) {
            File projectDirectory = new File(path.getAbsolutePath());
            FileProjectWriter writer = new FileProjectWriter(projectDirectory);
            if (new File(projectDirectory, "build.gradle").exists()
                    || new File(projectDirectory, "build.gradle.kts").exists()) {
                return new GradleBuildFile(writer);
            }
            else if(new File(projectDirectory, "pom.xml").exists()){
                return new MavenBuildFile(writer);
            }
        }

        return null;
    }
}
