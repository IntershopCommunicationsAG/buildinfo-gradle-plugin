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
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.tasks.GenerateIvyDescriptor
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.bundling.Jar
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.inject.Inject

/**
 * <p>This plugin will apply the buildinfo plugin.</p>
 *
 * <p>It adds information from the build, the OS, the SCM and CI server to the jar, the pom or ivy file.</p>
 * <p>Git and Subversion are supported SCM types.</p>
 */
@Slf4j
@CompileStatic
class BuildInfoPlugin implements Plugin<Project> {

    /**
     * Name of the extension
     */
    final static String EXTENSION_NAME = 'buildinfo'

    private BuildInfoExtension extension
    private final ModelRegistry modelRegistry

    @Inject
    BuildInfoPlugin(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry
    }

    void apply(Project project) {
        project.logger.info("Applying ${EXTENSION_NAME} plugin to project: ${project.name}")

        this.extension = project.extensions.findByType(BuildInfoExtension) ?: project.extensions.create(EXTENSION_NAME, BuildInfoExtension, project)

        initializeProvider(project, extension.runOnCI)


        if(modelRegistry != null && modelRegistry.state(ModelPath.nonNullValidatedPath('buildInfoData')) == null) {
            modelRegistry.register(ModelRegistrations.bridgedInstance(
                    ModelReference.of('buildInfoData', BuildInfoExtension.class), extension)
                    .descriptor( 'Build info data').build())
        }
    }

