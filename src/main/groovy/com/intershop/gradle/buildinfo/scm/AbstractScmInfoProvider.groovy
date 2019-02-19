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
 * This info provider provides the information of the used SCM.
 */
@CompileStatic
abstract class AbstractScmInfoProvider {

    protected final static String UNKNOWN = 'unknown'

    protected final File projectDir

    /**
     * Constructs the SCM information provider
     * @param projectDir
     */
    AbstractScmInfoProvider(File projectDir) {
        this.projectDir = projectDir
    }

    /**
     * Returns Remote URL of the project (read only)
     * @return remote url
     */
    abstract String getSCMOrigin()

    /**
     * Returns branch name of the working copy (read only)
     * @return branch name
     */
    abstract String getBranchName()

    /**
     * Returns revision ID of the working copy (read only)
     * @return revision
     */
    abstract String getSCMRevInfo()

    /**
     * Returns time of the latest changes of the working copy (read only)
     * @return time string
     */
    abstract String getLastChangeTime()

    /**
     * Returns SCM type (read only)
     * @return type string
     */
    abstract String getSCMType()
}
