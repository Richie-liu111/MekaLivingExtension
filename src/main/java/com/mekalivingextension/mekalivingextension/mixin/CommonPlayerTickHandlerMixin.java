package com.mekalivingextension.mekalivingextension.mixin;

import mekanism.common.CommonPlayerTickHandler;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonPlayerTickHandler.class, remap = false)
public class CommonPlayerTickHandlerMixin {

    @Inject(method = "onEntityAttacked", at = @At("HEAD"), cancellable = true)
    private void onEntityAttackedHead(
            net.minecraftforge.event.entity.living.LivingAttackEvent event,
            CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        if (event.getAmount() <= 0 || !entity.isAlive()) {
            return;
        }
        if (!isWearingMekaSuit(entity)) {
            return;
        }
        if (ItemMekaSuitArmor.tryAbsorbAll(
                (Player) (Object) entity, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
        ci.cancel();
    }

    @Inject(method = "onLivingHurt", at = @At("HEAD"), cancellable = true)
    private void onLivingHurtHead(
            net.minecraftforge.event.entity.living.LivingHurtEvent event,
            CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        if (event.getAmount() <= 0 || !entity.isAlive()) {
            return;
        }
        if (!isWearingMekaSuit(entity)) {
            return;
        }
        float ratioAbsorbed = ItemMekaSuitArmor.getDamageAbsorbed(
                (Player) (Object) entity, event.getSource(), event.getAmount());
        if (ratioAbsorbed > 0) {
            float damageRemaining = event.getAmount() * Math.max(0, 1 - ratioAbsorbed);
            if (damageRemaining <= 0) {
                event.setCanceled(true);
            } else {
                event.setAmount(damageRemaining);
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
