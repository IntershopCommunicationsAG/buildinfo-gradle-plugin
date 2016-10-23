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
package com.intershop.gradle.buildinfo

import com.intershop.gradle.buildinfo.basic.InfoProvider
import com.intershop.gradle.buildinfo.ci.*
import com.intershop.gradle.buildinfo.scm.GitScmInfoProvider
import com.intershop.gradle.buildinfo.scm.AbstractScmInfoProvider
import com.intershop.gradle.buildinfo.scm.SvnScmInfoProvider
import com.intershop.gradle.buildinfo.scm.UnknownScmInfoProvider
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.ivy.tasks.GenerateIvyDescriptor
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.bundling.Jar

/**
 * <p>This plugin will apply the buildinfo plugin.</p>
 *
 * <p>It adds information from the build, the OS, the SCM and CI server to the jar, the pom or ivy file.</p>
 * <p>Git and Subversion are supported SCM types.</p>
 */
@Slf4j
class BuildInfoPlugin implements Plugin<Project> {

    /**
     * Name of the extension
     */
    final static String EXTENSION_NAME = 'buildinfo'

    private BuildInfoExtension extension
    // private doesn't work inside closures
    @PackageScope InfoProvider infoProvider
    @PackageScope AbstractScmInfoProvider scmProvider
    @PackageScope AbstractCIInfoProvider ciProvider

