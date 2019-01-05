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
package com.intershop.gradle.buildinfo.scm

import groovy.transform.CompileStatic

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

/**
 * This info provider provides the information of the used Git repository.
 */
@CompileStatic
class GitScmInfoProvider extends AbstractScmInfoProvider {

    /**
     * This is a solution for an bamboo build.
     */
    boolean bambooBuild = false

    /**
     * Constructs the Git information provider
     * @param projectDir
     */
    GitScmInfoProvider(File projectDir) {
        super(projectDir)
    }

    private Repository getGitRepo()
    {
        return new RepositoryBuilder().findGitDir(projectDir).build()
    }
    /**
     * Returns Remote URL of the project (read only)
     * @return remote url
     */
    @Override
    String getSCMOrigin() {
        String origin = ''
        if(bambooBuild) {
            origin = System.getenv('bamboo_planRepository_repositoryUrl')
        }
        if(! origin) {
            Config config = getGitRepo().config
            String rv = config.getString('remote', 'origin', 'url')

            if (rv && rv.startsWith('https://') && rv.contains('@')) {
                origin = "https://${rv.substring(rv.indexOf('@') + 1)}"
            } else {
                origin = rv
            }
        }
        return origin
    }

    /**
     * Returns branch name of the working copy (read only)
     * @return branch name
     */
    @Override
    String getBranchName() {
        return getGitRepo().branch
    }

    /**
     * Returns revision ID of the working copy (read only)
     * @return revision
     */
    @Override
    String getSCMRevInfo() {
        ObjectId id = getGitRepo().resolve(Constants.HEAD)
        String rv = ''

        if(id) {
            rv = id.name?.substring(0,7)
        }

        return rv
    }

    /**
     * Returns time of the latest changes of the working copy (read only)
     * @return time string
     */
    @Override
    String getLastChangeTime() {
        if(SCMRevInfo) {
            RevWalk walk = new RevWalk(gitRepo)
            RevCommit commit = walk.parseCommit(getGitRepo().resolve(Constants.HEAD))
            return new Date( ((long)commit.commitTime)*1000).format("yyyyMMddHHmmss")
        }
        return ''
    }

    /**
     * Returns SCM type (read only)
     * @return 'git'
     */
    @Override
    String getSCMType() {
        return 'git'
    }
}
