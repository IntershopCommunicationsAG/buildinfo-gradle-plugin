import com.jfrog.bintray.gradle.BintrayExtension
import org.asciidoctor.gradle.AsciidoctorExtension
import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Date

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
 * limitations under the License.
 */
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

    // intershop version plugin
    id("com.intershop.gradle.scmversion") version "4.1.0"

    // plugin for documentation
    id("org.asciidoctor.convert") version "1.5.10"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "0.10.1"

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

scm {
    version.initialVersion = "1.0.0"
}

// release configuration
group = "com.intershop.gradle.buildinfo"
description = "Gradle Buildinfo Plugin"
version = scm.version.version

val pluginId = "com.intershop.gradle.buildinfo"

gradlePlugin {
    plugins {
        create("jaxbPlugin") {
            id = pluginId
            implementationClass = "com.intershop.gradle.buildinfo.BuildInfoPlugin"
            displayName = project.name
            description = project.description
        }
    }
}

pluginBundle {
    website = "https://github.com/IntershopCommunicationsAG/${project.name}"
    vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
    tags = listOf("intershop", "gradle", "plugin", "release", "build", "info")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets.main {
    java.setSrcDirs(emptyList())
    withConvention(GroovySourceSet::class) {
        groovy.setSrcDirs(mutableListOf("src/main/groovy", "src/main/java"))
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

configure<AsciidoctorExtension> {
    noDefaultRepositories = true
}

tasks {
    withType<Test>().configureEach {
        systemProperty("intershop.gradle.versions", "5.2")
        systemProperty("org.gradle.native.dir", ".gradle")

        if(! System.getenv("SVNUSER").isNullOrBlank() &&
                ! System.getenv("SVNPASSWD").isNullOrBlank() &&
                ! System.getenv("SVNURL").isNullOrBlank()) {
            systemProperty("svnurl", System.getenv("SVNURL"))
            systemProperty("svnuser", System.getenv("SVNUSER"))
            systemProperty("svnpasswd", System.getenv("SVNPASSWD"))
        }
        if(! System.getenv("GITUSER").isNullOrBlank() &&
                ! System.getenv("GITPASSWD").isNullOrBlank() &&
                ! System.getenv("GITURL").isNullOrBlank()) {
            systemProperty("giturl", System.getenv("GITURL"))
            systemProperty("gituser", System.getenv("GITUSER"))
            systemProperty("gitpasswd", System.getenv("GITPASSWD"))
        }

        if(! System.getProperty("SVNUSER").isNullOrBlank() &&
                ! System.getProperty("SVNPASSWD").isNullOrBlank() &&
                ! System.getProperty("SVNURL").isNullOrBlank()) {
            systemProperty("svnurl", System.getProperty("SVNURL"))
            systemProperty("svnuser", System.getProperty("SVNUSER"))
            systemProperty("svnpasswd", System.getProperty("SVNPASSWD"))
        }

        if(! System.getProperty("GITUSER").isNullOrBlank() &&
                ! System.getProperty("GITPASSWD").isNullOrBlank() &&
                ! System.getProperty("GITURL").isNullOrBlank()) {
            systemProperty("giturl", System.getProperty("GITURL"))
            systemProperty("gituser", System.getProperty("GITUSER"))
            systemProperty("gitpasswd", System.getProperty("GITPASSWD"))
        }

        testLogging {
            events = mutableSetOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL

            showStackTraces = true
            showStandardStreams = true
        }

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
        dependsOn("copyAsciiDoc")

        sourceDir = file("$buildDir/tmp/asciidoctorSrc")
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        backends("html5", "docbook")
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
            xml.isEnabled = true
            html.isEnabled = true

            html.destination = File(project.buildDir, "jacocoHtml")
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("bintrayUpload")?.dependsOn("asciidoctor")
    getByName("jar")?.dependsOn("asciidoctor")



    register<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."

        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    register<Jar>("javaDoc") {
        dependsOn(groovydoc)
        from(groovydoc)
        getArchiveClassifier().set("javadoc")
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])
            artifact(tasks.getByName("sourceJar"))
            artifact(tasks.getByName("javaDoc"))

            artifact(File(buildDir, "asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom.withXml {
                val root = asNode()
                root.appendNode("name", project.name)
                root.appendNode("description", project.description)
                root.appendNode("url", "https://github.com/IntershopCommunicationsAG/${project.name}")

                val scm = root.appendNode("scm")
                scm.appendNode("url", "https://github.com/IntershopCommunicationsAG/${project.name}")
                scm.appendNode("connection", "git@github.com:IntershopCommunicationsAG/${project.name}.git")

                val org = root.appendNode("organization")
                org.appendNode("name", "Intershop Communications")
                org.appendNode("url", "http://intershop.com")

                val license = root.appendNode("licenses").appendNode("license")
                license.appendNode("name", "Apache License, Version 2.0")
                license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0")
                license.appendNode("distribution", "repo")
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications("intershopMvn")

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = project.name
        userOrg = "intershopcommunicationsag"

        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"

        desc = project.description
        websiteUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        issueTrackerUrl = "https://github.com/IntershopCommunicationsAG/${project.name}/issues"

        setLabels("intershop", "gradle", "plugin", "release", "build", "info")
        publicDownloadNumbers = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            released  = Date().toString()
            vcsTag = project.version.toString()
        })
    })
}

dependencies {
    //svn
    // replace svnkit"s JNA 4.x with 3.2.7, which is used by Gradle itself
    implementation("org.tmatesoft.svnkit:svnkit:1.9.3") {
        exclude(group = "net.java.dev.jna")
        exclude(group = "com.trilead", module = "trilead-ssh2")
    }
    implementation("com.trilead:trilead-ssh2:1.0.0-build221")
    runtimeOnly("net.java.dev.jna:jna:4.1.0")

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.0.1.201806211838-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    runtimeOnly("org.apache.httpcomponents:httpclient:4.5.5")
    runtimeOnly("org.slf4j:slf4j-api:1.7.16")

    testImplementation("com.intershop.gradle.test:test-gradle-plugin:3.1.0-dev.2")
    testImplementation(gradleTestKit())
}

repositories {
    jcenter()
}