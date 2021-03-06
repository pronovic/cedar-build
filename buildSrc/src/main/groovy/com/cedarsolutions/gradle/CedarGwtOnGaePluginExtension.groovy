// vim: set ft=groovy ts=4 sw=4:
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// *              C E D A R
// *          S O L U T I O N S       "Software done right."
// *           S O F T W A R E
// *
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// *
// * Copyright (c) 2013-2015 Kenneth J. Pronovici.
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
import org.gradle.api.InvalidUserDataException
import java.io.File
import java.util.concurrent.Callable
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * Plugin extension for cedarGwtOnGae.
 * @author Kenneth J. Pronovici <pronovic@ieee.org>
 */
class CedarGwtOnGaePluginExtension {

    /** Project tied to this extension. */
    private Project project;

    /** Create an extension for a project. */
    public CedarGwtOnGaePluginExtension(Project project) {
        this.project = project;
    }

    /** Module name for the application, taken from the main .gwt.xml file. */
    def appModuleName

    /** Fully-qualified name of the GWT application to be executed. */
    def appEntryPoint

    /** Startup URL for the application, like "MySite.html". */
    def appStartupUrl

    /** Amount of memory to give the GWT compiler, like "256M". */
    def gwtCompilerMemory

    /** Amount of memory to give the unit tests, like "512M". */
    def unittestMemory

    /** Amount of permgen space to give the unit tests, like "128M". */
    def unittestPermgen

    /** Amount of memory to give the client tests, like "512M". */
    def clienttestMemory

    /** Amount of permgen space to give the client tests, like "128M". */
    def clienttestPermgen

    /** Port to be used for HTTP traffic */
    def devmodeServerPort

    /** Port to be used for the GWT codeserver. */
    def devmodeCodeserverPort

    /** Amount of memory to give the devmode server, like "512M". */
    def devmodeServerMemory

    /** Amount of permgen space to give the devmode server, like "128M". */
    def devmodeServerPermgen

    /** Expected boot time for the server, in seconds. */
    def serverWait

    /** Expected stop time for the server, in seconds. */
    def stopWait

    /** Whether the project uses a GWT version of 2.7.0-rc1 or newer. */
    def postGwt27
   
    /** The version of Google App Engine. */
    def appEngineVersion

    /** Location of the xvfb-run script used for headless test runs on UNIX systems. */
    def xvfbRunPath

    /** Whether to spawn GWT processes that are started, normally always true. */
    def spawnProcesses=true

    /** Get appModuleName, accounting for closures. */
    String getAppModuleName() {
        return appModuleName != null && appModuleName instanceof Callable ? appModuleName.call() : appModuleName
    }

    /** Get appEntryPoint, accounting for closures. */
    String getAppEntryPoint() {
        return appEntryPoint != null && appEntryPoint instanceof Callable ? appEntryPoint.call() : appEntryPoint
    }

    /** Get appStartupUrl, accounting for closures. */
    String getAppStartupUrl() {
        return appStartupUrl != null && appStartupUrl instanceof Callable ? appStartupUrl.call() : appStartupUrl
    }

    /** Get gwtCompilerMemory, accounting for closures. */
    String getGwtCompilerMemory() {
        return gwtCompilerMemory != null && gwtCompilerMemory instanceof Callable ? gwtCompilerMemory.call() : gwtCompilerMemory
    }

    /** Get unittestMemory, accounting for closures. */
    String getUnitTestMemory() {
        return unittestMemory != null && unittestMemory instanceof Callable ? unittestMemory.call() : unittestMemory
    }

    /** Get unittestPermgen, accounting for closures. */
    String getUnitTestPermgen() {
        return unittestPermgen != null && unittestPermgen instanceof Callable ? unittestPermgen.call() : unittestPermgen
    }

    /** Get clienttestMemory, accounting for closures. */
    String getClientTestMemory() {
        return clienttestMemory != null && clienttestMemory instanceof Callable ? clienttestMemory.call() : clienttestMemory
    }

    /** Get clienttestPermgen, accounting for closures. */
    String getClientTestPermgen() {
        return clienttestPermgen != null && clienttestPermgen instanceof Callable ? clienttestPermgen.call() : clienttestPermgen
    }

    /** Get devmodeServerPort, accounting for closures. */
    String getDevmodeServerPort() {
        return devmodeServerPort != null && devmodeServerPort instanceof Callable ? devmodeServerPort.call() : devmodeServerPort
    }

