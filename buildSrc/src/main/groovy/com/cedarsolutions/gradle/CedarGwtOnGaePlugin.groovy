// vim: set ft=groovy ts=4 sw=4:
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// *              C E D A R
// *          S O L U T I O N S       "Software done right."
// *           S O F T W A R E
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// * Copyright (c) 2013 Kenneth J. Pronovici.
// * All rights reserved.
// *
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the Apache License, Version 2.0.
// * See LICENSE for more information about the licensing terms.
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// * Author   : Kenneth J. Pronovici <pronovic@ieee.org>
// * Language : Gradle (>= 1.7)
// * Project  : Secret Santa Exchange
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.cedarsolutions.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.gae.GaePlugin

/**
 * The cedarGwtOnGae plugin.
 * @author Kenneth J. Pronovici <pronovic@ieee.org>
 */
class CedarGwtOnGaePlugin implements Plugin<Project> {

    /** Apply the plugin. */
    void apply(Project project) {
        project.plugins.apply(GaePlugin)
        project.plugins.apply(JavaPlugin)

        project.extensions.create("cedarGwtOnGae", CedarGwtOnGaePluginExtension, project)
        project.extensions.create("cedarCucumber", CedarCucumberPluginExtension, project)

        project.convention.plugins.cedarGwtOnGae = new CedarGwtOnGaePluginConvention(project)
        project.convention.plugins.cedarCucumber = new CedarCucumberPluginConvention(project)

        applyCedarGwtOnGae(project)
        applyCedarCucumber(project)
        applyProjectTestSuites(project)
    }

    /** Apply cedarGwtOnGae. */
    void applyCedarGwtOnGae(Project project) {

        // We need to download the SDK before the classpath can be generated properly.
        project.tasks.compileJava.dependsOn(project.tasks.gaeDownloadSdk)

        // Tell the GAE plugin to download the SDK
        project.convention.plugins.gae.downloadSdk = true

        // Set up the war plugin
        project.webAppDirName = "war"  // for some reason, I can't pull this out into cedarGwtOnGae configuration (?)
        project.war {
            // The gradle build should not get any of the files generated by Eclipse
            excludes += [ "*.JUnit/**",
                          "*.JUnit",
                          "WEB-INF/deploy/**/*",
                          "WEB-INF/deploy",
                          "WEB-INF/classes/**/*",
                          "WEB-INF/classes",
                          "WEB-INF/lib/**/*",
                          "WEB-INF/lib",
                          "WEB-INF/appengine-generated/**/*",
                          "WEB-INF/appengine-generated", ]
        }

        // Compile GWT into the exploded war directory
        project.task("buildApplication", dependsOn: project.tasks.gaeExplodeWar) << {
            project.cedarGwtOnGae.validateGwtConfig()

            def warDir = project.gaeExplodeWar.explodedWarDirectory.getPath()
            def moduleDir = project.file(warDir + "/" + project.cedarGwtOnGae.getAppModuleName()).canonicalPath
            def classesDir = project.file(warDir + "/WEB-INF/classes").canonicalPath
            def libDir = project.file(warDir + "/WEB-INF/lib").canonicalPath
            def compilerClass = "com.google.gwt.dev.Compiler"

            project.file(moduleDir).deleteDir()  // It's proved difficult to exclude this via project.war.excludes

            project.ant.java(classname : compilerClass, fork : "true", failonerror : "true") {
                jvmarg(value: "-Xmx" + project.cedarGwtOnGae.getGwtCompilerMemory())
                arg(value : "-war")
                arg(value : warDir)
                arg(value : project.cedarGwtOnGae.getAppEntryPoint())
                classpath() {
                    pathelement(path: project.configurations.providedRuntime.asPath)
                    fileset(dir: libDir, includes : "*.jar")
                    pathelement(location: classesDir)
                    project.sourceSets.main.java.srcDirs.each { dir ->
                        pathelement(location: dir)
                    }
                }
            }
        }

        // Start the development mode server
        project.task("startDevmode") << {
            project.convention.plugins.cedarGwtOnGae.bootDevmode()
        }

        // Stop the development mode server
        project.task("stopDevmode") << {
            project.convention.plugins.cedarGwtOnGae.killDevmode()
        }

        // Reboot the development mode server
        project.task("rebootDevmode") << {
            project.convention.plugins.cedarGwtOnGae.rebootDevmode()
        }

    }

    /** Apply cedarCucumber. */
    void applyCedarCucumber(Project project) {

        // Install all of the Cucumber-related tooling
        project.task("installCucumber") << {
            project.convention.plugins.cedarCucumber.installCucumber()
        }

        // Uninstall all of the Cucumber-related tooling
        project.task("uninstallCucumber") << {
            project.convention.plugins.cedarCucumber.uninstallCucumber()
        }

        // Reinstall all of the Cucumber-related tooling
        project.task("reinstallCucumber", dependsOn: [project.tasks.uninstallCucumber, project.tasks.installCucumber]) << {
        }
    
        // If they invoke both, uninstall and then reinstall
        project.tasks.installCucumber.mustRunAfter project.tasks.uninstallCucumber

        // Verify the configured cucumber install is ok.
        project.task("verifyCucumber") << {
            project.convention.plugins.cedarCucumber.verifyCucumberInstall()
        }

        // Run the Cucumber tests, assuming the devmode server is already up
        // Note that you have to manually build the application and boot devmode for this to work
        project.task("runCucumber", dependsOn: project.tasks.verifyCucumber) << {
            project.convention.plugins.cedarCucumber.execCucumber(null, null).assertNormalExitValue()
        }

        // Run the Cucumber tests, including a reboot of the server
        // Note that you have to manually build the application for this to work
        project.task("runCucumberWithReboot", dependsOn: project.tasks.verifyCucumber) << {
            project.convention.plugins.cedarGwtOnGae.rebootDevmode()
            project.convention.plugins.cedarGwtOnGae.waitForDevmode()
            project.convention.plugins.cedarCucumber.execCucumber(null, null).assertNormalExitValue()
        }

        // Run the Cucumber tests, restricting by name containing a substring, assuming the devmode server is already up
        // Note that you have to manually build the application and boot devmode for this to work
        // This does not depend on verifyCucumber because that step is really slow with JRuby and this is a time-saving task.
        project.task("runCucumberByName") << {
            def name = null
            project.convention.plugins.cedarBuild.getInput("Configure Cucumber", "Test Name", false, { input -> name = input})
            project.convention.plugins.cedarCucumber.execCucumber(name, null).assertNormalExitValue()
        }

        // Run the Cucumber tests for a specific feature file, assuming the devmode server is already up
        // Note that you have to manually build the application and boot devmode for this to work
        // This does not depend on verifyCucumber because that step is really slow with JRuby and this is a time-saving task.
        project.task("runCucumberByFeature") << {
            def feature = null
            project.convention.plugins.cedarBuild.getInput("Configure Cucumber", "Feature Path", false, { input -> feature = input})
            project.convention.plugins.cedarCucumber.execCucumber(null, feature).assertNormalExitValue()
        }

    }

