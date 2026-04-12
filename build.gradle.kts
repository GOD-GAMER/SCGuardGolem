plugins {
    id("net.neoforged.moddev") version "2.0.99"
    id("net.darkhax.curseforgegradle") version "1.1.25"
}

val modId = "scguardgolem"
val mcVersion: String by project
val neoforgeVersion: String by project
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

neoForge {
    version = neoforgeVersion

    runs {
        configureEach {
            logLevel = org.slf4j.event.Level.DEBUG
            gameDirectory = file("run/" + name)
        }

        register("client") {
            client()
        }

        register("server") {
            server()
            programArgument("-nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly(fileTree("libs") { include("*.jar") })
}

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
