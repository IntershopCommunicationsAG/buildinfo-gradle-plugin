/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intershop.gradle.buildinfo.basic;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This info provider provides the basic information of the project.
 */
public class InfoProvider {

    /*
     * Project of the current working copy
     */
    private final Project project;

    /**
     * Constructs the basic info provider.
     *
     * @param project project of the working copy
     */
    public InfoProvider(Project project) {
        this.project = project;
    }

    /**
     * Returns the project java.runtime.version (read only)
     * @return java.runtime.version
     */
    public static String getJavaRuntimeVersion() {
        return System.getProperty("java.runtime.version");
    }

    /**
     * Returns the project java.version (read only)
     * @return java.version
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Returns the project java VM vendor (read only)
     * @return java.vm.specification.vendor
     */
    public static String getJavaVendor() {
        return System.getProperty("java.vm.specification.vendor");
    }

    /**
     * Returns the project target compatibility (read only)
     * @return java target compatibility
     */
    public String getJavaTargetCompatibility() {
        JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        return javaPluginExtension != null ? javaPluginExtension.getTargetCompatibility().toString() : "";
    }

    /**
     * Returns the project source compatibility (read only)
     * @return java source compatibility
     */
    public String getJavaSourceCompatibility() {
        JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        return javaPluginExtension != null ? javaPluginExtension.getSourceCompatibility().toString() : "";
    }

    /**
     * Returns the project module.
     * @return project module name
     */
    public String getRootProject() {
        return project.getRootProject().getName();
    }

    /**
     * Returns the project version.
     * @return version of the project
     */
    public String getProjectVersion() {
        return project.getVersion().toString();
    }

    /**
     * Returns the project status
     * @return status of the project
     */
    public String getProjectStatus() {
        return project.getStatus().toString();
    }

    /**
     * Returns the username of the build user
     * @return username
     */
    public static String getOSUser() {
        return System.getProperty("user.name");
    }

    /**
     * Returns the OS name.
     * @return OS name
     */
    public static String getOSName() {
        return System.getProperty("os.name");
    }

    /**
     * The build time on the build machine.
     * @return time string
     */
    public static String getOSTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
    }

    /**
     * The used Gradle version.
     * @return version of Gradle
     */
    public String getGradleVersion() {
        return project.getGradle().getGradleVersion();
    }
}
