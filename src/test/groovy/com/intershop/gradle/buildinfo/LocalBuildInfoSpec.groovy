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

    def 'Add BuildInfo To Jar (Gradle #gradleVersion)'(gradleVersion) {
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

            sourceCompatibility = 1.8
            targetCompatibility = 1.8

            buildinfo.noJarInfo = false

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
        assertTrue(ivyFileContents.contains('com.test:test:1.0.0.0'))

        1 == ivyFileContents.count('<e:scm-type>local</e:scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        secResult.output.contains('Add buildinfo to ivy file')
        ! secResult.output.contains('Add buildinfo to manifest')
        ! result.output.contains('Buildinfo for manifest will be extended!')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String secIvyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text
        assertTrue(secIvyFileContents.contains('com.test:test:1.0.0.0'))

        1 == secIvyFileContents.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Jar with runOnCI (Gradle #gradleVersion)'(gradleVersion) {
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

            sourceCompatibility = 1.8
            targetCompatibility = 1.8

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
                .withArguments('clean', 'publish', '-PrunOnCI=true', '-PadditionalJarInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.output.contains('Add buildinfo to ivy file')
        result.output.contains('Add buildinfo to manifest')
        result.output.contains('Buildinfo for manifest will be extended!')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String ivyFileContents = new File(testProjectDir,  'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text
        assertTrue(ivyFileContents.contains('com.test:test:1.0.0.0'))

        1 == ivyFileContents.count('<e:scm-type>local</e:scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Ivy with runOnCI (Gradle #gradleVersion)'(gradleVersion) {
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

            sourceCompatibility = 1.8
            targetCompatibility = 1.8

            buildinfo.noJarInfo = false

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

        String mfFileContents = new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF').text
        assertTrue(mfFileContents.contains('Implementation-Title: com.test:test:1.0.0.0'))

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

    def 'No BuildInfo To POM with runOnCI (Gradle #gradleVersion)'(gradleVersion) {
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

            sourceCompatibility = 1.8
            targetCompatibility = 1.8

            buildinfo.noJarInfo = false

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

        String mfFileContents = new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF').text
        assertTrue(mfFileContents.contains('Implementation-Title: com.test:test:1.0.0.0'))

        String pomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text
        assertFalse(pomFileContents.contains('com.test:test:1.0.0.0'))

        0 == pomFileContents.count('<scm-type>local</scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '-PnoDescriptorInfo=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()

        String secPomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text
        assertFalse(secPomFileContents.contains('com.test:test:1.0.0.0'))

        0 == secPomFileContents.count('<scm-type>local</scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'Add BuildInfo To POM (Gradle #gradleVersion)'(gradleVersion) {
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

            sourceCompatibility = 1.8
            targetCompatibility = 1.8

            buildinfo.noJarInfo = false

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

        String mfFileContents = new File(testProjectDir, 'build/tmp/jar/MANIFEST.MF').text
        assertTrue(mfFileContents.contains('Implementation-Title: com.test:test:1.0.0.0'))

        String pomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text
        assertTrue(pomFileContents.contains('com.test:test:1.0.0.0'))

        1 == pomFileContents.count('<scm-type>local</scm-type>')

        when:
        def secResult = getPreparedGradleRunner()
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        (new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom')).exists()

        String secPomFileContents = new File(testProjectDir, 'build/repo/com/test/test/1.0.0.0/test-1.0.0.0.pom').text
        assertTrue(secPomFileContents.contains('com.test:test:1.0.0.0'))

        1 == secPomFileContents.count('<scm-type>local</scm-type>')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'No BuildInfo To Jar (Gradle #gradleVersion)'(gradleVersion) {
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
        ! result.output.contains('Buildinfo for manifest will be extended!')
        result.output.contains('Add buildinfo to manifest')
        (new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml')).exists()

        String ivyFileContents = new File(testProjectDir, 'build/repo/com.test/test/1.0.0.0/ivy-1.0.0.0.xml').text
        assertFalse(ivyFileContents.contains('com.test:test:1.0.0.0'))

        where:
        gradleVersion << supportedGradleVersions
    }


}

