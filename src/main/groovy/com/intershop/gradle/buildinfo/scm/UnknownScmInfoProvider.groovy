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

/**
 * This info provider provides the information of the used file system repository.
 */
@CompileStatic
class UnknownScmInfoProvider extends AbstractScmInfoProvider {

    /**
     * Constructs the local information provider
     * @param projectDir
     */
    def UnknownScmInfoProvider(File projectDir) {
        super(projectDir)
    }

    /**
     * Returns Remote URL of the project (read only)
     * In this implementation it is the URL of the local path.
     *
     * @return remote url
     */
    @Override
    String getSCMOrigin() {
        return projectDir.absoluteFile.toURI().toString()
    }

    /**
     * Returns branch name of the working copy (read only)
     * It returns for this implementation always 'localFileSystem'.
     *
     * @return branch name
     */
    @Override
    String getBranchName() {
        return 'localFileSystem'
    }

    /**
     * Returns revision ID of the working copy (read only)
     * It returns for this implementation the time stamp of the latest change.
     * @return revision
     */
    @Override
    String getSCMRevInfo() {
        return Long.toString(projectDir.lastModified())
    }

    /**
     * Returns revision ID of the working copy (read only)
     *
     * @return revision
     */
    @Override
    String getLastChangeTime() {
        return (new Date(projectDir.lastModified())).format("yyyyMMddHHmmss")
    }

    /**
     * Returns SCM type (read only)
     * @return 'local'
     */
    @Override
    String getSCMType() {
        return 'local'
    }
}
