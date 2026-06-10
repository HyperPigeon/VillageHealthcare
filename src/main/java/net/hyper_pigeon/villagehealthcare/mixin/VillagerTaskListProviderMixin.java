package net.hyper_pigeon.villagehealthcare.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.BlacksmithVillagerTask;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.ClericVillagerTask;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VillagerGoalPackages.class, priority = 1)
public class VillagerTaskListProviderMixin {
    @Inject(at = @At("RETURN"), method = "getCorePackage", cancellable = true)
    private static void createCoreTasks(Holder<VillagerProfession> profession, float speed, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        cir.setReturnValue(ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder().
                addAll(cir.getReturnValue()).
                add(Pair.of(4, new ClericVillagerTask())).
                add(Pair.of(4, new BlacksmithVillagerTask())).
                build()
        );
    }

}