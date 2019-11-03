package com.intershop.gradle.buildinfo

import com.intershop.gradle.buildinfo.basic.InfoProvider
import com.intershop.gradle.buildinfo.ci.AbstractCIInfoProvider
import com.intershop.gradle.buildinfo.scm.AbstractScmInfoProvider
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
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
@CompileStatic
class BuildInfoProjectPlugin implements Plugin<Project> {

    private final ModelRegistry modelRegistry
    private BuildInfoExtension rootExtension

    @Inject
    BuildInfoProjectPlugin(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry
    }

    void apply(Project project) {
        rootExtension = project.getRootProject().extensions.findByType(BuildInfoExtension.class)

        if (rootExtension != null) {
            project.logger.info("Initialize build info for project {}", project.name)

            project.afterEvaluate {
                project.tasks.withType(Jar.class, new ManifestActionJar())
            }

            if(modelRegistry != null && modelRegistry.state(ModelPath.nonNullValidatedPath('buildInfoData')) == null) {
                modelRegistry.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of('buildInfoData', BuildInfoExtension.class), rootExtension)
                        .descriptor( 'Build info data').build())
            }
        } else {
            project.logger.info("Rootproject plugin is missing!")
        }
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

    @CompileStatic
    static class BuildInfoRule extends RuleSource {
        @Defaults
        void configureComponentBuildPublishing(PublishingExtension publishing,
                                               BuildInfoExtension extension) {

            if (!extension.noDescriptorInfo) {
                try {
                    MavenPublication mvnPub = publishing.publications.maybeCreate('mvn', MavenPublication.class)
                    if(mvnPub != null && mvnPub.pom != null) {
                        mvnPub.pom.withXml(new Action<XmlProvider>() {
                            @Override
                            void execute(XmlProvider xmlProvider) {
                                log.info('Add buildinfo to pom file')

                                InfoProvider infoProvider = extension.infoProvider
                                AbstractScmInfoProvider scmProvider = extension.scmProvider
                                AbstractCIInfoProvider ciProvider = extension.ciProvider

                                Element rootElement = xmlProvider.asElement()
                                org.w3c.dom.NodeList nl = rootElement.getElementsByTagName('properties')

                                nl.each { org.w3c.dom.Node n -> rootElement.removeChild(n) }

                                org.w3c.dom.Node propsNode = rootElement.appendChild(
                                        rootElement.getOwnerDocument().createElement('properties'))

                                addNode(propsNode, 'created-by',
                                        "${infoProvider.javaRuntimeVersion} (${infoProvider.javaVendor})")
                                addNode(propsNode, "build-java-version",
                                        infoProvider.javaVersion)
                                addNode(propsNode, 'java-source-compatibility',
                                        infoProvider.javaSourceCompatibility ?: infoProvider.javaVersion.split('_')[0])
                                addNode(propsNode, 'java-target-compatibility',
                                        infoProvider.javaTargetCompatibility ?: infoProvider.javaVersion.split('_')[0])
                                addNode(propsNode, 'implementation-version',
                                        infoProvider.projectVersion.toString())
                                addNode(propsNode, 'build-status',
                                        infoProvider.projectStatus)
                                addNode(propsNode, 'built-by',
                                        infoProvider.OSUser)
                                addNode(propsNode, 'built-os',
                                        infoProvider.OSName)
                                addNode(propsNode, 'build-date',
                                        infoProvider.OSTime)
                                addNode(propsNode, 'gradle-version',
                                        infoProvider.gradleVersion)
                                addNode(propsNode, 'gradle-rootproject',
                                        infoProvider.rootProject)

                                addNode(propsNode, 'module-origin',
                                        scmProvider.SCMOrigin)
                                addNode(propsNode, 'scm-change-info',
                                        scmProvider.SCMRevInfo)
                                addNode(propsNode, 'scm-change-time',
                                        scmProvider.lastChangeTime)
                                addNode(propsNode, 'scm-branch-name',
                                        scmProvider.branchName)
                                addNode(propsNode, 'scm-type',
                                        scmProvider.SCMType)

                                addNode(propsNode, 'ci-build-host',
                                        ciProvider.buildHost)
                                addNode(propsNode, 'ci-build-url',
                                        ciProvider.buildUrl)
                                addNode(propsNode, 'ci-build-number',
                                        ciProvider.buildNumber)
                                addNode(propsNode, 'ci-build-job',
                                        ciProvider.buildJob)
                                addNode(propsNode, 'ci-build-time',
                                        ciProvider.buildTime)
                            }

                            private void addNode(org.w3c.dom.Node node, String name, String value) {
                                Document document = node.getOwnerDocument()
                                Element nelement = document.createElement(name)
                                nelement.appendChild(document.createTextNode(value))

                                node.appendChild(nelement)
                            }
                        })
                    } else {
                        log.debug('Mvn publishing plugin is applied, but not configured.')
                    }
                } catch (InvalidUserDataException ex) {
                    log.debug('Maven publishing was not applied to the project')
                }

            }
        }
    }
}