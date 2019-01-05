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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
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

    private final Repository gitRepo

    private String pOrigin = ''
    private String pBranchName = ''
    private String pRevInfo = ''
    private String pChangeTime = ''

    /**
     * Constructs the Git information provider
     * @param projectDir
     */
    GitScmInfoProvider(File projectDir) {
        super(projectDir)

        gitRepo = new RepositoryBuilder().findGitDir(projectDir).build()
    }

    /**
     * Returns Remote URL of the project (read only)
     * @return remote url
     */
    @Override
    String getSCMOrigin() {
        if(! pOrigin) {
            if (bambooBuild) {
                pOrigin = System.getenv('bamboo_planRepository_repositoryUrl')
            }
            if (! pOrigin) {
                Config config = gitRepo.config
                String rv = config.getString('remote', 'origin', 'url')

                if (rv && rv.startsWith('https://') && rv.contains('@')) {
                    pOrigin = "https://${rv.substring(rv.indexOf('@') + 1)}"
                } else {
                    pOrigin = rv ? rv : UNKNOWN
                }
            }
        }
        return pOrigin
    }

    /**
     * Returns branch name of the working copy (read only)
     * @return branch name
     */
    @CompileDynamic
    @Override
    String getBranchName() {
        if(! pBranchName) {
            String rv = gitRepo.branch

            Git git = new Git(gitRepo)
            List<Ref> refList = git.tagList().call()

            refList.any { Ref ref ->
                Ref peeledRef = gitRepo.peel(ref)
                String hashID = ref.getObjectId()
                if (peeledRef.getPeeledObjectId() != null) {
                    hashID = peeledRef.getPeeledObjectId().getName()
                }
                if (hashID == rv) {
                    rv = ref.getName()
                    return true
                }
            }

            pBranchName = rv.split('/').last()
        }

        return pBranchName
    }

    /**
     * Returns revision ID of the working copy (read only)
     * @return revision
     */
    @Override
    String getSCMRevInfo() {
        if(! pRevInfo) {
            ObjectId id = gitRepo.resolve(Constants.HEAD)

            if (id) {
                pRevInfo = id.name?.substring(0, 7)
            } else {
                pRevInfo = UNKNOWN
            }
        }
        return pRevInfo
    }

    /**
     * Returns time of the latest changes of the working copy (read only)
     * @return time string
     */
    @Override
    String getLastChangeTime() {
        if(! getSCMRevInfo().equals('unknown')) {
            RevWalk walk = new RevWalk(gitRepo)
            RevCommit commit = walk.parseCommit(gitRepo.resolve(Constants.HEAD))
            pChangeTime = new Date( ((long)commit.commitTime)*1000).format("yyyyMMddHHmmss")
        } else {
            pChangeTime = UNKNOWN
        }
        return pChangeTime
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
