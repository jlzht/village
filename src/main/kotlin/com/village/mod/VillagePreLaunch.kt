package com.village.mod



import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import com.llamalad7.mixinextras.MixinExtrasBootstrap

object VillagePreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        MixinExtrasBootstrap.init()
    }
}
