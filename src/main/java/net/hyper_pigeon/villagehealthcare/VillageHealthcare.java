package net.hyper_pigeon.villagehealthcare;

import net.fabricmc.api.ModInitializer;
import net.hyper_pigeon.villagehealthcare.config.ModConfig;

public class VillageHealthcare implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfig.load();
    }
}
