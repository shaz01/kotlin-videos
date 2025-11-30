import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// Project extension to easily get the libs catalog
val Project.catalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// VersionCatalog extension functions for cleaner access
fun VersionCatalog.pluginId(name: String): String = 
    findPlugin(name).get().get().pluginId


fun VersionCatalog.library(name: String) = 
    findLibrary(name).get()

fun VersionCatalog.bundle(name: String) = 
    findBundle(name).get()

fun VersionCatalog.version(name: String): String = 
    findVersion(name).get().requiredVersion

// Alternative: use these if you prefer optional returns
fun VersionCatalog.libraryOrNull(name: String) = 
    findLibrary(name).orElse(null)

fun VersionCatalog.pluginIdOrNull(name: String): String? = 
    findPlugin(name).orElse(null)?.get()?.pluginId