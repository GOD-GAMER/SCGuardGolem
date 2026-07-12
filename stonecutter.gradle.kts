plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.141" apply false
}

stonecutter active "26.1"

// Shared per-node preprocessing parameters.
stonecutter parameters {
    // MC 1.21.11 renamed ResourceLocation -> Identifier (official mappings).
    // The replacement runs across the whole tree in both directions on switch,
    // so the source never needs comment guards for the pure class-name rename.
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
    // `//? if forge` guards the Forge-1.20.1-only code paths.
    constants["forge"] = node.metadata.version == "1.20.1"
}

// Aggregate helpers over every registered version node.
tasks.register("buildAll") {
    group = "project"
    description = "Builds every target in versions.matrix.toml"
    dependsOn(stonecutter.tasks.named("build"))
}
