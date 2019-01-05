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

import com.intershop.gradle.buildinfo.scm.GitScmInfoProvider
import com.intershop.gradle.test.util.TestDir
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.FetchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import spock.lang.Requires
import spock.lang.Specification

class GitBuildInfoSpec extends Specification {

    @TestDir
    File projectDir

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'calculate module origin and branch'() {
        setup:
        Git.cloneRepository()
                .setURI(System.properties['giturl'])
                .setDirectory(projectDir)
                .setCredentialsProvider( new UsernamePasswordCredentialsProvider( System.properties['gituser'], System.properties['gitpasswd'] ) )
                .call()

        def provider = new GitScmInfoProvider(projectDir)

        when:
        String origin = provider.SCMOrigin

        then:
        origin == System.properties['giturl']

        when:
        String branch = provider.branchName

        then:
        branch == 'master'

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
        type == 'git'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'calculate module origin and branch from tag'() {
        setup:
        Git.cloneRepository()
                .setURI(System.properties['giturl'])
                .setDirectory(projectDir)
                .setCredentialsProvider( new UsernamePasswordCredentialsProvider( System.properties['gituser'], System.properties['gitpasswd'] ) )
                .call()

        Repository repo = new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(projectDir)
                .build()
        Git git = new Git(repo)

        FetchCommand fetch = git.fetch()
        fetch.remote = 'origin'
        fetch.setCheckFetchedObjects(true)
        fetch.setCredentialsProvider( new UsernamePasswordCredentialsProvider( System.properties['gituser'], System.properties['gitpasswd']) )
        fetch.call()

        CheckoutCommand checkout = git.checkout()
        checkout.setName("tags/CLRELEASE_1.5.0")
        checkout.setStartPoint("tags/CLRELEASE_1.5.0")
        checkout.call()

        def provider = new GitScmInfoProvider(projectDir)

        when:
        String origin = provider.SCMOrigin

        then:
        origin == System.properties['giturl']

        when:
        String branch = provider.branchName

        then:
        branch == 'CLRELEASE_1.5.0'

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
        type == 'git'
    }

    def 'no module origin'() {
        setup:
        Git.init()
                .setDirectory(projectDir)
                .call()

        def provider = new GitScmInfoProvider(projectDir)

        when:
        String origin = provider.SCMOrigin

        then:
        origin == 'unknown'

        when:
        String branch = provider.branchName

        then:
        branch == 'master'

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
        type == 'git'
    }
}
