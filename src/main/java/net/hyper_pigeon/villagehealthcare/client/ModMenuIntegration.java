package net.hyper_pigeon.villagehealthcare.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.hyper_pigeon.villagehealthcare.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Village Healthcare Settings"))
                    .setSavingRunnable(ModConfig::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ModConfig config = ModConfig.INSTANCE;

            // --- CLERIC CATEGORY ---
            ConfigCategory cleric = builder.getOrCreateCategory(Component.literal("Cleric Options"));

            cleric.addEntry(entryBuilder.startBooleanToggle(Component.literal("Toggle Cleric Healing"), config.toggleClericHealing)
                    .setDefaultValue(true)
                    .setSaveConsumer(val -> config.toggleClericHealing = val)
                    .setTooltip(Component.literal("If enabled, Clerics will look for and heal injured villagers."))
                    .build());

            cleric.addEntry(entryBuilder.startBooleanToggle(Component.literal("Toggle Cleric Self-Healing"), config.toggleClericSelfHealing)
                    .setDefaultValue(true)
                    .setSaveConsumer(val -> config.toggleClericSelfHealing = val)
                    .setTooltip(Component.literal("Allows Clerics to prioritize healing themselves when injured."))
                    .build());

            cleric.addEntry(entryBuilder.startDoubleField(Component.literal("Cleric Detection Radius"), config.clericDetectionRadius)
                    .setDefaultValue(64.0)
                    .setSaveConsumer(val -> config.clericDetectionRadius = val)
                    .setTooltip(Component.literal("The range (in blocks) within which Clerics look for injured targets."))
                    .build());

            cleric.addEntry(entryBuilder.startFloatField(Component.literal("Heal Threshold"), config.clericHealThreshold)
                    .setDefaultValue(0.7F)
                    .setMin(0.1F).setMax(1.0F)
                    .setSaveConsumer(val -> config.clericHealThreshold = val)
                    .setTooltip(Component.literal("Health percentage (0.1 - 1.0) below which a villager qualifies for normal healing (e.g., 0.7 = 70%)."))
                    .build());

            cleric.addEntry(entryBuilder.startFloatField(Component.literal("Urgent Health Threshold"), config.clericUrgentHealthThreshold)
                    .setDefaultValue(0.4F)
                    .setMin(0.1F).setMax(1.0F)
                    .setSaveConsumer(val -> config.clericUrgentHealthThreshold = val)
                    .setTooltip(Component.literal("Health percentage (0.1 - 1.0) that triggers emergency, high-priority healing behaviors (e.g., 0.4 = 40%)."))
                    .build());

            cleric.addEntry(entryBuilder.startFloatField(Component.literal("Heal Amount"), config.clericHealAmount)
                    .setDefaultValue(4.0F)
                    .setSaveConsumer(val -> config.clericHealAmount = val)
                    .setTooltip(Component.literal("The number of half-hearts restored to the target per heal action."))
                    .build());


            // --- BLACKSMITH CATEGORY ---
            ConfigCategory blacksmith = builder.getOrCreateCategory(Component.literal("Blacksmith Options"));

            blacksmith.addEntry(entryBuilder.startBooleanToggle(Component.literal("Toggle Blacksmith Healing"), config.toggleBlacksmithHealing)
                    .setDefaultValue(true)
                    .setSaveConsumer(val -> config.toggleBlacksmithHealing = val)
                    .setTooltip(Component.literal("If enabled, Blacksmiths will look for and repair damaged Iron Golems."))
                    .build());

            blacksmith.addEntry(entryBuilder.startDoubleField(Component.literal("Blacksmith Detection Radius"), config.blacksmithDetectionRadius)
                    .setDefaultValue(64.0)
                    .setSaveConsumer(val -> config.blacksmithDetectionRadius = val)
                    .setTooltip(Component.literal("The range (in blocks) within which Blacksmiths look for damaged golems."))
                    .build());

            blacksmith.addEntry(entryBuilder.startFloatField(Component.literal("Heal Threshold"), config.blacksmithHealThreshold)
                    .setDefaultValue(0.7F)
                    .setMin(0.1F).setMax(1.0F)
                    .setSaveConsumer(val -> config.blacksmithHealThreshold = val)
                    .setTooltip(Component.literal("Health percentage (0.1 - 1.0) below which a golem qualifies for normal healing (e.g., 0.7 = 70%)."))
                    .build());

            blacksmith.addEntry(entryBuilder.startFloatField(Component.literal("Urgent Health Threshold"), config.blacksmithUrgentHealthThreshold)
                    .setDefaultValue(0.4F)
                    .setMin(0.1F).setMax(1.0F)
                    .setSaveConsumer(val -> config.blacksmithUrgentHealthThreshold = val)
                    .setTooltip(Component.literal("Health percentage (0.1 - 1.0) that triggers emergency, high-priority healing behaviors (e.g., 0.4 = 40%)."))
                    .build());

            blacksmith.addEntry(entryBuilder.startFloatField(Component.literal("Heal Amount"), config.blacksmithHealAmount)
                    .setDefaultValue(6.0F)
                    .setSaveConsumer(val -> config.blacksmithHealAmount = val)
                    .setTooltip(Component.literal("The number of half-hearts restored to the target per heal action."))
                    .build());

            return builder.build();
        };
    }
}
