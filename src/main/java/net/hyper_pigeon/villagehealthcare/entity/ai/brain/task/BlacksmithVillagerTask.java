package net.hyper_pigeon.villagehealthcare.entity.ai.brain.task;

import net.hyper_pigeon.villagehealthcare.config.ModConfig;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class BlacksmithVillagerTask extends Behavior<Villager> {
    private static final int TARGET_REFRESH_INTERVAL = 10;     // ticks
    private static final double NORMAL_HEAL_RANGE_SQR = 6.0;   // 2.4 blocks
    private static final int STUCK_TIMEOUT_TICKS = 60;         // 3 seconds
    private static final double MIN_PROGRESS = 1.0;            // blocks moved closer

    private long nextResponseTime;
    private int ticksSinceLastRefresh;
    private IronGolem target;
    private int stuckTimer;
    private double lastDistance;
    private boolean useLongRangeHeal;

    public BlacksmithVillagerTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT
        ), Integer.MAX_VALUE);
    }

    // ---------- Config getters ----------
    private boolean isHealingEnabled() {
        return ModConfig.INSTANCE.toggleBlacksmithHealing;
    }

    private double getDetectionRadius() {
        return ModConfig.INSTANCE.blacksmithDetectionRadius;
    }

    private float getHealthThreshold() {
        return ModConfig.INSTANCE.blacksmithHealThreshold;
    }

    private float getUrgentHealthThreshold() {
        return ModConfig.INSTANCE.blacksmithUrgentHealthThreshold;
    }

    private float getHealAmount() {
        return ModConfig.INSTANCE.blacksmithHealAmount;
    }

    private double getMaxDistanceSqr() {
        double r = getDetectionRadius();
        return r * r;
    }

    private double getLongRangeHealSqr() {
        double r = getDetectionRadius();
        return r * r;
    }

    // ---------- Helper methods ----------
    private float getWalkSpeed(IronGolem golem) {
        float percent = golem.getHealth() / golem.getMaxHealth();
        if (percent <= 0.3f) return 1.0f;
        if (percent <= 0.6f) return 0.8f;
        return 0.6f;
    }

    private boolean isAlreadyClaimed(Villager self, IronGolem golem) {
        double radius = getDetectionRadius();
        AABB box = self.getBoundingBox().inflate(radius);
        return self.level().getEntitiesOfClass(Villager.class, box, v -> v != self)
                .stream()
                .anyMatch(v -> v.getBrain()
                        .getMemory(MemoryModuleType.INTERACTION_TARGET)
                        .filter(t -> t == golem)
                        .isPresent());
    }

    // ---------- Task lifecycle ----------
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager blacksmith) {
        if (!isHealingEnabled()) return false;
        ResourceKey<VillagerProfession> prof = blacksmith.getVillagerData().profession().unwrapKey().orElse(null);
        if (!blacksmith.isAlive() ||
            !(prof == VillagerProfession.ARMORER ||
              prof == VillagerProfession.TOOLSMITH ||
              prof == VillagerProfession.WEAPONSMITH)) {
            return false;
        }
        Optional<LivingEntity> injured = getNearestUnclaimedInjuredIronGolem(blacksmith, getHealthThreshold());
        if (injured.isPresent()) {
            this.target = (IronGolem) injured.get();
            return true;
        }
        return false;
    }

    @Override
    protected void start(ServerLevel level, Villager blacksmith, long gameTime) {
        // Double-check that the target is still unclaimed before committing
        if (target == null || isAlreadyClaimed(blacksmith, target)) {
            this.target = null;
            return;
        }
        blacksmith.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        blacksmith.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        blacksmith.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);
        blacksmith.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        BehaviorUtils.lookAtEntity(blacksmith, target);
        this.ticksSinceLastRefresh = 0;
        this.nextResponseTime = 0;
        this.stuckTimer = 0;
        this.lastDistance = blacksmith.distanceTo(target);
        this.useLongRangeHeal = false;
    }

    @Override
    protected void tick(ServerLevel level, Villager blacksmith, long gameTime) {
        blacksmith.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        blacksmith.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (++this.ticksSinceLastRefresh >= TARGET_REFRESH_INTERVAL) {
            this.ticksSinceLastRefresh = 0;
            refreshTargetSticky(blacksmith);
        }

        if (target == null) return;

        double currentDist = blacksmith.distanceTo(target);

        // Stuck detection: only if we are not already in long-range mode
        if (!useLongRangeHeal) {
            if (currentDist >= lastDistance - MIN_PROGRESS) {
                stuckTimer++;
            } else {
                stuckTimer = 0;
                lastDistance = currentDist;
            }
            // After 3 seconds stuck and not in normal heal range, enable long-range healing
            if (stuckTimer >= STUCK_TIMEOUT_TICKS && currentDist > Math.sqrt(NORMAL_HEAL_RANGE_SQR)) {
                useLongRangeHeal = true;
            }
        }

        // Navigation: only move if we are not using long-range heal
        if (!useLongRangeHeal) {
            blacksmith.getNavigation().moveTo(target, getWalkSpeed(target));
        } else {
            // When long-range healing, stop moving to avoid endless pathfinding attempts
            blacksmith.getNavigation().stop();
        }

        // Keep look and interaction memories alive
        blacksmith.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        blacksmith.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);

        // Determine healing range (dynamic)
        double healRangeSqr = useLongRangeHeal ? getLongRangeHealSqr() : NORMAL_HEAL_RANGE_SQR;
        double distSqr = currentDist * currentDist;

        if (distSqr <= healRangeSqr && gameTime >= this.nextResponseTime) {
            this.nextResponseTime = gameTime + 15L;
            target.heal(getHealAmount());
            float pitch = 1.0F + (target.getRandom().nextFloat() - target.getRandom().nextFloat()) * 0.2F;
            produceParticles(target);
            target.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
            // Reset stuck timer on successful heal
            stuckTimer = 0;
            lastDistance = currentDist;
        }
    }

    private void refreshTargetSticky(Villager blacksmith) {
        if (target != null && target.isAlive()) {
            // Continue with current target as long as they aren't fully healed and are in range
            boolean stillValid = target.getHealth() < target.getMaxHealth() &&
                    blacksmith.distanceToSqr(target) < getMaxDistanceSqr();
            if (stillValid) {
                // Only switch if there's a critically urgent unclaimed target AND current target is doing okay
                Optional<LivingEntity> urgent = getNearestUnclaimedInjuredIronGolem(blacksmith, getUrgentHealthThreshold());
                if (urgent.isPresent() && urgent.get() != target
                        && target.getHealth() > target.getMaxHealth() * 0.75f) {
                    this.target = (IronGolem) urgent.get();
                    this.nextResponseTime = 0;
                    this.stuckTimer = 0;
                    this.lastDistance = blacksmith.distanceTo(target);
                    this.useLongRangeHeal = false;
                }
                return;
            }
        }
        // Current target is gone, dead, or fully healed — find a new unclaimed one
        Optional<LivingEntity> best = getNearestUnclaimedInjuredIronGolem(blacksmith, getHealthThreshold());
        if (best.isPresent()) {
            this.target = (IronGolem) best.get();
            this.nextResponseTime = 0;
            this.stuckTimer = 0;
            this.lastDistance = blacksmith.distanceTo(target);
            this.useLongRangeHeal = false;
        } else {
            this.target = null;
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager blacksmith, long gameTime) {
        super.stop(level, blacksmith, gameTime);
        blacksmith.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        blacksmith.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        blacksmith.getNavigation().stop();
        holdNothing(blacksmith);
        this.nextResponseTime = gameTime + 40L;
        this.target = null;
        this.ticksSinceLastRefresh = 0;
        this.stuckTimer = 0;
        this.useLongRangeHeal = false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager blacksmith, long gameTime) {
        if (target == null || !target.isAlive()) return false;
        return blacksmith.distanceToSqr(target) < getMaxDistanceSqr() && target.getHealth() < target.getMaxHealth();
    }

    // ---------- Target search helpers ----------
    private Optional<LivingEntity> getNearestUnclaimedInjuredIronGolem(Villager blacksmith, float threshold) {
        double radius = getDetectionRadius();
        AABB box = blacksmith.getBoundingBox().inflate(radius);
        List<IronGolem> golems = blacksmith.level().getEntitiesOfClass(IronGolem.class, box,
                g -> g.isAlive() && g.getHealth() <= g.getMaxHealth() * threshold
                        && !isAlreadyClaimed(blacksmith, g));
        return golems.stream()
                .min((a, b) -> Double.compare(blacksmith.distanceToSqr(a), blacksmith.distanceToSqr(b)))
                .map(LivingEntity.class::cast);
    }

    private static void holdNothing(Villager v) {
        v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        v.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    protected void produceParticles(IronGolem golem) {
        for (int i = 0; i < 5; ++i) {
            double dx = golem.getRandom().nextGaussian() * 0.02;
            double dy = golem.getRandom().nextGaussian() * 0.02;
            double dz = golem.getRandom().nextGaussian() * 0.02;
            if (!golem.level().isClientSide()) {
                ((ServerLevel) golem.level()).sendParticles(ParticleTypes.SCRAPE,
                        golem.getRandomX(1), golem.getRandomY() + 0.5, golem.getRandomZ(1),
                        0, dx, dy, dz, 0.0);
            }
        }
    }
}