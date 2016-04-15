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
 * This is the implementation for Gitlab CI server.
 * It uses the environment variables from Bamboo.
 */
@CompileStatic
@Slf4j
class TravisCIInfoProvider extends AbstractCIInfoProvider {

    /**
     * Constructs the CI information provider for Gitlab CI Bamboo
     * @param projectDir
     */
    TravisCIInfoProvider(File projectDir) {
        super(projectDir)
        log.debug('Travis CI info provider initialized.')
    }

    /**
     * Returns build number (bamboo_buildNumber)(read only)
     * @return build number
     */
    @Override
    String getBuildNumber() {
        return getEnvironmentVariable('TRAVIS_BUILD_NUMBER')
    }

    /**
     * Returns build plan url (read only)
     * @return plan url
     */
    @Override
    String getBuildUrl() {
        return getSecuredURL(getEnvironmentVariable('TRAVIS_REPO_SLUG'))
    }

    /**
     * Returns build job name (read only)
     * @return job name
     */
    @Override
    String getBuildJob() {
        return getEnvironmentVariable('TRAVIS_BUILD_DIR')
    }

    /**
     * Returns build time (read only)
     * @return time string
     */
    @Override
    String getBuildTime() {
        return getEnvironmentVariable('CI_BUILD_TIMESTAMP')
    }
}
