package net.hyper_pigeon.villagehealthcare.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "villagehealthcare.json");
    
    public static ModConfig INSTANCE = new ModConfig();

    // Cleric Options
    public boolean toggleClericHealing = true;
    public boolean toggleClericSelfHealing = true;
    public double clericDetectionRadius = 64.0;
    public float clericHealThreshold = 0.7F;
    public float clericUrgentHealthThreshold = 0.4F;
    public float clericHealAmount = 4F;

    // Blacksmith Options
    public boolean toggleBlacksmithHealing = true;
    public double blacksmithDetectionRadius = 64.0;
    public float blacksmithHealThreshold = 0.7F;
    public float blacksmithUrgentHealthThreshold = 0.4F;
    public float blacksmithHealAmount = 6F;

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}