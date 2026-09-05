${fmlHeader}license = "MIT"
issueTrackerURL = "https://github.com/GOD-GAMER/SCGuardGolem/issues"

[[mods]]
modId = "scguardgolem"
version = "${modVersion}"
displayName = "SecurityCraft Guard Golem"
description = '''
A SecurityCraft addon that adds a configurable Security Guard Golem with patrol routes, SecurityCraft module upgrades, a tabbed config GUI, loot collection, and bell recall.
'''
authors = "SCGuardGolem Team"

[[dependencies.scguardgolem]]
modId = "${loaderDepId}"
${requiredAttr}
versionRange = "${loaderRange}"
ordering = "NONE"
side = "BOTH"

[[dependencies.scguardgolem]]
modId = "minecraft"
${requiredAttr}
versionRange = "${mcRange}"
ordering = "NONE"
side = "BOTH"

[[dependencies.scguardgolem]]
modId = "securitycraft"
${requiredAttr}
versionRange = "${scRange}"
ordering = "AFTER"
side = "BOTH"