    void apply(Project project) {
        project.logger.info("Applying ${EXTENSION_NAME} plugin to project: ${project.name}")

        this.extension = project.extensions.findByType(BuildInfoExtension) ?: project.extensions.create(EXTENSION_NAME, BuildInfoExtension, project)

        if(extension.runOnCI) {
            initializeProvider(project)
        }

        project.afterEvaluate {
            if (extension.runOnCI) {
                project.tasks.withType(Jar) { Jar jarTask ->
                    def attributes = [
                        'Created-By'            : "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})",
                        'Build-Java-Version'    : infoProvider.javaVersion,
                        'X-Compile-Source-JDK'  : infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0],
                        'X-Compile-Target-JDK'  : infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0],

                        'Manifest-Version'      : '1.0',
                        'Implementation-Title'  : "${infoProvider.projectModule?:'unknown'}",
                        'Implementation-Version': "${infoProvider.projectVersion?:'unknown'}",

                        'Build-Status'          : "${infoProvider.projectStatus?:'unknown'}",
                        'Built-By'              : "${infoProvider.OSUser?:'unknown'}",
                        'Built-OS'              : "${infoProvider.OSName?:'unknown'}",
                        'Build-Date'            : "${infoProvider.OSTime?:'unknown'}",
                        'Gradle-Version'        : "${infoProvider.gradleVersion?:'unknown'}",
                        'Gradle-RootProject'    : "${infoProvider.rootProject?:'unknown'}",

                        'Module-Origin'         : "${scmProvider.SCMOrigin?:'unknown'}",
                        'SCM-change-info'       : "${scmProvider.SCMRevInfo?:'unknown'}",
                        'SCM-change-time'       : "${scmProvider.lastChangeTime?:'unknown'}",
                        'SCM-branch-name'       : "${scmProvider.branchName?:'unknown'}",
                        'SCM-type'              : "${scmProvider.SCMType?:'unknown'}",

                        'CI-build-host'         : "${ciProvider.buildHost?:'unknown'}",
                        'CI-build-url'          : "${ciProvider.buildUrl?:'unknown'}",
                        'CI-build-number'       : "${ciProvider.buildNumber?:'unknown'}",
                        'CI-build-job'          : "${ciProvider.buildJob?:'unknown'}",
                        'CI-build-time'         : "${ciProvider.buildTime?:'unknown'}"
                    ]

                    jarTask.inputs.properties(attributes)

                    jarTask.doFirst {
                        project.logger.info("Add buildinfo to manifest")
                        jarTask.manifest.attributes.putAll(attributes)
                    }
                }

                project.tasks.withType(GenerateIvyDescriptor) { GenerateIvyDescriptor ivyTask ->
                    project.logger.info("Add buildinfo to ivy file")
                    ivyTask.descriptor.withXml {
                        asNode().@'xmlns:e' = 'http://ant.apache.org/ivy/extra'
                        checkNode(asNode().info[0],'e:created-by', "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                        checkNode(asNode().info[0],'e:build-java-version', infoProvider.javaVersion)
                        checkNode(asNode().info[0],'e:java-source-compatibility', infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                        checkNode(asNode().info[0],'e:java-target-compatibility', infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
                        checkNode(asNode().info[0],'e:implementation-title', infoProvider.projectModule)
                        checkNode(asNode().info[0],'e:implementation-version', infoProvider.projectVersion)
                        checkNode(asNode().info[0],'e:build-status', infoProvider.projectStatus)
                        checkNode(asNode().info[0],'e:built-by', infoProvider.OSUser)
                        checkNode(asNode().info[0],'e:built-os', infoProvider.OSName)
                        checkNode(asNode().info[0],'e:build-date', infoProvider.OSTime)
                        checkNode(asNode().info[0],'e:gradle-version', infoProvider.gradleVersion)
                        checkNode(asNode().info[0],'e:gradle-rootproject', infoProvider.rootProject)
                        checkNode(asNode().info[0],'e:module-origin', scmProvider.SCMOrigin)
                        checkNode(asNode().info[0],'e:scm-change-info', scmProvider.SCMRevInfo)
                        checkNode(asNode().info[0],'e:scm-change-time', scmProvider.lastChangeTime)
                        checkNode(asNode().info[0],'e:scm-branch-name', scmProvider.branchName)
                        checkNode(asNode().info[0],'e:scm-type', scmProvider.SCMType)
                        checkNode(asNode().info[0],'e:ci-build-host', ciProvider.buildHost)
                        checkNode(asNode().info[0],'e:ci-build-url', ciProvider.buildUrl)
                        checkNode(asNode().info[0],'e:ci-build-number', ciProvider.buildNumber)
                        checkNode(asNode().info[0],'e:ci-build-job', ciProvider.buildJob)
                        checkNode(asNode().info[0],'e:ci-build-time', ciProvider.buildTime)
                    }
                }

                project.tasks.withType(GenerateMavenPom) { GenerateMavenPom pomGen ->
                    project.logger.info("Add buildinfo to pom file")
                    pomGen.pom.withXml {
                        NodeList nl = asNode().getByName('properties')
                        nl.each {Node n -> asNode().remove(n) }

                        Node propsNode = asNode().appendNode('properties')
                        propsNode.appendNode('created-by', "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                        propsNode.appendNode("build-java-version", infoProvider.javaVersion)
                        propsNode.appendNode('java-source-compatibility', infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                        propsNode.appendNode('java-target-compatibility', infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
                        propsNode.appendNode('implementation-title', infoProvider.projectModule)
                        propsNode.appendNode('implementation-version', infoProvider.projectVersion)
                        propsNode.appendNode('build-status', infoProvider.projectStatus)
                        propsNode.appendNode('built-by', infoProvider.OSUser)
                        propsNode.appendNode('built-os', infoProvider.OSName)
                        propsNode.appendNode('build-date', infoProvider.OSTime)
                        propsNode.appendNode('gradle-version', infoProvider.gradleVersion)
                        propsNode.appendNode('gradle-rootproject', infoProvider.rootProject)
                        propsNode.appendNode('module-origin', scmProvider.SCMOrigin)
                        propsNode.appendNode('scm-change-info', scmProvider.SCMRevInfo)
                        propsNode.appendNode('scm-change-time', scmProvider.lastChangeTime)
                        propsNode.appendNode('scm-branch-name', scmProvider.branchName)
                        propsNode.appendNode('scm-type', scmProvider.SCMType)
                        propsNode.appendNode('ci-build-host', ciProvider.buildHost)
                        propsNode.appendNode('ci-build-url', ciProvider.buildUrl)
                        propsNode.appendNode('ci-build-number', ciProvider.buildNumber)
                        propsNode.appendNode('ci-build-job', ciProvider.buildJob)
                        propsNode.appendNode('ci-build-time', ciProvider.buildTime)
                    }
                }
            }
        }
    }
    
    private void checkNode(Node node, String name, String value) {
        NodeList nl = node.getByName(name)
        nl.each {Node n ->
            node.remove(n)
        }
        node.appendNode(name, value)
    }

    /**
     * Initialize all build info provider.
     *
     * @param project
     */
    @CompileStatic
    private void initializeProvider(Project project) {
        if (System.getenv('bamboo_buildResultsUrl')) {
            ciProvider = new BambooCIInfoProvider(project.projectDir)
        } else if (System.getenv('JENKINS_URL')) {
            ciProvider = new JenkinsCIInfoProvider(project.projectDir)
        } else if (System.getenv('CI_BUILD_ID')) {
            ciProvider = new GitlabCIInfoProvider(project.projectDir)
        } else if (System.getenv('TRAVIS')) {
            ciProvider = new TravisCIInfoProvider(project.projectDir)
        } else {
            ciProvider = new UnknownCIInfoProvider(project.projectDir)
        }

        File gitDir = new File(project.rootDir, '.git')
        File svnDir = new File(project.rootDir, '.svn')
        if (gitDir.directory) {
            scmProvider = new GitScmInfoProvider(project.projectDir)
            if (System.getenv('bamboo_buildResultsUrl')) {
                ((GitScmInfoProvider)scmProvider).bambooBuild = true
            }
        } else if (svnDir.directory) {
            scmProvider = new SvnScmInfoProvider(project.projectDir)
        } else {
            scmProvider = new UnknownScmInfoProvider(project.projectDir)
        }

        infoProvider = new InfoProvider(project)

        extension.ciProvider = ciProvider
        extension.scmProvider = scmProvider
        extension.infoProvider = infoProvider
    }
}
