import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

class PublishPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maven-publish")
        }
    }
}

/**
 * Requires GH_USERNAME and GH_TOKEN environment variables.
 * @param repoPath - path to the repo on GitHub, e.g. "org/repo", "shaz01/margiela"
 */
fun Project.publishToGithub(repoPath: String) {
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/$repoPath")
                credentials {
                    println("GH_USERNAME: ${System.getenv("GH_USERNAME")}")
                    username = System.getenv("GH_USERNAME")
                    password = System.getenv("GH_TOKEN")
                }
            }
        }
    }
}