package com.mekalivingextension.mekalivingextension.mixin;

import com.mekalivingextension.mekalivingextension.MekaSuitDamageHelper;
import mekanism.common.CommonPlayerTickHandler;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonPlayerTickHandler.class, remap = false)
public class CommonPlayerTickHandlerMixin {

    @Inject(method = "onEntityAttacked", at = @At("HEAD"), cancellable = true)
    private void onEntityAttackedHead(LivingIncomingDamageEvent event, CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        DamageContainer damageContainer = event.getContainer();
        float damage = damageContainer.getNewDamage();
        if (damage <= 0 || !entity.isAlive()) {
            return;
        }
        if (!isWearingMekaSuit(entity)) {
            return;
        }
        float ratioAbsorbed = MekaSuitDamageHelper.getDamageAbsorbed(
                entity, damageContainer.getSource(), damage);
        if (ratioAbsorbed > 0) {
            float damageRemaining = damage * Math.max(0, 1 - ratioAbsorbed);
            if (damageRemaining <= 0) {
                event.setCanceled(true);
            } else {
                damageContainer.setNewDamage(damageRemaining);
            }
        }
        ci.cancel();
    }

    private static boolean isWearingMekaSuit(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ItemMekaSuitArmor) {
                return true;
            }
        }
        return false;
    }
}