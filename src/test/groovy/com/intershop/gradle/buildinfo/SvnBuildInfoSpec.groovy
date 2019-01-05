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
import com.intershop.gradle.buildinfo.scm.SvnScmInfoProvider
import com.intershop.gradle.test.util.TestDir
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.SvnCheckout
import org.tmatesoft.svn.core.wc2.SvnOperationFactory
import org.tmatesoft.svn.core.wc2.SvnTarget
import spock.lang.Requires
import spock.lang.Specification

class SvnBuildInfoSpec extends Specification {

    @TestDir
    File projectDir

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate module origin and branch'() {
        setup:
        svnCheckOut(projectDir, System.properties['svnurl'])

        def provider = new SvnScmInfoProvider(projectDir)

        when:
        String origin = provider.SCMOrigin

        then:
        origin == "${System.properties['svnurl']}/trunk"

        when:
        String branch = provider.branchName

        then:
        branch == 'trunk' || branch.startsWith('tags') || branch.startsWith('branches')

        when:
        String rev = provider.SCMRevInfo

        then:
        rev != ''

        when:
        String time = provider.lastChangeTime

        then:
        time != ''

        when:
        String type = provider.SCMType

        then:
        type == 'svn'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate module origin and branch of local filesystem'() {
        setup:
        def provider = new SvnScmInfoProvider(projectDir)

        when:
        String origin = provider.SCMOrigin

        then:
        origin == 'unknown'

        when:
        String branch = provider.branchName

        then:
        branch == ''

        when:
        String rev = provider.SCMRevInfo

        then:
        rev == 'unknown'

        when:
        String time = provider.lastChangeTime

        then:
        time == 'unknown'

        when:
        String type = provider.SCMType

        then:
        type == 'unknown'
    }

    protected void svnCheckOut(File target, String source) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'], System.properties['svnpasswd'].toCharArray())
        svnOperationFactory.setAuthenticationManager(authenticationManager);
        try {
            final SvnCheckout checkout = svnOperationFactory.createCheckout()
            checkout.setSingleTarget(SvnTarget.fromFile(target))
            checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIEncoded("${source}/trunk")))
            checkout.run()
        } finally {
            svnOperationFactory.dispose();
        }
    }

}
