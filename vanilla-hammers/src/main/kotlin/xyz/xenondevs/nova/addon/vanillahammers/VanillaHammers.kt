package xyz.xenondevs.nova.addon.vanillahammers

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.update.ProjectDistributor

object VanillaHammers : Addon() {
    
    override val projectDistributors = listOf(
        ProjectDistributor.modrinth("nova-vanilla-hammers"),
        ProjectDistributor.hangar("xenondevs/Vanilla-Hammers")
    )
    
}