    /** Get devmodeCodeserverPort, accounting for closures. */
    String getDevmodeCodeserverPort() {
        return devmodeCodeserverPort != null && devmodeCodeserverPort instanceof Callable ? devmodeCodeserverPort.call() : devmodeCodeserverPort
    }

    /** Get devmodeServerMemory, accounting for closures. */
    String getDevmodeServerMemory() {
        return devmodeServerMemory != null && devmodeServerMemory instanceof Callable ? devmodeServerMemory.call() : devmodeServerMemory
    }

    /** Get devmodeServerPermgen, accounting for closures. */
    String getDevmodeServerPermgen() {
        return devmodeServerPermgen != null && devmodeServerPermgen instanceof Callable ? devmodeServerPermgen.call() : devmodeServerPermgen
    }

    /** Get serverWait, accounting for closures. */
    Integer getServerWait() {
        try {
           String result = serverWait != null && serverWait instanceof Callable ? serverWait.call() : serverWait
           return result == null ? null : Integer.parseInt(result.trim())
        } catch (NumberFormatException e) {
           throw new NumberFormatException("serverWait is not an integer: " + e.getMessage());
        }
    }

    /** Get stopWait, accounting for closures. */
    Integer getStopWait() {
        try {
           String result = stopWait != null && stopWait instanceof Callable ? stopWait.call() : stopWait
           return result == null ? 5 : Integer.parseInt(result.trim())  // default of 5 for backwards compatibility
        } catch (NumberFormatException e) {
           throw new NumberFormatException("stopWait is not an integer: " + e.getMessage());
        }
    }

    /** Get gwtVersion, accounting for closures. */
    boolean isPostGwt27() {
        def value = postGwt27 != null && postGwt27 instanceof Callable ? postGwt27.call() : postGwt27
        return value == null ? false : value   // default of false for backwards compatibility
    }

    /** Get appEngineVersion, accounting for closures. */
    String getAppEngineVersion() {
        return appEngineVersion != null && appEngineVersion instanceof Callable ? appEngineVersion.call() : appEngineVersion
    }

    /** Get xvfbRunPath, accounting for closures. */
    String getXvfbRunPath() {
        return xvfbRunPath != null && xvfbRunPath instanceof Callable ? xvfbRunPath.call() : xvfbRunPath
    }

    /** Get spawnProcesses, accounting for closures. */
    String getSpawnProcesses() {
        return spawnProcesses != null && spawnProcesses instanceof Callable ? spawnProcesses.call() : spawnProcesses
    }

    /** Whether headless mode is available. */
    boolean getIsHeadlessModeAvailable() {
        return !isWindows() && getXvfbRunPath() != null && getXvfbRunPath().length() != 0;
    }

    /** Validate the GWT configuration. */
    def validateGwtConfig() {
        if (getAppModuleName() == null || getAppModuleName() == "unset") {
            throw new InvalidUserDataException("GWT error: appModuleName is unset")
        }

        if (getAppEntryPoint() == null || getAppEntryPoint() == "unset") {
            throw new InvalidUserDataException("GWT error: appEntryPoint is unset")
        }

        if (getAppStartupUrl() == null || getAppStartupUrl() == "unset") {
            throw new InvalidUserDataException("GWT error: appStartupUrl is unset")
        }

        if (getGwtCompilerMemory() == null || getGwtCompilerMemory() == "unset") {
            throw new InvalidUserDataException("GWT error: gwtCompilerMemory is unset")
        }

        if (getDevmodeServerPort() == null || getDevmodeServerPort() == "unset") {
            throw new InvalidUserDataException("GWT error: devmodeServerPort is unset")
        }

        if (getDevmodeCodeserverPort() == null || getDevmodeCodeserverPort() == "unset") {
            throw new InvalidUserDataException("GWT error: devmodeCodeserverPort is unset")
        }

        if (getDevmodeServerMemory() == null || getDevmodeServerMemory() == "unset") {
            throw new InvalidUserDataException("GWT error: devmodeServerMemory is unset")
        }

        if (getDevmodeServerPermgen() == null || getDevmodeServerPermgen() == "unset") {
            throw new InvalidUserDataException("GWT error: devmodeServerPermgen is unset")
        }

        if (getDevmodeServerMemory() == null || getDevmodeServerMemory() == "unset") {
            throw new InvalidUserDataException("GWT error: devmodeServerMemory is unset")
        }
    }

    private boolean isWindows() {
        return Os.isFamily(Os.FAMILY_WINDOWS);
    }

}
