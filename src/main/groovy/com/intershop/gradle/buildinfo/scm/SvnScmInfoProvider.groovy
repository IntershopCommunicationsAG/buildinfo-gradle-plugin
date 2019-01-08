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
import groovy.util.logging.Slf4j
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNInfo
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNWCClient
import org.tmatesoft.svn.core.wc.SVNWCUtil

/**
 * This info provider provides the information of the used Subversion repository.
 */
@CompileStatic
@Slf4j
class SvnScmInfoProvider extends AbstractScmInfoProvider {

    /*
     * svn client object for all methods of this provider.
     */
    private final SVNWCClient svnClient

    private String pOrigin = ''
    private String pBranchName = ''
    private String pRevInfo = ''
    private String pChangeTime = ''

    /**
     * Constructs the SVN information provider
     * @param projectDir
     */
    SvnScmInfoProvider(File projectDir) {
        super(projectDir)
        svnClient  = workingCopyClient
    }

    /**
     * Returns an SVN kit client object with all information.
     *
     * @return object with available information
     */
    private SVNInfo getInfo() {
        try {
            return svnClient.doInfo(projectDir, SVNRevision.WORKING)
        } catch(SVNException ex) {
            log.error(ex.message, ex)
            return null
        }
    }

    /**
     * Returns Remote URL of the project (read only)
     * @return remote url
     */
    @Override
    String getSCMOrigin() {
        if(info && ! pOrigin ) {
            pOrigin = info.getURL().toString()
        }

        if(! pOrigin) {
            pOrigin = UNKNOWN
        }

        return pOrigin
    }

    /**
     * Returns branch name of the working copy (read only)
     * @return branch name
     */
    @Override
    String getBranchName() {
        if(! getSCMOrigin().equals(UNKNOWN) && ! pBranchName) {
            List splitPath = Arrays.asList(getSCMOrigin().split('/'))

            pBranchName = 'trunk'
            // if svn URL contains "/tags/", then find the name of the branch
            if (getSCMOrigin().contains('/tags/') && splitPath.indexOf('tags') < splitPath.size() - 1) {
                pBranchName = "tags/${splitPath[splitPath.indexOf('tags') + 1]}"
            }
                // if svn URL contains "/branches/", then find the name of the branch
            if (getSCMOrigin().contains('/branches/') && splitPath.indexOf('branches') < splitPath.size() - 1) {
                pBranchName = "branches/${splitPath[splitPath.indexOf('branches') + 1]}"
            }
        }
        return pBranchName
    }

    /**
     * Returns revision ID of the working copy (read only)
     * @return revision
     */
    @Override
    String getSCMRevInfo() {
        if(info && ! pRevInfo ) {
            SVNRevision rev = info.revision
            pRevInfo = rev.number
        }
        if (! pRevInfo) {
            pRevInfo = UNKNOWN
        }
        return pRevInfo
    }

    /**
     * Returns time of the latest changes of the working copy (read only)
     * @return time string
     */
    @Override
    String getLastChangeTime() {
        if(info && ! pChangeTime ) {
            SVNRevision rev = info.revision
            String rv = rev?.date?.format('yyyyMMddHHmmss')
            pChangeTime = rv ? rv : UNKNOWN
        }
        if(! pChangeTime) {
            pChangeTime = UNKNOWN
        }
        return pChangeTime
    }

    /**
     * Returns SCM type (read only)
     * @return 'svn'
     */
    @Override
    String getSCMType() {
        if(info) {
            return 'svn'
        }
        return 'unknown'
    }

    /**
     * Creates the client object based on the file system.
     *
     * @return svn kit client object
     */
    protected static SVNWCClient getWorkingCopyClient() {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true)
        SVNClientManager clientManager = SVNClientManager.newInstance(options)
        SVNWCClient client = clientManager.getWCClient()
        return client
    }
}
