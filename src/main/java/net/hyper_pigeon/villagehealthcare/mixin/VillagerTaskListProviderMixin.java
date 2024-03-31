package net.hyper_pigeon.villagehealthcare.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.BlacksmithVillagerTask;
import net.hyper_pigeon.villagehealthcare.entity.ai.brain.task.ClericVillagerTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VillagerTaskListProvider.class, priority = 1)
public class VillagerTaskListProviderMixin {
    //Pair.of(new GoToNearbyPositionTask(MemoryModuleType.JOB_SITE, 0.4f, 1, 10), 5)

    @Inject(at = @At("RETURN"), method = "createCoreTasks(Lnet/minecraft/village/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;", cancellable = true)
    private static void createCoreTasks(VillagerProfession profession, float speed, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        cir.setReturnValue(ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>builder().
                addAll(cir.getReturnValue()).
                add(Pair.of(4, new ClericVillagerTask())).
                add(Pair.of(4, new BlacksmithVillagerTask())).
                build()
        );
    }

}