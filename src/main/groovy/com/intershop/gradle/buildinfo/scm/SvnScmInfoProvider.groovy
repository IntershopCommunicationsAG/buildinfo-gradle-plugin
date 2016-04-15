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
        if(info) {
            String rv = info.getURL().toString()
            return rv
        }
        return ''
    }

    /**
     * Returns branch name of the working copy (read only)
     * @return branch name
     */
    @Override
    String getBranchName() {
        String url = getSCMOrigin()
        String rv = ''
        if(url) {
            List splitPath = Arrays.asList(url.split('/'))

            rv = 'trunk'
            // if svn URL contains "/tags/", then find the name of the branch
            if (url.contains('/tags/') && splitPath.indexOf('tags') < splitPath.size() - 1) {
                rv = "tags/${splitPath[splitPath.indexOf('tags') + 1]}"
            }
            // if svn URL contains "/branches/", then find the name of the branch
            if (url.contains('/branches/') && splitPath.indexOf('branches') < splitPath.size() - 1) {
                rv = "branches/${splitPath[splitPath.indexOf('branches') + 1]}"
            }
        }
        return rv
    }

    /**
     * Returns revision ID of the working copy (read only)
     * @return revision
     */
    @Override
    String getSCMRevInfo() {
        if(info) {
            SVNRevision rev = info.revision
            return rev.number
        }
        return ''
    }

    /**
     * Returns time of the latest changes of the working copy (read only)
     * @return time string
     */
    @Override
    String getLastChangeTime() {
        if(info) {
            SVNRevision rev = info.revision
            String rv = rev?.date?.format('yyyyMMddHHmmss')
            if(rv) {
                return rv
            }
        }
        return '-'
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
