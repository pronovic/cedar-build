// vim: set ft=groovy ts=4 sw=4:
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// *              C E D A R
// *          S O L U T I O N S       "Software done right."
// *           S O F T W A R E
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// * Copyright (c) 2013,2015 Kenneth J. Pronovici.
// * All rights reserved.
// *
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the Apache License, Version 2.0.
// * See LICENSE for more information about the licensing terms.
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// * Author   : Kenneth J. Pronovici <pronovic@ieee.org>
// * Language : Gradle (>= 2.5)
// * Project  : Common Gradle Build Functionality
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package com.cedarsolutions.gradle

import org.gradle.api.Project
import org.gradle.plugins.signing.Sign
import org.gradle.api.InvalidUserDataException

/** 
 * Plugin convention for cedarSigning. 
 * @author Kenneth J. Pronovici <pronovic@ieee.org>
 */
class CedarSigningPluginConvention {

    /** The project tied to this convention. */
    private Project project;

    /** Create a convention tied to a project. */
    public CedarSigningPluginConvention(Project project) {
        this.project = project
    }

    /** Apply signature configuration to all projects if necessary. */
    def applySignatureConfiguration(taskGraph) {
        if (taskGraphRequiresSignatures(taskGraph)) {
            getGpgPassphrase({ input -> setSignatureConfiguration(input) })
        }
    }

    /** Check whether a task graph indicates that signatures are required. */
    def taskGraphRequiresSignatures(taskGraph) {
        return taskGraph.allTasks.any { it instanceof Sign && it.required }
    }

    /** Get a GPG passphrase from the user, calling a closure with the result. */
    def getGpgPassphrase(action) {
        validateGpgConfig()

        String passphrase = project.convention.plugins.cedarKeyValueStore.getCacheValue("gpgPassphrase")
        if (passphrase == null) {
            String title = "GPG key " + project.cedarSigning.getGpgKeyId();
            String label = "Enter passphrase"

            def result = null  // def NOT String, otherwise closure assignment won't work
            def resultaction = { value -> result = value }
            project.convention.plugins.cedarBuild.getInput(title, label, true, resultaction)

            passphrase = result.toString()
            project.convention.plugins.cedarKeyValueStore.setCacheValue("gpgPassphrase", passphrase)
        }

        action(passphrase)
    }

    /** Set signature configuration for all projects. */
    def setSignatureConfiguration(passphrase) {
        project.cedarSigning.getProjects().each { project ->
            project.ext."signing.keyId" = project.cedarSigning.getGpgKeyId() 
            project.ext."signing.secretKeyRingFile" = project.cedarSigning.getGpgSecretKey()
            project.ext."signing.password" = passphrase
        }
    }

    /** Validate the GPG configuration. */
    def validateGpgConfig() {
        if (project.cedarSigning.getGpgKeyId() == null || project.cedarSigning.getGpgKeyId() == "unset") {
            throw new InvalidUserDataException("Publish error: gpgKeyId is unset")
        }

        if (project.cedarSigning.getGpgSecretKey() == null || project.cedarSigning.getGpgSecretKey() == "unset") {
            throw new InvalidUserDataException("Publish error: gpgSecretKey is unset")
        }

        if (!(new File(project.cedarSigning.getGpgSecretKey()).isFile())) {
            throw new InvalidUserDataException("Publish error: GPG secret key not found: " + project.cedarSigning.getGpgSecretKey())
        }
    }

}
