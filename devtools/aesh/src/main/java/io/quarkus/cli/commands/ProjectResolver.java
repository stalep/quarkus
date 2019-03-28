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

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class ProjectResolver {

    private File projectPath;
    private File projectFile;
    private ProjectType projectType;

    public ProjectResolver(File projectPath) {
        this.projectPath = projectPath;
        resolveProject();
    }

    public File projectFile() {
        return projectFile;
    }

    public ProjectType projectType() {
        return projectType;
    }

    //for now just search for the default folders, in the future we would need to read the project files
    public File resolveBuildDir() {
        File buildDir;
        if (projectType == ProjectType.MAVEN)
            buildDir = new File(projectPath.getAbsolutePath() + File.separatorChar + "target");
        else
            buildDir = new File(projectPath.getAbsolutePath() + File.separatorChar + "build");

        if (!buildDir.isDirectory())
            throw new IllegalArgumentException(
                    "Build dir not found, make sure that the project have been built at least once");

        return buildDir;
    }

    private void resolveProject() {
        if (!projectPath.isDirectory())
            throw new IllegalArgumentException("Project path need to be a directory!");

        for (String file : projectPath.list()) {
            if (file.equals("pom.xml")) {
                projectFile = new File(projectPath + File.separator + file);
                projectType = ProjectType.MAVEN;
                return;
            } else if (file.equals("build.gradle")) {
                projectFile = new File(projectPath + File.separator + file);
                projectType = ProjectType.GRADLE;
                return;
            }
        }

        throw new IllegalArgumentException("The project directory do not contain a pom.xml nor a build.gradle project file");
    }

    //for now just search for the default folders, in the future we would need to read the project files
    public File resolveSourceDir() {
        File sourceDir = new File(projectPath.getAbsolutePath() + File.separatorChar + "src"
                + File.separatorChar + "main" + File.separatorChar + "java");

        if (!sourceDir.isDirectory())
            throw new IllegalArgumentException(
                    "Source dir not found, if the source directory is something different than src/main/java, " +
                            "please use the source option");
        return sourceDir;
    }

}
