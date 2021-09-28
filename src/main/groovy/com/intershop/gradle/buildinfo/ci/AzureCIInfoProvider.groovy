package com.intershop.gradle.buildinfo.ci

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * This is the implementation for Gitlab CI server.
 * It uses the environment variables from Bamboo.
 */
@CompileStatic
@Slf4j
class AzureCIInfoProvider extends AbstractCIInfoProvider {

    /**
     * Constructs the CI information provider for MS Azure
     * @param projectDir
     */
    AzureCIInfoProvider(File projectDir) {
        super(projectDir)
        log.debug('MS Azure info provider initialized.')
    }

    /**
     * Returns build number (bamboo_buildNumber)(read only)
     * @return build number
     */
    @Override
    String getBuildNumber() {
        return getEnvironmentVariable('BUILD_BUILDNUMBER')
    }

    /**
     * Returns build plan url (read only)
     * @return plan url
     */
    @Override
    String getBuildUrl() {
        def url1 = "${getEnvironmentVariable('SYSTEM_TEAMFOUNDATIONCOLLECTIONURI')}"
        def url2 = "${getEnvironmentVariable('SYSTEM_TEAMPROJECT')}"
        def url3 = "/_build/results?buildId=${getEnvironmentVariable('BUILD_BUILDID')}&view=results"

        return getSecuredURL("${url1}${url2}${url3}")
    }

    /**
     * Returns build job name (bamboo_planKey)(read only)
     * @return job name
     */
    @Override
    String getBuildJob() {
        return getEnvironmentVariable('BUILD_DEFINITIONNAME')
    }

    /**
     * Returns build time (bamboo_buildTimeStamp)(read only)
     * @return time string
     */
    @Override
    String getBuildTime() {
        return getEnvironmentVariable('SYSTEM_PIPELINESTARTTIME')
    }
}
