package com.adibarra.enchanttweaker;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class EnchantTweakerPreLaunchEntry implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		MixinExtrasBootstrap.init();
	}
}
