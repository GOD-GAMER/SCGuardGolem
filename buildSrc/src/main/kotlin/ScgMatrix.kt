/**
 * Helpers over the parsed versions.matrix.toml (settings.gradle.kts parses the
 * file once and exposes it via gradle extra properties; node build scripts pass
 * the per-target map here). Keeping this logic in buildSrc lets the NeoForge and
 * legacy-Forge node scripts share it without duplicating range math.
 */
object ScgMatrix {

    /** `[min,max)` or `[min,)` when maxExclusive is blank. */
    fun range(min: String, maxExclusive: String?): String =
        if (maxExclusive.isNullOrBlank()) "[$min,)" else "[$min,$maxExclusive)"

    /**
     * Tokens stamped into templates/mods.toml.tpl. RULE (versions.matrix.toml):
     * every shipped lower bound is exactly the version compiled against.
     */
    fun metadataTokens(m: Map<String, String>, modVersion: String): Map<String, String> {
        val forge = m["loader"] == "forge"
        val fmlRange = m["fml_loader_range"].orEmpty()
        val header =
            if (fmlRange.isBlank()) ""
            else "modLoader = \"javafml\"\nloaderVersion = \"$fmlRange\"\n"
        return mapOf(
            "fmlHeader" to header,
            "modVersion" to modVersion,
            "loaderDepId" to if (forge) "forge" else "neoforge",
            "requiredAttr" to if (forge) "mandatory = true" else "type = \"required\"",
            "loaderRange" to range(m.getValue("loader_min"), null),
            "mcRange" to range(m.getValue("mc_min"), m["mc_max"]),
            "scRange" to range(m.getValue("sc_min"), m["sc_max"]),
        )
    }

    /** Maven coordinate of the pinned SecurityCraft build (Curse Maven primary). */
    fun securityCraftCoordinate(shared: Map<String, String>, m: Map<String, String>): String {
        val slug = shared.getValue("curse_slug")
        val projectId = shared.getValue("curse_projectId")
        val fileId = m.getValue("sc_curse_fileId")
        return "curse.maven:$slug-$projectId:$fileId"
    }

    /**
     * Lenient dotted-numeric comparison ("26.1" > "1.21.11"); enough for our
     * target names, mirroring Stonecutter's own lenient-semver ordering.
     */
    fun atLeast(version: String, floor: String): Boolean {
        val a = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val b = floor.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return true
    }
}
