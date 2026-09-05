import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.jvm.tasks.ProcessResources

// Node build script for the single FORGE target (1.20.1), using ModDevGradle
// Legacy. Kept intentionally parallel to build.gradle.kts; shared logic lives
// in buildSrc (ScgMatrix).
plugins {
    id("net.neoforged.moddev.legacyforge") // version pinned in stonecutter.gradle.kts
    id("neoforge-mutex")
}

val target: String = project.name

@Suppress("UNCHECKED_CAST")
val matrixTargets = (gradle as ExtensionAware).extensions.extraProperties
    .get("scg.matrix.targets") as Map<String, Map<String, String>>

@Suppress("UNCHECKED_CAST")
val matrixShared = (gradle as ExtensionAware).extensions.extraProperties
    .get("scg.matrix.shared") as Map<String, String>

val m = matrixTargets.getValue(target)
val modVersion = providers.gradleProperty("modVersion").get()

version = modVersion

java {
    toolchain.languageVersion = JavaLanguageVersion.of(m.getValue("java").toInt())
}

legacyForge {
    // "<mcVersion>-<forgeVersion>"
    version = "$target-${m.getValue("loader_min")}"

    runs {
        register("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", "scguardgolem")
        }
        register("server") {
            server()
            programArgument("-nogui")
            systemProperty("forge.enabledGameTestNamespaces", "scguardgolem")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("forge.enabledGameTestNamespaces", "scguardgolem")
        }
        configureEach {
            gameDirectory = file("run/$name")
            logLevel = org.slf4j.event.Level.INFO
        }
    }

    mods {
        register("scguardgolem") {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    // Forge 1.20.1 release jars are SRG-obfuscated; modImplementation is MDG
    // Legacy's auto-created remapping configuration (the fg.deobf successor).
    "modImplementation"(ScgMatrix.securityCraftCoordinate(matrixShared, m))
}

tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    dependsOn("stonecutterGenerate")
}

tasks.matching { it.name == "runServer" }.configureEach {
    if (this is JavaExec) standardInput = System.`in`
}

tasks.named<Jar>("jar") {
    archiveBaseName = "SecurityGolemAddon-$target-SC${m.getValue("sc_version")}"
    manifest {
        attributes(
            "Specification-Title" to "SCGuardGolem",
            "Specification-Version" to modVersion,
            "Implementation-Title" to "SCGuardGolem",
            "Implementation-Version" to modVersion,
        )
    }
}

val metaTokens = ScgMatrix.metadataTokens(m, modVersion)

tasks.named<ProcessResources>("processResources") {
    inputs.properties(metaTokens)
    from(rootProject.file("templates/mods.toml.tpl")) {
        into("META-INF")
        rename { m.getValue("metadata_file").substringAfterLast('/') }
        expand(metaTokens)
    }
    val packFormat = m["pack_format"]
    if (!packFormat.isNullOrBlank()) {
        inputs.property("packFormat", packFormat)
        from(rootProject.file("templates/pack.mcmeta.tpl")) {
            rename { "pack.mcmeta" }
            expand(mapOf("packFormat" to packFormat))
        }
    }
}
