package mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/entity/Entity")
public abstract class TestMixin {
    @Shadow
    public abstract boolean isInWater();

    @Inject(method = "tick", at = @At("HEAD"))
    private void testMixin(CallbackInfo ci) {
        System.out.println("giggity");
        if (this.isInWater()) {
            System.out.println("giggity");
        }
    }

    @ModifyReturnValue(method = "dismountsUnderwater", at = @At("RETURN"))
    private boolean mixinExtrasTest(boolean original) {
        return false;
    }

}
