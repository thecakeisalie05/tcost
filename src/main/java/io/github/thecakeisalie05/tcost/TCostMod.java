package io.github.thecakeisalie05.tcost;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import dev.cake.rawcost.Common;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import io.github.thecakeisalie05.tcost.config.TCostConfig;

@Mod(
    modid = TCostMod.MOD_ID,
    name = TCostMod.MOD_NAME,
    version = Tags.VERSION,
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:NotEnoughItems;required-after:gregtech")
public final class TCostMod {

    public static final String MOD_ID = "tcost";
    public static final String MOD_NAME = "TCost";

    @Mod.Instance(MOD_ID)
    public static TCostMod INSTANCE;

    @SidedProxy(clientSide = "dev.cake.rawcost.Client", serverSide = "dev.cake.rawcost.Common")
    public static Common proxy;

    private TCostConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = TCostConfig.load(event.getSuggestedConfigurationFile());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.complete();
    }

    public TCostConfig config() {
        return config;
    }
}
