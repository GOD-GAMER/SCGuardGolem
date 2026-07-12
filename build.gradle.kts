import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.jvm.tasks.ProcessResources

// Central per-node build script for every NEOFORGE target (the Forge 1.20.1
// node uses build.forge.gradle.kts). The node's project name IS the target
// name in versions.matrix.toml; all version data comes from that file.
plugins {
    id("net.neoforged.moddev") // version pinned in stonecutter.gradle.kts
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

neoForge {
    enable {
        version = m.getValue("loader_min")
        // Binary pipeline: skip Vineflower decompile + recompile (OOM-prone with
        // 8 targets; also MDG's default when CI=true). Pass -Pscg.decompile for
        // browsable Minecraft sources in the IDE.
        setDisableRecompilation(!providers.gradleProperty("scg.decompile").isPresent)
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", "scguardgolem")
        }
        register("server") {
            server()
            programArgument("-nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", "scguardgolem")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", "scguardgolem")
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
    // Pinned published SecurityCraft build for this target (see versions.matrix.toml).
    // implementation => on the compile classpath AND loaded as a mod in dev runs.
    implementation(ScgMatrix.securityCraftCoordinate(matrixShared, m))
}

// MDG must decompile/patch against the Stonecutter-generated sources.
tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    dependsOn("stonecutterGenerate")
}

// Allow `echo stop | gradlew :<target>:runServer` for the CI LOAD rung.
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

// Generated metadata: ONE template, tokens computed from the matrix. The
// declared lower bounds are by construction the versions compiled against.
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
