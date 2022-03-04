/*
 * Copyright 2022 Intershop Communications AG.
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
import com.intershop.gradle.buildinfo.ci.AbstractCIInfoProvider
import com.intershop.gradle.buildinfo.scm.AbstractScmInfoProvider
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import groovy.util.Node
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.tasks.GenerateIvyDescriptor
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.model.Defaults
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.api.tasks.bundling.Jar
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.inject.Inject

@Slf4j
class BuildInfoProjectPlugin implements Plugin<Project> {

    private BuildInfoExtension rootExtension

    void apply(Project project) {
        rootExtension = project.getRootProject().extensions.findByType(BuildInfoExtension.class)

        if (rootExtension != null) {
            project.logger.info("Initialize build info for project {}", project.name)

            project.afterEvaluate {
                project.tasks.withType(Jar.class, new ManifestActionJar())
                project.tasks.withType(GenerateIvyDescriptor.class, new IvyDescriptorAction())
                project.tasks.withType(GenerateMavenPom.class, new PomDescriptorAction())
            }
        } else {
            project.logger.info("Rootproject plugin is missing!")
        }
    }

    private class IvyDescriptorAction implements Action<GenerateIvyDescriptor> {
        @Override
        void execute(GenerateIvyDescriptor provider) {
            InfoProvider infoProvider = rootExtension.infoProvider
            AbstractScmInfoProvider scmProvider = rootExtension.scmProvider
            AbstractCIInfoProvider ciProvider = rootExtension.ciProvider

            if (! rootExtension.noDescriptorInfo) {
                provider.descriptor.withXml {
                    Node node = it.asNode()
                    node.@'xmlns:e' = 'http://ant.apache.org/ivy/extra'
                    checkNode(node.info[0], 'e:created-by', "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                    checkNode(node.info[0], 'e:build-java-version', infoProvider.javaVersion)
                    checkNode(node.info[0], 'e:java-source-compatibility', infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                    checkNode(node.info[0], 'e:java-target-compatibility', infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
                    checkNode(node.info[0], 'e:implementation-version', infoProvider.projectVersion)
                    checkNode(node.info[0], 'e:build-status', infoProvider.projectStatus)
                    checkNode(node.info[0], 'e:built-by', infoProvider.OSUser)
                    checkNode(node.info[0], 'e:built-os', infoProvider.OSName)
                    checkNode(node.info[0], 'e:build-date', infoProvider.OSTime)
                    checkNode(node.info[0], 'e:gradle-version', infoProvider.gradleVersion)
                    checkNode(node.info[0], 'e:gradle-rootproject', infoProvider.rootProject)
                    checkNode(node.info[0], 'e:module-origin', scmProvider.SCMOrigin)
                    checkNode(node.info[0], 'e:scm-change-info', scmProvider.SCMRevInfo)
                    checkNode(node.info[0], 'e:scm-change-time', scmProvider.lastChangeTime)
                    checkNode(node.info[0], 'e:scm-branch-name', scmProvider.branchName)
                    checkNode(node.info[0], 'e:scm-type', scmProvider.SCMType)
                    checkNode(node.info[0], 'e:ci-build-host', ciProvider.buildHost)
                    checkNode(node.info[0], 'e:ci-build-url', ciProvider.buildUrl)
                    checkNode(node.info[0], 'e:ci-build-number', ciProvider.buildNumber)
                    checkNode(node.info[0], 'e:ci-build-job', ciProvider.buildJob)
                    checkNode(node.info[0], 'e:ci-build-time', ciProvider.buildTime)
                }
            }
        }
    }

    private class PomDescriptorAction implements Action<GenerateMavenPom> {
        @Override
        void execute(GenerateMavenPom provider) {
            InfoProvider infoProvider = rootExtension.infoProvider
            AbstractScmInfoProvider scmProvider = rootExtension.scmProvider
            AbstractCIInfoProvider ciProvider = rootExtension.ciProvider

            if (! rootExtension.noDescriptorInfo) {
                provider.pom.withXml {
                    Node propsNode = it.asNode().appendNode('properties')

                    propsNode.appendNode('created-by', "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                    propsNode.appendNode("build-java-version", infoProvider.javaVersion)
                    propsNode.appendNode('java-source-compatibility', infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                    propsNode.appendNode('java-target-compatibility', infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
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

    private void checkNode(Node node, String name, String value) {
        NodeList nl = node.getByName(name)
        nl.each {Node n ->
            node.remove(n)
        }
        node.appendNode(name, value)
    }

    private class ManifestActionJar implements Action<Jar> {
        @Override
        void execute(Jar jar) {
            InfoProvider infoProvider = rootExtension.infoProvider
            AbstractScmInfoProvider scmProvider = rootExtension.scmProvider
            AbstractCIInfoProvider ciProvider = rootExtension.ciProvider

            log.info('Buildinfo for manifest will be extended!')
            HashMap attributes = [
                    'Created-By'           : "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})",
                    'Build-Java-Version'   : infoProvider.javaVersion,
                    'X-Compile-Source-JDK' : infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0],
                    'X-Compile-Target-JDK' : infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0],
                    'Manifest-Version'     : '1.0',
                    'Implementation-Vendor': (rootExtension.getModuleVendor() ?: 'unknown')
            ]

            if (rootExtension.additionalJarInfo) {
                log.info('Buildinfo for manifest will be extended with additional parameters.')
                attributes.putAll([
                        'Implementation-Version': "${infoProvider.projectVersion ?: 'unknown'}".toString(),

                        'Build-Status'          : "${infoProvider.projectStatus ?: 'unknown'}",
                        'Built-By'              : "${infoProvider.OSUser ?: 'unknown'}",
                        'Built-OS'              : "${infoProvider.OSName ?: 'unknown'}",
                        'Build-Date'            : "${infoProvider.OSTime ?: 'unknown'}",
                        'Gradle-Version'        : "${infoProvider.gradleVersion ?: 'unknown'}",
                        'Gradle-RootProject'    : "${infoProvider.rootProject ?: 'unknown'}",

                        'Module-Origin'         : "${scmProvider.SCMOrigin ?: 'unknown'}",
                        'SCM-change-info'       : "${scmProvider.SCMRevInfo ?: 'unknown'}",
                        'SCM-change-time'       : "${scmProvider.lastChangeTime ?: 'unknown'}",
                        'SCM-branch-name'       : "${scmProvider.branchName ?: 'unknown'}",
                        'SCM-type'              : "${scmProvider.SCMType ?: 'unknown'}",

                        'CI-build-host'         : "${ciProvider.buildHost ?: 'unknown'}",
                        'CI-build-url'          : "${ciProvider.buildUrl ?: 'unknown'}",
                        'CI-build-number'       : "${ciProvider.buildNumber ?: 'unknown'}",
                        'CI-build-job'          : "${ciProvider.buildJob ?: 'unknown'}",
                        'CI-build-time'         : "${ciProvider.buildTime ?: 'unknown'}"
                ])
            }
            jar.inputs.properties(attributes)

            jar.doFirst {
                log.info("Add buildinfo to manifest")
                jar.manifest.attributes.putAll(attributes)
            }
        }
    }
}