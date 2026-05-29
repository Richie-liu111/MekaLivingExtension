package com.mekalivingextension.mekalivingextension.mixin;

import mekanism.common.item.gear.ItemMekaSuitArmor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemMekaSuitArmor.class, remap = false)
public interface ItemMekaSuitArmorMixin {

    @Accessor("absorption")
    float mekalivingextension$getAbsorption();
}