package com.mekalivingextension.mekalivingextension.mixin;

import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemMekaSuitArmor.class, remap = false)
public class ItemMekaSuitArmorMixin {

    @Redirect(
        method = "getDamageAbsorbed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/damagesource/DamageSource;FLjava/util/List;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getArmorSlots()Ljava/lang/Iterable;"
        )
    )
    private static Iterable<ItemStack> redirectGetArmorSlots(Player instance) {
        return ((LivingEntity) (Object) instance).getArmorSlots();
    }

    @Redirect(
        method = "getDamageAbsorbed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/damagesource/DamageSource;FLjava/util/List;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;"
        )
    )
    private static net.minecraft.world.level.Level redirectLevel(Player instance) {
        return ((LivingEntity) (Object) instance).level();
    }
}