    /** Apply projectTestSuites. */
    void applyProjectTestSuites(Project project) {

        // Run unit tests, assumed to be found in a class suites/UnitTestSuite.
        // The caller that applies this plugin still has responsibility for making sure the suite gets compiled.
        project.task("unittest", type: com.cedarsolutions.gradle.TestTask) {

            // these configuration values are set when the task is created
            workingDir = project.projectDir
            scanForTestClasses = false
            enableAssertions = false
            outputs.upToDateWhen { false }
            include "suites/UnitTestSuite.class"

            // these configuration values are set immediately before the test is executed
            deferredConfig {
                // Note: it's important to add onto the JVM args rather than replace them.
                // Otherwise, important arguments like the Jacoco coverage agent don't get included as expected.
                setMaxHeapSize(project.cedarGwtOnGae.getUnitTestMemory())
                setJvmArgs(getJvmArgs() + [ "-XX:MaxPermSize=" + project.cedarGwtOnGae.getUnitTestPermgen(), ])
            }

        }

        // Run GWT client tests, assumed to be found in a class suites/ClientTestSuite.
        // The caller that applies this plugin still has responsibility for making sure the suite gets compiled.
        project.task("clienttest", type: com.cedarsolutions.gradle.TestTask) {

            // these configuration values are set when the task is created
            workingDir = project.projectDir
            scanForTestClasses = false
            scanForTestClasses = false
            enableAssertions = false
            outputs.upToDateWhen { false }
            systemProperty "gwt.args", "-out www-test -logLevel ERROR"
            systemProperty "java.awt.headless", "true"   // required on Linux to avoid deferred binding errors
            include "suites/ClientTestSuite.class"

            // these configuration values are set immediately before the test is executed
            deferredConfig {
                // Here, we *do* replace the JVM args, because this is apparently the only way
                // to disable Jacoco on a test task.  Replacing the JVM arguments discards
                // the -javaagent that Gradle adds into the list.  This is a big hammer approach,
                // but it's clear from looking at the Gradle code that there's no way to configure
                // the plugin to run for some test tasks and ignore others.  Poor design.
                setMaxHeapSize(project.cedarGwtOnGae.getClientTestMemory())
                setJvmArgs([ "-XX:MaxPermSize=" + project.cedarGwtOnGae.getClientTestPermgen(), ])
            }

            // delete the cache directories before running the suite
            beforeSuite { descriptor ->
                if (descriptor.className == "suites.ClientTestSuite") {
                    def wwwTest = project.file(workingDir.canonicalPath + "/www-test")
                    def gwtCache = project.file(workingDir.canonicalPath + "/gwt-unitCache")
                    wwwTest.deleteDir()
                    gwtCache.deleteDir()
                }
            }

            // delete the cache directories after running the suite
            afterSuite { descriptor ->
                if (descriptor.className == "suites.ClientTestSuite") {
                    def wwwTest = project.file(workingDir.canonicalPath + "/www-test")
                    def gwtCache = project.file(workingDir.canonicalPath + "/gwt-unitCache")
                    wwwTest.deleteDir()
                    gwtCache.deleteDir()
                }
            }
        }

        // Run the acceptance tests, including a build of the application
        project.task("acceptancetest", dependsOn: [ project.tasks.buildApplication, project.tasks.verifyCucumber ]) << {
            project.convention.plugins.cedarGwtOnGae.killDevmode()
            project.convention.plugins.cedarGwtOnGae.bootDevmode()
            project.convention.plugins.cedarGwtOnGae.waitForDevmode()
            def result = project.convention.plugins.cedarCucumber.execCucumber(null, null)
            project.convention.plugins.cedarGwtOnGae.killDevmode()
            result.assertNormalExitValue()
        }

        // Effectively disable the standard test runner by making it look for a bogus class.
        project.tasks.test.include("**/bogus.class")

        // Redefine the test runner in terms of the unit and client test suites.
        project.tasks.test.dependsOn(project.tasks.clienttest, project.tasks.unittest)

        // Define the order of tests if there are multiple called at the same time
        project.tasks.clienttest.mustRunAfter project.tasks.unittest
        project.tasks.acceptancetest.mustRunAfter project.tasks.clienttest

        // Define a task that runs all of the tests
        project.task("alltest", dependsOn: [ project.tasks.unittest, project.tasks.clienttest, project.tasks.acceptancetest ]);
    }

}

