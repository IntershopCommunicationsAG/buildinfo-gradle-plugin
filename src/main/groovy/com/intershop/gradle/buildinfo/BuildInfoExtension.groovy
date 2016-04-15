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
import com.intershop.gradle.buildinfo.ci.AbstractCIInfoProvider
import com.intershop.gradle.buildinfo.scm.AbstractScmInfoProvider
import groovy.util.logging.Slf4j
import org.gradle.api.Project

/**
 * <p>This is the extension object for the Intershop buildinfo plugin.</p>
 */
@Slf4j
class BuildInfoExtension {

    // run on CI server
    public final static String RUNONCI_ENV = 'RUNONCI'
    public final static String RUNONCI_PRJ = 'runOnCI'

    final private Project project

    BuildInfoExtension(Project project) {

        this.project = project

        // init default value for runOnCI
        if(! runOnCI) {
            runOnCI = Boolean.parseBoolean(getVariable(RUNONCI_ENV, RUNONCI_PRJ, 'false'))
            if(runOnCI) {
                log.warn('Buildinfo task will be executed on a CI build environment for {}.', project.name)
            }
        }
    }

    /**
     * <p>Configuration for the execution on the CI server</p>
     *
     * <p>Can be configured/overwritten with environment variable RUNONCI;
     * java environment RUNONCI or project variable runOnCI</p>
     */
    boolean runOnCI

    /**
     * Basic info provider
     */
    InfoProvider infoProvider

    /**
     * SCM info provider
     */
    AbstractScmInfoProvider scmProvider

    /**
     * CI info provider
     */
    AbstractCIInfoProvider ciProvider

    /**
     * Calculates the setting for special configuration from the system
     * or java environment or project properties.
     *
     * @param envVar        name of environment variable
     * @param projectVar    name of project variable
     * @param defaultValue  default value
     * @return              the string configuration
     */
    private String getVariable(String envVar, String projectVar, String defaultValue) {
        if(System.properties[envVar]) {
            log.debug('Specified from system property {}.', envVar)
            return System.properties[envVar].toString().trim()
        } else if(System.getenv(envVar)) {
            log.debug('Specified from system environment property {}.', envVar)
            return System.getenv(envVar).toString().trim()
        } else if(project.hasProperty(projectVar) && project."${projectVar}") {
            log.debug('Specified from project property {}.', projectVar)
            return project."${projectVar}".toString().trim()
        }
        return defaultValue
    }
}
