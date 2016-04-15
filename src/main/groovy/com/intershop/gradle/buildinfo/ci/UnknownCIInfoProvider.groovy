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
package com.intershop.gradle.buildinfo.ci

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * This is the implementation for a local environment.
 * It uses information from the local file system.
 */
@CompileStatic
@Slf4j
class UnknownCIInfoProvider extends AbstractCIInfoProvider {

    /**
     * This is used instead of build number and build plan.
     */
    public static final String LOCAL = 'LOCAL'

    /**
     * Constructs the CI information provider for a local file system.
     * @param projectDir
     */
    UnknownCIInfoProvider(File projectDir) {
        super(projectDir)
        log.debug('Unknown CI info provider initialized.')
    }

    /**
     * Returns build number (read only)
     * For this implementation it is always 'LOCAL'
     *
     * @return build number
     */
    @Override
    String getBuildNumber() {
        return LOCAL
    }

    /**
     * Returns build plan url (read only)
     * For this implementation it is always the URL of the project dir.
     *
     * @return plan url
     */
    @Override
    String getBuildUrl() {
        return projectDir.toURI().toURL().toString()
    }

    /**
     * Returns build job name (read only)
     * For this implementation it is always 'LOCAL'
     *
     * @return job name
     */
    @Override
    String getBuildJob() {
        return LOCAL
    }

    /**
     * Returns build time (read only)
     * @return time string
     */
    @Override
    String getBuildTime() {
        return new Date().toString()
    }
}
