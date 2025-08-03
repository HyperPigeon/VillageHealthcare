package net.hyper_pigeon.villagehealthcare.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.BlacksmithVillagerTask;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.ClericVillagerTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(value = VillagerTaskListProvider.class, priority = 1)
public class VillagerTaskListProviderMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "createCoreTasks")
    private static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> original) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>builder().
                addAll(original).
                add(Pair.of(4, new ClericVillagerTask())).
                add(Pair.of(4, new BlacksmithVillagerTask())).
                build();
    }

}
