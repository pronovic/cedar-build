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
import org.gradle.api.Plugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.plugins.signing.SigningPlugin

/** 
 * The cedarPublish plugin. 
 * @author Kenneth J. Pronovici <pronovic@ieee.org>
 */
class CedarPublishPlugin implements Plugin<Project> {

    /** Apply the plugin. */
    void apply(Project project) {
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        project.extensions.create("cedarPublish", CedarPublishPluginExtension, project)
        project.convention.plugins.cedarPublish = new CedarPublishPluginConvention(project)

        project.task("validatePublishSetup") << {
            project.cedarPublish.validateMavenRepositoryConfig()
        }

        project.task("publish", dependsOn: [ project.tasks.validatePublishSetup, project.tasks.uploadArchives, ])
        project.tasks.uploadArchives.mustRunAfter project.tasks.validatePublishSetup
    }

}
