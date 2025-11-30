package utils

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

fun Project.versionCatalog(name: String) =
    extensions.getByType<VersionCatalogsExtension>().named(name)
