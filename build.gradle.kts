plugins {
    id("net.minecraftforge.gradle") version "6.0.+"
    id("net.darkhax.curseforgegradle") version "1.1.25"
}

val modId = "scguardgolem"
val mcVersion: String by project
val forgeVersion: String by project
val scVersion: String by project
val modVersion: String by project

base {
    archivesName.set("SecurityGolemAddon-${mcVersion}-SC${scVersion}")
    version = modVersion
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

minecraft {
    mappings("official", mcVersion)

    runs {
        create("client") {
            workingDirectory(project.file("runs/client"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("runs/server"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args("--nogui")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

tasks.processResources {
    exclude(".cache")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    val props = mapOf("version" to modVersion)
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "SCGuardGolem",
            "Specification-Version" to project.version,
            "Implementation-Title" to "SCGuardGolem",
            "Implementation-Version" to project.version
        )
    }
}

repositories {
    maven { url = uri("https://maven.minecraftforge.net/") }
    flatDir {
        dirs("libs")
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${forgeVersion}")
    compileOnly(fileTree("libs") { include("*.jar") })
}

// SC jar has SRG names baked into mixin bytecode — incompatible with dev env
// configurations.named("runtimeClasspath") {
//     extendsFrom(configurations.getByName("compileOnly"))
// }

tasks.register("curseforge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
    dependsOn(tasks.jar)
    disableVersionDetection()
    apiToken = findProperty("curseforgeApiToken") as String?
        ?: System.getenv("CURSEFORGE_TOKEN") ?: ""

    val projectId = (findProperty("curseforgeProjectId") as String?)?.toIntOrNull() ?: 0
    val mainFile = upload(projectId, tasks.jar.get().archiveFile)
    mainFile.releaseType = "release"
    mainFile.addModLoader("Forge")
    mainFile.addGameVersion(mcVersion)
    mainFile.changelog = "See https://github.com/GOD-GAMER/SCGuardGolem/blob/mc/${mcVersion}/CHANGELOG.md"
    mainFile.changelogType = "markdown"
    mainFile.addRequirement("security-craft")
}
