plugins {
    id("net.neoforged.moddev") version "2.0.76"
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

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

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
