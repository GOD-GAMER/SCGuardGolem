/**
 * MDG recreates/decompiles Minecraft artifacts per subproject. With
 * org.gradle.parallel=true and eight versioned subprojects, several NeoForm
 * runs would otherwise execute at once and exhaust the machine. This shared
 * build service serializes the heavy `createMinecraftArtifacts` task across
 * all subprojects (same pattern as the official Stonecutter NeoForge template).
 */
abstract class CreateMinecraftArtifactsMutex : BuildService<BuildServiceParameters.None>

val mutex = gradle.sharedServices.registerIfAbsent(
    "createMinecraftArtifactsMutex",
    CreateMinecraftArtifactsMutex::class
) {
    maxParallelUsages.set(1)
}

tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    usesService(mutex)
}
