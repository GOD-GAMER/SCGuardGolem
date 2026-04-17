plugins {
    id("net.neoforged.gradle.userdev") version "7.0.163"
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

tasks.processResources {
    exclude(".cache")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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

runs {
    configureEach {
        modSource(sourceSets.main.get())
    }

    create("client") {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")
    }

    create("server") {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")
        programArgument("--nogui")
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    implementation("net.neoforged:forge:${forgeVersion}")
    compileOnly(fileTree("libs") { include("*.jar") })
}

// SC jar uses SRG names in mixins, incompatible with MCP dev env
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
    mainFile.addModLoader("NeoForge")
    mainFile.addGameVersion(mcVersion)
    mainFile.changelog = "See https://github.com/GOD-GAMER/SCGuardGolem/blob/mc/${mcVersion}/CHANGELOG.md"
    mainFile.changelogType = "markdown"
    mainFile.addRequirement("security-craft")
}
