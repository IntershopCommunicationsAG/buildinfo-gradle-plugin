/*
 * Copyright 2022 Intershop Communications AG.
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
 * limitations under the License.
 */
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // project plugins
    `java-gradle-plugin`
    groovy

    // test coverage
    jacoco

    // ide plugin
    idea

    // publish plugin
    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    // intershop version plugin
    id("com.intershop.gradle.scmversion") version "6.2.0"

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "0.20.0"
}

scm {
    version.initialVersion = "1.0.0"
}

// release configuration
group = "com.intershop.gradle.buildinfo"
description = "Gradle Buildinfo Plugin"
version = scm.version.version

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

repositories {
    mavenCentral()
}

val pluginId = "com.intershop.gradle.buildinfo"

gradlePlugin {
    plugins {
        create("buildinfoPlugin") {
            id = pluginId
            implementationClass = "com.intershop.gradle.buildinfo.BuildInfoPlugin"
            displayName = project.name
            description = project.description
        }
    }
}

pluginBundle {
    val pluginURL = "https://github.com/IntershopCommunicationsAG/${project.name}"
    website = pluginURL
    vcsUrl = pluginURL
    tags = listOf("intershop", "release", "build", "info")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    withJavadocJar()
}

sourceSets.main {
    java.setSrcDirs(listOf<String>())
    groovy.setSrcDirs(mutableListOf("src/main/groovy", "src/main/java"))
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

tasks {
    withType<Test>().configureEach {
        systemProperty("intershop.gradle.versions", "7.4")
        systemProperty("org.gradle.native.dir", ".gradle")

        if(! System.getenv("GITUSER").isNullOrBlank() &&
                ! System.getenv("GITPASSWD").isNullOrBlank() &&
                ! System.getenv("GITURL").isNullOrBlank()) {
            systemProperty("giturl", System.getenv("GITURL"))
            systemProperty("gituser", System.getenv("GITUSER"))
            systemProperty("gitpasswd", System.getenv("GITPASSWD"))
        }

        testLogging {
            events = mutableSetOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL

            showStackTraces = true
            showStandardStreams = true
        }

        useJUnitPlatform()

        dependsOn("jar")
    }

    val copyAsciiDoc = register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = file("$buildDir/tmp/asciidoctorSrc")
        val inputFiles = fileTree(mapOf("dir" to rootDir,
                "include" to listOf("**/*.asciidoc"),
                "exclude" to listOf("build/**")))

        inputs.files.plus( inputFiles )
        outputs.dir( outputDir )

        doFirst {
            outputDir.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn(copyAsciiDoc)

        setSourceDir(file("$buildDir/tmp/asciidoctorSrc"))
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        options = mapOf( "doctype" to "article",
            "ruby"    to "erubis")
        attributes = mapOf(
            "latestRevision"        to  project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true")
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)

            html.outputLocation.set(File(project.buildDir, "jacocoHtml"))
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("jar").dependsOn("asciidoctor")
}

publishing {
    publications {
        create<MavenPublication>("intershopMvn") {
            from(components["java"])

            artifact(File(buildDir, "docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
                developers {
                    developer {
                        id.set("m-raab")
                        name.set("M. Raab")
                        email.set("mraab@intershop.de")
                    }
                }
                scm {
                    connection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

dependencies {
    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    runtimeOnly("org.apache.httpcomponents:httpclient:4.5.5")
    runtimeOnly("org.slf4j:slf4j-api:1.7.16")

    testImplementation("com.intershop.gradle.test:test-gradle-plugin:4.1.1")
    testImplementation(gradleTestKit())
}

