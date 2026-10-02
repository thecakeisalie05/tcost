package io.github.thecakeisalie05.tcost;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import io.github.thecakeisalie05.tcost.config.TCostConfig;

@Mod(
    modid = TCostMod.MOD_ID,
    name = TCostMod.MOD_NAME,
    version = Tags.VERSION,
    acceptableRemoteVersions = "*")
public final class TCostMod {

    public static final String MOD_ID = "tcost";
    public static final String MOD_NAME = "TCost";

    @Mod.Instance(MOD_ID)
    public static TCostMod INSTANCE;

    private TCostConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = TCostConfig.load(event.getSuggestedConfigurationFile());
    }

    public TCostConfig config() {
        return config;
    }
}
