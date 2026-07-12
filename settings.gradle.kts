import org.gradle.api.plugins.ExtensionAware

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// ---------------------------------------------------------------------------
// versions.matrix.toml is the single source of truth for every target.
// This small parser handles exactly our constrained schema:
//   [securitycraft]           -> shared key/values
//   [targets."<name>"]        -> one table per build target
// Values are bare ints or double-quoted strings; '#' starts a comment and
// never appears inside a value.
// ---------------------------------------------------------------------------
fun parseMatrix(file: java.io.File): Pair<Map<String, String>, Map<String, Map<String, String>>> {
    val shared = linkedMapOf<String, String>()
    val targets = linkedMapOf<String, MutableMap<String, String>>()
    var currentTarget: MutableMap<String, String>? = null
    var inShared = false
    file.readLines().forEach { raw ->
        val line = raw.substringBefore('#').trim()
        if (line.isEmpty()) return@forEach
        when {
            line.startsWith("[targets.") -> {
                val name = line.removePrefix("[targets.").removeSuffix("]").trim('"')
                currentTarget = linkedMapOf()
                targets[name] = currentTarget!!
                inShared = false
            }
            line.startsWith("[") -> {
                inShared = line == "[securitycraft]"
                currentTarget = null
            }
            line.contains('=') -> {
                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim().trim('"')
                if (currentTarget != null) currentTarget!![key] = value
                else if (inShared) shared[key] = value
            }
        }
    }
    return shared to targets
}

val (scShared, scTargets) = parseMatrix(file("versions.matrix.toml"))
require(scTargets.isNotEmpty()) { "versions.matrix.toml defined no targets" }

// Expose the parsed matrix to every project script (read-only, config time).
(gradle as ExtensionAware).extensions.extraProperties.set("scg.matrix.shared", scShared)
(gradle as ExtensionAware).extensions.extraProperties.set("scg.matrix.targets", scTargets)

stonecutter {
    create(rootProject) {
        scTargets.forEach { (name, cfg) ->
            if (cfg["loader"] == "forge")
                version(name, name).buildscript("build.forge.gradle.kts")
            else
                version(name, name)
        }
        vcsVersion = "26.1"
    }
}

rootProject.name = "SCGuardGolem"
