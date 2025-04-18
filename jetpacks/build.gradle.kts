plugins {
    id("addons.common-conventions")
}

version = "0.1.14"

addon {
    name = "Jetpacks"
    main = "xyz.xenondevs.nova.addon.jetpacks.Jetpacks"
    version = project.version.toString()
    authors = listOf("StudioCode")
}

pluginPublish {
    hangar("Jetpacks") { 
        gameVersions(libs.versions.minecraft.get())
        requiredDependency("Nova")
    }
}