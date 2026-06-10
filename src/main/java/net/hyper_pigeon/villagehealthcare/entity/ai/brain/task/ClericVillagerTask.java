package net.hyper_pigeon.villagehealthcare.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import java.util.Optional;

public class ClericVillagerTask extends Behavior<Villager> {
    private static final int MAX_RUN_TIME = 1600;
    public static final float WALK_SPEED = 0.5F;

    private int ticksRan;
    private long nextResponseTime;
    private Villager target;

    public ClericVillagerTask() {
        super(
                ImmutableMap.of(
                        MemoryModuleType.LOOK_TARGET,
                        MemoryStatus.REGISTERED,
                        MemoryModuleType.WALK_TARGET,
                        MemoryStatus.VALUE_ABSENT,
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                        MemoryStatus.VALUE_PRESENT,
                        MemoryModuleType.INTERACTION_TARGET,
                        MemoryStatus.VALUE_ABSENT
                )
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel serverWorld, Villager villagerEntity) {
        ResourceKey<VillagerProfession> profession = villagerEntity.getVillagerData().profession().unwrapKey().get();

        if (villagerEntity.isAlive() && profession == VillagerProfession.CLERIC) {
            Optional<LivingEntity> injuredVillagerOptional = getNearestInjuredVillager(villagerEntity);
            if (injuredVillagerOptional.isPresent()) {
                this.target = (Villager) injuredVillagerOptional.get();
                return true;
            }
        }
        return false;
    }

    protected void start(ServerLevel serverWorld, Villager villagerEntity, long l) {
        if(l > this.nextResponseTime && target != null) {
            villagerEntity.setItemSlot(EquipmentSlot.MAINHAND,  PotionContents.createItemStack(Items.POTION, Potions.HEALING));
            villagerEntity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            villagerEntity.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);
            villagerEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
            villagerEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, WALK_SPEED, 1));

            BehaviorUtils.lookAtEntity(villagerEntity, target);
        }
    }

    protected void stop(ServerLevel serverWorld, Villager villagerEntity, long l) {
        super.stop(serverWorld, villagerEntity, l);
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        holdNothing(villagerEntity);
        this.ticksRan = 0;
        this.nextResponseTime = l + 40L;
    }

    protected void tick(ServerLevel serverWorld, Villager villagerEntity, long l) {

        villagerEntity.setItemSlot(EquipmentSlot.MAINHAND,  PotionContents.createItemStack(Items.POTION, Potions.HEALING));
        villagerEntity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (villagerEntity.distanceToSqr(target) <= 6 && l > this.nextResponseTime) {
            this.nextResponseTime = l + 10L;
            target.heal(5F);
            produceParticles(target);
            target.playSound(SoundEvents.WANDERING_TRADER_DRINK_POTION,0.8F,1.0F);
        }

        villagerEntity.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);
        villagerEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        villagerEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, WALK_SPEED, 2));

        ++this.ticksRan;

    }

    protected boolean canStillUse(ServerLevel serverWorld, Villager villagerEntity, long l) {
        return this.ticksRan < MAX_RUN_TIME && target != null && target.isAlive() && villagerEntity.distanceToSqr(target) < 100 && target.getHealth() < target.getMaxHealth();
    }

    private Optional<LivingEntity> getNearestInjuredVillager(Villager villager) {
        return ((NearestVisibleLivingEntities)villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty()))
                .findClosest(livingEntity -> livingEntity instanceof Villager && livingEntity.getHealth() < livingEntity.getMaxHealth()*0.7F);
    }

    private static void holdNothing(Villager villager) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }


    protected void produceParticles(Villager targetVillager) {
        for(int i = 0; i < 5; ++i) {
            double d = targetVillager.getRandom().nextGaussian() * 0.02;
            double e = targetVillager.getRandom().nextGaussian() * 0.02;
            double f = targetVillager.getRandom().nextGaussian() * 0.02;

            if(!targetVillager.level().isClientSide()) {
                ((ServerLevel)targetVillager.level()).sendParticles((ParticleOptions) ParticleTypes.HAPPY_VILLAGER,targetVillager.getRandomX(1), targetVillager.getRandomY() + 1.0, targetVillager.getRandomZ(1), 0,  d, e, f, 0.0);
            }
        }
    }

}