    @CompileStatic
    static class BuildInfoRule extends RuleSource {
        @Defaults
        void configureComponentBuildPublishing(ModelMap<Task> tasks,
                                               PublishingExtension publishing,
                                               BuildInfoExtension extension) {

            tasks.withType(Jar.class, new Action<Jar>() {

                @Override
                void execute(Jar jar) {
                    InfoProvider infoProvider = extension.infoProvider
                    AbstractScmInfoProvider scmProvider = extension.scmProvider
                    AbstractCIInfoProvider ciProvider = extension.ciProvider

                    def attributes = [
                            'Created-By'           : "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})",
                            'Build-Java-Version'   : infoProvider.javaVersion,
                            'X-Compile-Source-JDK' : infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0],
                            'X-Compile-Target-JDK' : infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0],

                            'Manifest-Version'     : '1.0',
                            'Implementation-Title' : (infoProvider.projectModule ?: 'unknown'),
                            'Implementation-Vendor': (extension.getModuleVendor() ?: 'unknown')
                    ]

                    if (extension.runOnCI && extension.additionalJarInfo) {
                        log.info('Buildinfo for manifest will be extended!')
                        attributes.putAll([
                                'Implementation-Version': "${infoProvider.projectVersion ?: 'unknown'}",

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
            })

            if (extension.runOnCI && !extension.noDescriptorInfo) {
                try {
                    MavenPublication mvnPub = publishing.publications.maybeCreate('mvn', MavenPublication.class)
                    mvnPub.pom.withXml(new Action<XmlProvider>() {
                        @Override
                        void execute(XmlProvider xmlProvider) {
                            log.info('Add buildinfo to pom file')

                            InfoProvider infoProvider = extension.infoProvider
                            AbstractScmInfoProvider scmProvider = extension.scmProvider
                            AbstractCIInfoProvider ciProvider = extension.ciProvider

                            Element rootElement =  xmlProvider.asElement()
                            org.w3c.dom.NodeList nl = rootElement.getElementsByTagName('properties')

                            nl.each { org.w3c.dom.Node n -> rootElement.removeChild(n) }

                            org.w3c.dom.Node propsNode = rootElement.appendChild(
                                    rootElement.getOwnerDocument().createElement('properties'))

                            addNode(propsNode, 'created-by',
                                    "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                            addNode(propsNode,"build-java-version",
                                    infoProvider.javaVersion)
                            addNode(propsNode,'java-source-compatibility',
                                    infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                            addNode(propsNode,'java-target-compatibility',
                                    infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
                            addNode(propsNode,'implementation-title',
                                    infoProvider.projectModule)
                            addNode(propsNode,'implementation-version',
                                    infoProvider.projectVersion)
                            addNode(propsNode,'build-status',
                                    infoProvider.projectStatus)
                            addNode(propsNode,'built-by',
                                    infoProvider.OSUser)
                            addNode(propsNode,'built-os',
                                    infoProvider.OSName)
                            addNode(propsNode,'build-date',
                                    infoProvider.OSTime)
                            addNode(propsNode,'gradle-version',
                                    infoProvider.gradleVersion)
                            addNode(propsNode,'gradle-rootproject',
                                    infoProvider.rootProject)

                            addNode(propsNode,'module-origin',
                                    scmProvider.SCMOrigin)
                            addNode(propsNode,'scm-change-info',
                                    scmProvider.SCMRevInfo)
                            addNode(propsNode,'scm-change-time',
                                    scmProvider.lastChangeTime)
                            addNode(propsNode,'scm-branch-name',
                                    scmProvider.branchName)
                            addNode(propsNode,'scm-type',
                                    scmProvider.SCMType)

                            addNode(propsNode,'ci-build-host',
                                    ciProvider.buildHost)
                            addNode(propsNode,'ci-build-url',
                                    ciProvider.buildUrl)
                            addNode(propsNode,'ci-build-number',
                                    ciProvider.buildNumber)
                            addNode(propsNode,'ci-build-job',
                                    ciProvider.buildJob)
                            addNode(propsNode,'ci-build-time',
                                    ciProvider.buildTime)
                        }

                        private void addNode(org.w3c.dom.Node node, String name, String value) {
                            Document document = node.getOwnerDocument()
                            Element nelement = document.createElement(name)
                            nelement.appendChild(document.createTextNode(value))

                            node.appendChild(nelement)
                        }
                    })

                } catch(InvalidUserDataException ex ) {
                    log.debug('Maven publishing was not applied to the project')
                }

                try {
                    IvyPublication ivyPub = publishing.publications.maybeCreate('ivy', IvyPublication.class)
                    ivyPub.descriptor.withXml(new Action<XmlProvider>() {
                        @Override
                        void execute(XmlProvider xmlProvider) {
                            log.info('Add buildinfo to ivy file')

                            InfoProvider infoProvider = extension.infoProvider
                            AbstractScmInfoProvider scmProvider = extension.scmProvider
                            AbstractCIInfoProvider ciProvider = extension.ciProvider

                            xmlProvider.asElement().setAttribute('xmlns:e', 'http://ant.apache.org/ivy/extra')

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:created-by',
                                "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:build-java-version',
                                infoProvider.javaVersion)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:java-source-compatibility',
                                infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:java-target-compatibility',
                                infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:implementation-vendor',
                                (extension.getModuleVendor() ?: 'unknonw'))

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:implementation-title',
                                infoProvider.projectModule)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:implementation-version',
                                infoProvider.projectVersion)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:build-status',
                                infoProvider.projectStatus)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:built-by',
                                infoProvider.OSUser)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:built-os',
                                infoProvider.OSName)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:build-date',
                                infoProvider.OSTime)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:gradle-version',
                                infoProvider.gradleVersion)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:gradle-rootproject',
                                infoProvider.rootProject)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:module-origin',
                                scmProvider.SCMOrigin)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:scm-change-info',
                                scmProvider.SCMRevInfo)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:scm-change-time',
                                scmProvider.lastChangeTime)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:scm-branch-name',
                                scmProvider.branchName)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:scm-type',
                                scmProvider.SCMType)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:ci-build-host',
                                ciProvider.buildHost)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:ci-build-url',
                                ciProvider.buildUrl)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:ci-build-number',
                                ciProvider.buildNumber)

                        checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                'e:ci-build-job',
                                ciProvider.buildJob)

                            checkNode(xmlProvider.asElement().getElementsByTagName('info')?.item(0),
                                    'e:ci-build-time',
                                    ciProvider.buildTime)
                        }

                    private void checkNode(org.w3c.dom.Node node, String name, String value) {
                        Element infoElement = ((Element) node)
                        org.w3c.dom.NodeList nl = infoElement.getElementsByTagName(name)
                        nl.each { org.w3c.dom.Node n ->
                            infoElement.removeChild(n)
                        }
                        Document document = node.getOwnerDocument()

                            Element nelement = document.createElement(name)
                            nelement.appendChild(document.createTextNode(value))
                            infoElement.appendChild(nelement)
                        }
                    })

                } catch(InvalidUserDataException ex ) {
                    log.debug('Ivy publishing was not applied to the project')
                }
            }
        }
    }


    /**
     * Initialize all build info provider.
     *
     * @param project
     */
    @CompileStatic
    private void initializeProvider(Project project, boolean runOnCI) {
        if(runOnCI) {
            if (System.getenv('bamboo_buildResultsUrl')) {
                extension.ciProvider = new BambooCIInfoProvider(project.projectDir)
            } else if (System.getenv('JENKINS_URL')) {
                extension.ciProvider = new JenkinsCIInfoProvider(project.projectDir)
            } else if (System.getenv('CI_BUILD_ID')) {
                extension.ciProvider = new GitlabCIInfoProvider(project.projectDir)
            } else if (System.getenv('TRAVIS')) {
                extension.ciProvider = new TravisCIInfoProvider(project.projectDir)
            } else {
                extension.ciProvider = new UnknownCIInfoProvider(project.projectDir)
            }

            File gitDir = new File(project.rootDir, '.git')
            File svnDir = new File(project.rootDir, '.svn')

            if (gitDir.directory) {
                extension.scmProvider = new GitScmInfoProvider(project.projectDir)
                if (System.getenv('bamboo_buildResultsUrl')) {
                    ((GitScmInfoProvider) extension.scmProvider).bambooBuild = true
                }
            } else if (svnDir.directory) {
                extension.scmProvider = new SvnScmInfoProvider(project.projectDir)
            } else {
                extension.scmProvider = new UnknownScmInfoProvider(project.projectDir)
            }
        }

        extension.infoProvider = new InfoProvider(project)
    }
}
