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

import com.intershop.gradle.test.AbstractIntegrationSpec
import groovy.util.logging.Slf4j
import spock.lang.Unroll

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@Unroll
@Slf4j
class LocalBuildInfoSpec extends AbstractIntegrationSpec {

    def 'Add BuildInfo To Jar - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'ivy-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.output.contains('Add buildinfo to ivy file')
        result.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String ivyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text

        1 == ivyFileContents.count('<e:scm-type>local</e:scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        secResult.output.contains('Add buildinfo to ivy file')
        secResult.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String secIvyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text

        1 == secIvyFileContents.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Add BuildInfo to Jars in multi projects'(gradleVersion) {

        String prja = """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

                group = 'com.intershop.testproject'
                version = rootProject.getVersion()

                publishing {
                    repositories {
                        ivy {
                            // change to point to your repo, e.g. http://my.org/repo
                            url "\${rootProject.buildDir}/repo"
                        }
                    }
                    publications {
                        ivya(IvyPublication) {
                            from components.java
                        }
                    }
                }
                """.stripIndent()
        String prjb = """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

                group = 'com.intershop.testproject'
                version = rootProject.getVersion()

                publishing {
                    repositories {
                        ivy {
                            // change to point to your repo, e.g. http://my.org/repo
                            url "\${rootProject.buildDir}/repo"
                        }
                    }
                    publications {
                        ivyb(IvyPublication) {
                            from components.java
                        }
                    }
                }
                """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            rootProject.name= 'multiprojecttest'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'ivy-publish'
            }

            group = 'com.test.root'
            version = '1.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', prja, '1.0.0')
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', prjb, '1.0.0')

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.output.contains('Add buildinfo to ivy file')
        result.output.contains('Add buildinfo to manifest')

        (new File(testProjectDir, 'build/repo/com.test.root/multiprojecttest/1.0.0/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'build/repo/com.intershop.testproject/project1a/1.0.0/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'build/repo/com.intershop.testproject/project2b/1.0.0/ivy-1.0.0.xml')).exists()

        String ivyFileContentsRoot = new File(testProjectDir,  'build/repo/com.test.root/multiprojecttest/1.0.0/ivy-1.0.0.xml').text
        String ivyFileContentsA = new File(testProjectDir,  'build/repo/com.intershop.testproject/project1a/1.0.0/ivy-1.0.0.xml').text
        String ivyFileContentsB = new File(testProjectDir,  'build/repo/com.intershop.testproject/project2b/1.0.0/ivy-1.0.0.xml').text

        1 == ivyFileContentsRoot.count('<e:scm-type>local</e:scm-type>')
        1 == ivyFileContentsA.count('<e:scm-type>local</e:scm-type>')
        1 == ivyFileContentsB.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Jar with runOnCI - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'ivy-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoJarInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.output.contains('Add buildinfo to ivy file')
        ! result.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String ivyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text

        1 == ivyFileContents.count('<e:scm-type>local</e:scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoJarInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        secResult.output.contains('Add buildinfo to ivy file')
        ! secResult.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String secIvyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text

        1 == secIvyFileContents.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Ivy with runOnCI - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'ivy-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoDescriptorInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()
        (new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF')).exists()

        String ivyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text
        assertFalse(ivyFileContents.contains('com.test:test:1.0.0.0'))

        0 == ivyFileContents.count('<e:scm-type>local</e:scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoDescriptorInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String secIvyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text
        assertFalse(secIvyFileContents.contains('com.test:test:1.0.0.0'))

        0 == secIvyFileContents.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To POM with runOnCI - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'maven-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoDescriptorInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()
        (new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF')).exists()

        String pomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text

        0 == pomFileContents.count('<scm-type>local</scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoDescriptorInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()

        String secPomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text

        0 == secPomFileContents.count('<scm-type>local</scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Add BuildInfo To POM - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'maven-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()
        (new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF')).exists()

        String pomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text

        1 == pomFileContents.count('<scm-type>local</scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()

        String secPomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text

        1 == secPomFileContents.count('<scm-type>local</scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Jar - #gradleVersion'(gradleVersion) {
        given:
        writeJavaTestClass('com.intershop.test')

        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.buildinfo'
                id 'ivy-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            publishing {
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        ! result.output.contains('Add buildinfo to pom file')
        ! result.output.contains('Add buildinfo to ivy file')
        ! result.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    private File createSubProjectJava(String projectPath, File settingsGradle, String packageName, String buildContent, String version){
        File subProject = createSubProject(projectPath, settingsGradle, buildContent)
        writeJavaTestClass(packageName, subProject)
        return subProject
    }
}

