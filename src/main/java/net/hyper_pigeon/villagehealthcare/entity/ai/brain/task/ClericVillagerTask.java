package net.hyper_pigeon.villagehealthcare.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.VillagerProfession;

import java.util.Optional;

public class ClericVillagerTask extends MultiTickTask<VillagerEntity> {
    private static final int MAX_RUN_TIME = 1000;
    public static final float WALK_SPEED = 0.5F;

    private int ticksRan;
    private long nextResponseTime;
    private VillagerEntity target;

    public ClericVillagerTask() {
        super(
                ImmutableMap.of(
                        MemoryModuleType.LOOK_TARGET,
                        MemoryModuleState.REGISTERED,
                        MemoryModuleType.WALK_TARGET,
                        MemoryModuleState.VALUE_ABSENT,
                        MemoryModuleType.VISIBLE_MOBS,
                        MemoryModuleState.VALUE_PRESENT,
                        MemoryModuleType.INTERACTION_TARGET,
                        MemoryModuleState.VALUE_ABSENT
                )
        );
    }

    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        if (villagerEntity.isAlive() && villagerEntity.getVillagerData().getProfession() == VillagerProfession.CLERIC) {
            Optional<LivingEntity> injuredVillagerOptional = getNearestInjuredVillager(villagerEntity);
            if (injuredVillagerOptional.isPresent()) {
                this.target = (VillagerEntity) injuredVillagerOptional.get();
                return true;
            }
        }
        return false;
    }

    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        if(l > this.nextResponseTime && target != null) {
            villagerEntity.equipStack(EquipmentSlot.MAINHAND,  PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.HEALING));
            villagerEntity.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);

            villagerEntity.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, target);
            villagerEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
            villagerEntity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(target, WALK_SPEED, 1));

            LookTargetUtil.lookAt(villagerEntity, target);
        }
    }

    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        super.finishRunning(serverWorld, villagerEntity, l);
        villagerEntity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
        holdNothing(villagerEntity);
        this.ticksRan = 0;
        this.nextResponseTime = l + 40L;
    }

    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        if (villagerEntity.squaredDistanceTo(target) <= 4 && l > this.nextResponseTime) {
            this.nextResponseTime = l + 10L;
            StatusEffectInstance statusEffectInstance = new StatusEffectInstance(StatusEffects.INSTANT_HEALTH,5);
            target.addStatusEffect(statusEffectInstance, villagerEntity);
            target.playSound(SoundEvents.ITEM_HONEY_BOTTLE_DRINK,0.8F,1.0F);
        }

        villagerEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
        villagerEntity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(target, WALK_SPEED, 2));
        ++this.ticksRan;

    }

    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        return this.ticksRan < MAX_RUN_TIME && target != null && target.isAlive() && villagerEntity.squaredDistanceTo(target) < 100 && target.getHealth() < target.getMaxHealth();
    }

    private Optional<LivingEntity> getNearestInjuredVillager(VillagerEntity villager) {
        return ((LivingTargetCache)villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS).orElse(LivingTargetCache.empty()))
                .findFirst(livingEntity -> livingEntity instanceof VillagerEntity && livingEntity.getHealth() < livingEntity.getMaxHealth()*0.7F);
    }

    private static void holdNothing(VillagerEntity villager) {
        villager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }


}