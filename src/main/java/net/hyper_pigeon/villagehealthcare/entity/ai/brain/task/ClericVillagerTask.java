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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ClericVillagerTask extends Behavior<Villager> {
    private static final int TARGET_REFRESH_INTERVAL = 10;     // ticks
    private static final double NORMAL_HEAL_RANGE_SQR = 6.0;   // 2.4 blocks
    private static final int STUCK_TIMEOUT_TICKS = 60;         // 3 seconds
    private static final double MIN_PROGRESS = 1.0;            // blocks moved closer

    private long nextResponseTime;
    private int ticksSinceLastRefresh;
    private LivingEntity target;
    private int stuckTimer;
    private double lastDistance;
    private boolean useLongRangeHeal;

    public ClericVillagerTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT
        ), Integer.MAX_VALUE);
    }

    // ---------- Config getters ----------
    private boolean isHealingEnabled() {
        return ModConfig.INSTANCE.toggleClericHealing;
    }

    private boolean isSelfHealingEnabled() {
        return ModConfig.INSTANCE.toggleClericSelfHealing;
    }

    private double getDetectionRadius() {
        return ModConfig.INSTANCE.clericDetectionRadius;
    }

    private float getHealthThreshold() {
        return ModConfig.INSTANCE.clericHealThreshold;
    }

    private float getUrgentHealthThreshold() {
        return ModConfig.INSTANCE.clericUrgentHealthThreshold;
    }

    private float getHealAmount() {
        return ModConfig.INSTANCE.clericHealAmount;
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
    private float getWalkSpeed(LivingEntity entity) {
        float percent = entity.getHealth() / entity.getMaxHealth();
        if (percent <= 0.3f) return 1.0f;
        if (percent <= 0.6f) return 0.8f;
        return 0.6f;
    }

    private boolean isAlreadyClaimed(Villager self, LivingEntity entity) {
        double radius = getDetectionRadius();
        AABB box = self.getBoundingBox().inflate(radius);
        return self.level().getEntitiesOfClass(Villager.class, box, v -> v != self)
                .stream()
                .anyMatch(v -> v.getBrain()
                        .getMemory(MemoryModuleType.INTERACTION_TARGET)
                        .filter(t -> t == entity)
                        .isPresent());
    }

    /**
     * Priority order (all config values live):
     * 1. Emergency self-heal (health < urgent) – only if self-healing allowed.
     * 2. Urgent other (≤ urgent) – heal them if cleric healthy enough or self-healing disabled.
     * 3. Non‑urgent other (≤ threshold) – same rule.
     * 4. Self-heal if below threshold and no others need help – only if allowed.
     */
    private Optional<LivingEntity> selectTarget(Villager cleric) {
        float selfHealthPct = cleric.getHealth() / cleric.getMaxHealth();
        boolean allowSelf = isSelfHealingEnabled();
        float urgentThr = getUrgentHealthThreshold();
        float normalThr = getHealthThreshold();

        // 1. Emergency self‑heal
        if (allowSelf && selfHealthPct < urgentThr) {
            return Optional.of(cleric);
        }

        // 2. Urgent other
        Optional<LivingEntity> urgentOther = getNearestUnclaimedInjuredVillager(cleric, urgentThr);
        if (urgentOther.isPresent()) {
            if (!allowSelf || selfHealthPct >= normalThr) {
                return urgentOther;
            } else {
                return Optional.of(cleric);
            }
        }

        // 3. Non‑urgent other
        Optional<LivingEntity> injuredOther = getNearestUnclaimedInjuredVillager(cleric, normalThr);
        if (injuredOther.isPresent()) {
            if (!allowSelf || selfHealthPct >= normalThr) {
                return injuredOther;
            } else {
                return Optional.of(cleric);
            }
        }

        // 4. Self‑heal (only if below normal threshold)
        if (allowSelf && selfHealthPct < normalThr) {
            return Optional.of(cleric);
        }

        return Optional.empty();
    }

    // ---------- Task lifecycle ----------
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager cleric) {
        if (!isHealingEnabled()) return false;
        ResourceKey<VillagerProfession> prof = cleric.getVillagerData().profession().unwrapKey().orElse(null);
        if (!cleric.isAlive() || prof != VillagerProfession.CLERIC) return false;

        Optional<LivingEntity> newTarget = selectTarget(cleric);
        if (newTarget.isPresent()) {
            this.target = newTarget.get();
            return true;
        }
        return false;
    }

    @Override
    protected void start(ServerLevel level, Villager cleric, long gameTime) {
        // Claim check for other targets (not self)
        if (target != cleric && isAlreadyClaimed(cleric, target)) {
            this.target = null;
            return;
        }
        cleric.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, Potions.HEALING));
        cleric.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (target != cleric) {
            cleric.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);
            cleric.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
            BehaviorUtils.lookAtEntity(cleric, target);
        }
        this.ticksSinceLastRefresh = 0;
        this.nextResponseTime = 0;
        this.stuckTimer = 0;
        if (target != cleric) {
            this.lastDistance = cleric.distanceTo(target);
        }
        this.useLongRangeHeal = false;
    }

    @Override
    protected void tick(ServerLevel level, Villager cleric, long gameTime) {
        cleric.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, Potions.HEALING));
        cleric.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (++this.ticksSinceLastRefresh >= TARGET_REFRESH_INTERVAL) {
            this.ticksSinceLastRefresh = 0;
            refreshTargetSticky(cleric);
        }

        if (target == null) return;

        // Self‑healing branch
        if (target == cleric) {
            if (isSelfHealingEnabled() && cleric.getHealth() < cleric.getMaxHealth() && gameTime >= this.nextResponseTime) {
                this.nextResponseTime = gameTime + 15L;
                cleric.heal(getHealAmount());
                produceParticles(cleric);
                cleric.playSound(SoundEvents.WANDERING_TRADER_DRINK_POTION, 0.8F, 1.0F);
            }
            return;
        }

        // Healing others
        double currentDist = cleric.distanceTo(target);

        // Stuck detection (only when not already in long‑range mode)
        if (!useLongRangeHeal) {
            if (currentDist >= lastDistance - MIN_PROGRESS) {
                stuckTimer++;
            } else {
                stuckTimer = 0;
                lastDistance = currentDist;
            }
            if (stuckTimer >= STUCK_TIMEOUT_TICKS && currentDist > Math.sqrt(NORMAL_HEAL_RANGE_SQR)) {
                useLongRangeHeal = true;
            }
        }

        // Navigation
        if (!useLongRangeHeal) {
            cleric.getNavigation().moveTo(target, getWalkSpeed(target));
        } else {
            cleric.getNavigation().stop();
        }

        // Keep memories alive
        cleric.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        cleric.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);

        double healRangeSqr = useLongRangeHeal ? getLongRangeHealSqr() : NORMAL_HEAL_RANGE_SQR;
        double distSqr = currentDist * currentDist;

        if (distSqr <= healRangeSqr && gameTime >= this.nextResponseTime) {
            this.nextResponseTime = gameTime + 15L;
            target.heal(getHealAmount());
            produceParticles(target);
            target.playSound(SoundEvents.WANDERING_TRADER_DRINK_POTION, 0.8F, 1.0F);
            stuckTimer = 0;
            lastDistance = currentDist;
        }
    }

    private void refreshTargetSticky(Villager cleric) {
        boolean allowSelf = isSelfHealingEnabled();
        float urgentThr = getUrgentHealthThreshold();
        float normalThr = getHealthThreshold();

        // Emergency self‑heal override (only if allowed)
        if (allowSelf && cleric.getHealth() < cleric.getMaxHealth() * urgentThr) {
            this.target = cleric;
            this.nextResponseTime = 0;
            this.stuckTimer = 0;
            this.useLongRangeHeal = false;
            return;
        }

        // Currently self‑healing – switch away only if healthy enough and an urgent other exists
        if (target == cleric) {
            if (allowSelf && cleric.getHealth() / cleric.getMaxHealth() >= normalThr) {
                Optional<LivingEntity> urgentOther = getNearestUnclaimedInjuredVillager(cleric, urgentThr);
                if (urgentOther.isPresent()) {
                    this.target = urgentOther.get();
                    this.nextResponseTime = 0;
                    this.stuckTimer = 0;
                    this.lastDistance = cleric.distanceTo(target);
                    this.useLongRangeHeal = false;
                }
            }
            return;
        }

        // Healing another villager – continue if still valid, else pick new target
        if (target != null && target.isAlive()) {
            boolean stillValid = target.getHealth() < target.getMaxHealth()
                    && cleric.distanceToSqr(target) < getMaxDistanceSqr();
            if (stillValid) {
                // Switch only if a more urgent unclaimed target appears and current patient is doing well (>75%)
                Optional<LivingEntity> urgent = getNearestUnclaimedInjuredVillager(cleric, urgentThr);
                if (urgent.isPresent() && urgent.get() != target
                        && target.getHealth() > target.getMaxHealth() * 0.75f) {
                    this.target = urgent.get();
                    this.nextResponseTime = 0;
                    this.stuckTimer = 0;
                    this.lastDistance = cleric.distanceTo(target);
                    this.useLongRangeHeal = false;
                }
                return;
            }
        }

        // Fallback – choose new target using live config
        Optional<LivingEntity> newTarget = selectTarget(cleric);
        if (newTarget.isPresent()) {
            this.target = newTarget.get();
            this.nextResponseTime = 0;
            this.stuckTimer = 0;
            if (target != cleric) {
                this.lastDistance = cleric.distanceTo(target);
            }
            this.useLongRangeHeal = false;
        } else {
            this.target = null;
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager cleric, long gameTime) {
        super.stop(level, cleric, gameTime);
        cleric.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        cleric.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        cleric.getNavigation().stop();
        holdNothing(cleric);
        this.nextResponseTime = gameTime + 40L;
        this.target = null;
        this.ticksSinceLastRefresh = 0;
        this.stuckTimer = 0;
        this.useLongRangeHeal = false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager cleric, long gameTime) {
        if (target == null || !target.isAlive()) return false;
        if (target == cleric) return cleric.getHealth() < cleric.getMaxHealth();
        return cleric.distanceToSqr(target) < getMaxDistanceSqr() && target.getHealth() < target.getMaxHealth();
    }

    // ---------- Target search helpers ----------
    private Optional<LivingEntity> getNearestUnclaimedInjuredVillager(Villager cleric, float threshold) {
        double radius = getDetectionRadius();
        AABB box = cleric.getBoundingBox().inflate(radius);
        List<Villager> list = cleric.level().getEntitiesOfClass(Villager.class, box,
                v -> v.isAlive()
                        && v != cleric
                        && v.getHealth() <= v.getMaxHealth() * threshold
                        && !isAlreadyClaimed(cleric, v));
        return list.stream()
                .min(Comparator.comparingDouble(cleric::distanceToSqr))
                .map(LivingEntity.class::cast);
    }

    private static void holdNothing(Villager v) {
        v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        v.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    protected void produceParticles(LivingEntity target) {
        for (int i = 0; i < 5; ++i) {
            double dx = target.getRandom().nextGaussian() * 0.02;
            double dy = target.getRandom().nextGaussian() * 0.02;
            double dz = target.getRandom().nextGaussian() * 0.02;
            if (!target.level().isClientSide()) {
                ((ServerLevel) target.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getRandomX(1), target.getRandomY() + 1.0, target.getRandomZ(1),
                        0, dx, dy, dz, 0.0);
            }
        }
    }
}