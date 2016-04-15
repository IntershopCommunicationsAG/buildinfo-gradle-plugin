#!/bin/bash
# This script will build the project.

export JAVA_OPTS="-Xmx1024M -XX:MaxPermSize=512M -XX:ReservedCodeCacheSize=512M"
export GRADLE_OPTS="-Dorg.gradle.daemon=true"

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
        git status
        ./gradlew test build -s
        git status
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        git status
        ./gradlew -PrunOnCI=true test build :bintrayUpload :publishPlugins -s
        git status
    fi
else
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
        git status
        ./gradlew test build -s
        git status
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        git status
        ./gradlew -PrunOnCI=true test build :bintrayUpload :publishPlugins -s
        git status
    fi
fi