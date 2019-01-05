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
package com.intershop.gradle.buildinfo.basic

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
/**
 * This info provider provides the basic information of the project.
 */
@CompileStatic
class InfoProvider {

    /*
     * Project of the current working copy
     */
    private final Project project

    /**
     * Constructs the basic info provider.
     *
     * @param project project of the working copy
     */
    InfoProvider(Project project) {
        this.project = project
    }

    /**
     * Returns the project java.runtime.version (read only)
     * @return java.runtime.version
     */
    String getJavaRuntimeVersion() {
        return System.getProperty('java.runtime.version')
    }

    /**
     * Returns the project java.version (read only)
     * @return java.version
     */
    String getJavaVersion() {
        return System.getProperty('java.version')
    }

    /**
     * Returns the project java VM vendor (read only)
     * @return java.vm.specification.vendor
     */
    String getJavaVendor() {
        System.getProperty('java.vm.specification.vendor')
    }

    /**
     * Returns the project target compatibility (read only)
     * @return java target compatibility
     */
    String getJavaTargetCompatibility() {
        JavaPluginConvention javaConvention = project.convention.findPlugin(JavaPluginConvention)
        return javaConvention ? javaConvention.targetCompatibility : ''
    }

    /**
     * Returns the project source compatibility (read only)
     * @return java source compatibility
     */
    String getJavaSourceCompatibility() {
        JavaPluginConvention javaConvention = project.convention.findPlugin(JavaPluginConvention)
        return javaConvention ? javaConvention.sourceCompatibility : ''
    }

    /**
     * Returns the path of the used JAVA_HOME
     * @return path to java home
     */
    String getJavaHome() {
        return System.getenv('JAVA_HOME')
    }

    /**
     * Returns the project module.
     * @return project module name
     */
    String getRootProject() {
        return project.rootProject.name
    }

    /**
     * Returns the project version.
     * @return version of the project
     */
    String getProjectVersion() {
        return project.getVersion()
    }

    /**
     * Returns the project status
     * @return status of the project
     */
    String getProjectStatus() {
        return project.status
    }

    /**
     * Returns the username of the build user
     * @return username
     */
    String getOSUser() {
        return System.getProperty('user.name')
    }

    /**
     * Returns the OS name.
     * @return OS name
     */
    String getOSName() {
        System.getProperty('os.name')
    }

    /**
     * The build time on the build machine.
     * @return time string
     */
    String getOSTime() {
       new Date().format('yyyy-MM-dd_HH:mm:ss')
    }

    /**
     * The used Gradle version.
     * @return version of Gradle
     */
    String getGradleVersion() {
        project.gradle.gradleVersion
    }
}
