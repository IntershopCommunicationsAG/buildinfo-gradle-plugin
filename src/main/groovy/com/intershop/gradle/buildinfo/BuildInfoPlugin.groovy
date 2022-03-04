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
import com.intershop.gradle.buildinfo.scm.AzureGitScmInfoProvider
import com.intershop.gradle.buildinfo.scm.GitScmInfoProvider
import com.intershop.gradle.buildinfo.scm.UnknownScmInfoProvider
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

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

    void apply(Project project) {
        project.logger.info("Applying ${EXTENSION_NAME} plugin to project: ${project.name}")
        this.extension = project.extensions.findByType(BuildInfoExtension) ?: project.extensions.create(EXTENSION_NAME, BuildInfoExtension, project)

        initializeProvider(project)

        project.rootProject.plugins.apply(BuildInfoProjectPlugin.class)
        project.rootProject.subprojects(new Action<Project>() {
            @Override
            void execute(Project p) {
                p.plugins.apply(BuildInfoProjectPlugin.class)
            }
        })
    }

    /**
     * Initialize all build info provider.
     *
     * @param project
     */
    private void initializeProvider(Project project) {
        if (System.getenv('AZURE_HTTP_USER_AGENT')) {
            extension.ciProvider = new AzureCIInfoProvider(project.projectDir)
        } else if (System.getenv('bamboo_buildResultsUrl')) {
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
        if (gitDir.directory) {
            if(System.getenv('AZURE_HTTP_USER_AGENT')) {
                extension.scmProvider = new AzureGitScmInfoProvider(project.projectDir)
            } else {
                extension.scmProvider = new GitScmInfoProvider(project.projectDir)
                if (System.getenv('bamboo_buildResultsUrl')) {
                    ((GitScmInfoProvider) extension.scmProvider).bambooBuild = true
                }
            }
        } else {
            extension.scmProvider = new UnknownScmInfoProvider(project.projectDir)
        }

        extension.infoProvider = new InfoProvider(project)
        System.out.println("Info provider initialized .....")
    }
}
