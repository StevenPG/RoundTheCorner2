package com.stevenpg.roundthecorner2.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Typed access to `gradle/libs.versions.toml` from inside binary plugins.
 * (The generated `libs` accessor only exists in build scripts.)
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("No version '$alias' in libs.versions.toml") }.requiredVersion

internal fun VersionCatalog.library(alias: String) =
    findLibrary(alias).orElseThrow { IllegalStateException("No library '$alias' in libs.versions.toml") }

internal fun VersionCatalog.pluginId(alias: String): String =
    findPlugin(alias).orElseThrow { IllegalStateException("No plugin '$alias' in libs.versions.toml") }.get().pluginId
