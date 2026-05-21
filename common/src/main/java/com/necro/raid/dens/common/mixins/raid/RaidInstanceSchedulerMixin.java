package com.necro.raid.dens.common.mixins.raid;

import com.necro.raid.dens.common.raids.RaidInstance;
import com.necro.raid.dens.common.raids.scripts.RaidTriggerType;
import com.necro.raid.dens.common.raids.scripts.triggers.RaidTrigger;
import com.necro.raid.dens.common.raids.scripts.triggers.TimerTrigger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(RaidInstance.class)
public abstract class RaidInstanceSchedulerMixin {
    @Shadow
    @Final
    private Map<RaidTriggerType, List<RaidTrigger<?>>> triggers;

    @Shadow
    @Final
    private List<RaidTrigger<?>> triggerAddQueue;

    @Shadow
    public abstract void schedule(Runnable runnable, int delay, boolean repeat);

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;removeIf(Ljava/util/function/Predicate;)Z"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean crd$stableScheduledRunQueueTick(List queue, Predicate predicate) {
        boolean changed = false;
        List snapshot = new ArrayList(queue);
        for (Object runnable : snapshot) {
            if (!queue.contains(runnable)) continue;
            if (predicate.test(runnable)) changed |= queue.remove(runnable);
        }
        return changed;
    }

    @Inject(method = "actuallyAddTriggers", at = @At("HEAD"), cancellable = true)
    private void crd$actuallyAddTriggersSafely(CallbackInfo ci) {
        for (RaidTrigger<?> trigger : this.triggerAddQueue) {
            if (trigger.type() == RaidTriggerType.TIMER && trigger instanceof TimerTrigger timerTrigger) {
                this.schedule(() -> timerTrigger.trigger((RaidInstance) (Object) this, null), timerTrigger.after() * 20, timerTrigger.repeat());
            }
            else {
                this.crd$getTriggers(trigger.type()).add(trigger);
            }
        }
        this.triggerAddQueue.clear();
        ci.cancel();
    }

    @Unique
    private List<RaidTrigger<?>> crd$getTriggers(RaidTriggerType type) {
        return this.triggers.computeIfAbsent(type, ignored -> new ArrayList<>());
    }
}
