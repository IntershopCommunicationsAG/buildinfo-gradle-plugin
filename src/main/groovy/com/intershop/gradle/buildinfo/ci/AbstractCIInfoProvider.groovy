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

/**
 * This is the base class for all continuous integration provider.
 */
@CompileStatic
abstract class AbstractCIInfoProvider {

    protected final File projectDir

    /**
     * Constructs the CI information provider
     * @param projectDir
     */
    AbstractCIInfoProvider(File projectDir) {
        this.projectDir = projectDir
    }

    /**
     * Returns build number (read only)
     * @return build number
     */
    abstract String getBuildNumber()

    /**
     * Returns build plan url (read only)
     * @return plan url
     */
    abstract String getBuildUrl()

    /**
     * Returns build job name (read only)
     * @return job name
     */
    abstract String getBuildJob()

    /**
     * Returns build time (read only)
     * @return time string
     */
    abstract String getBuildTime()

    /**
     * Returns build host name (read only)
     * @return host name
     */
    String getBuildHost() {
        return InetAddress.localHost.hostName
    }

    /**
     * Returns the environment variable for specific key.
     *
     * @param envKey
     * @return
     */
    protected static String getEnvironmentVariable(String envKey) {
        System.getenv(envKey)
    }

    protected static String getSecuredURL(String urlString) {
        if (urlString && urlString.contains('@')) {
            return "https://${urlString.substring(urlString.indexOf('@') + 1)}"
        } else {
            return urlString
        }
    }
}
