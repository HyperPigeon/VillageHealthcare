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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Optional;

public class BlacksmithVillagerTask extends Behavior<Villager> {
    private static final int MAX_RUN_TIME = 1600;
    public static final float WALK_SPEED = 0.5F;

    private int ticksRan;
    private long nextResponseTime;
    private IronGolem target;

    public BlacksmithVillagerTask() {
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

        if (villagerEntity.isAlive() && (profession == VillagerProfession.ARMORER || profession == VillagerProfession.TOOLSMITH
        || profession == VillagerProfession.WEAPONSMITH)) {
            Optional<LivingEntity> injuredIronGolemOption = getNearestInjuredIronGolem(villagerEntity);
            if (injuredIronGolemOption.isPresent()) {
                this.target = (IronGolem) injuredIronGolemOption.get();
                return true;
            }

        }
        return false;
    }

    protected void start(ServerLevel serverWorld, Villager villagerEntity, long l) {
        if(l > this.nextResponseTime && target != null) {
            villagerEntity.setItemSlot(EquipmentSlot.MAINHAND,  new ItemStack(Items.IRON_INGOT));
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
        villagerEntity.setItemSlot(EquipmentSlot.MAINHAND,  new ItemStack(Items.IRON_INGOT));
        villagerEntity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (villagerEntity.distanceToSqr(target) <= 6 && l > this.nextResponseTime) {
            this.nextResponseTime = l + 10L;
            target.heal(25F);
            float g = 1.0F + (target.getRandom().nextFloat() - target.getRandom().nextFloat()) * 0.2F;
            produceParticles(target);
            target.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, g);
        }

        villagerEntity.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, target);
        villagerEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        villagerEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, WALK_SPEED, 2));

        ++this.ticksRan;

    }

    protected boolean canStillUse(ServerLevel serverWorld, Villager villagerEntity, long l) {
        return this.ticksRan < MAX_RUN_TIME && target != null && target.isAlive() && villagerEntity.distanceToSqr(target) < 100 && target.getHealth() < target.getMaxHealth();
    }

    private Optional<LivingEntity> getNearestInjuredIronGolem(Villager villager) {
        return ((NearestVisibleLivingEntities)villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty()))
                .findClosest(livingEntity -> livingEntity instanceof IronGolem && livingEntity.getHealth() < livingEntity.getMaxHealth()*0.7F);
    }

    private static void holdNothing(Villager villager) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    protected void produceParticles(IronGolem targetGolem) {
        for(int i = 0; i < 5; ++i) {
            double d = targetGolem.getRandom().nextGaussian() * 0.02;
            double e = targetGolem.getRandom().nextGaussian() * 0.02;
            double f = targetGolem.getRandom().nextGaussian() * 0.02;

            if(!targetGolem.level().isClientSide()) {
                ((ServerLevel)targetGolem.level()).sendParticles((ParticleOptions) ParticleTypes.SCRAPE,targetGolem.getRandomX(1), targetGolem.getRandomY() + 0.5, targetGolem.getRandomZ(1), 0,  d, e, f, 0.0);
            }
        }
    }
}
