package com.mekalivingextension.mekalivingextension;

import com.mekalivingextension.mekalivingextension.mixin.ItemMekaSuitArmorMixin;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.tags.MekanismTags;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MekaSuitDamageHelper {

    public static boolean tryAbsorbAll(LivingEntity entity, DamageSource source, float amount) {
        List<Runnable> energyUsageCallbacks = new ArrayList<>(4);
        if (getDamageAbsorbed(entity, source, amount, energyUsageCallbacks) >= 1) {
            for (Runnable callback : energyUsageCallbacks) {
                callback.run();
            }
            return true;
        }
        return false;
    }

    public static float getDamageAbsorbed(LivingEntity entity, DamageSource source, float amount) {
        return getDamageAbsorbed(entity, source, amount, null);
    }

    private static float getDamageAbsorbed(LivingEntity entity, DamageSource source, float amount,
                                            @Nullable List<Runnable> energyUseCallbacks) {
        if (amount <= 0) {
            return 0;
        }
        float ratioAbsorbed = 0;
        List<FoundArmorDetails> armorDetails = new ArrayList<>();

        for (ItemStack stack : entity.getArmorSlots()) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemMekaSuitArmor armor) {
                IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
                if (energyContainer != null) {
                    FoundArmorDetails details = new FoundArmorDetails(energyContainer, armor);
                    armorDetails.add(details);
                    for (IModule<?> module : armor.getModules(stack)) {
                        if (module.isEnabled()) {
                            ICustomModule.ModuleDamageAbsorbInfo damageAbsorbInfo =
                                    getModuleDamageAbsorbInfo(module, source);
                            if (damageAbsorbInfo != null) {
                                float absorption = damageAbsorbInfo.absorptionRatio().getAsFloat();
                                ratioAbsorbed += absorbDamage(details.usageInfo, amount, absorption,
                                        ratioAbsorbed, damageAbsorbInfo.energyCost());
                                if (ratioAbsorbed >= 1) {
                                    break;
                                }
                            }
                        }
                    }
                    if (ratioAbsorbed >= 1) {
                        break;
                    }
                }
            }
        }

        if (ratioAbsorbed < 1) {
            Float absorbRatio = null;
            for (FoundArmorDetails details : armorDetails) {
                if (absorbRatio == null) {
                    if (!source.is(MekanismTags.DamageTypes.MEKASUIT_ALWAYS_SUPPORTED)
                            && source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                        break;
                    }
                    ResourceLocation damageTypeName = source.typeHolder().unwrapKey()
                            .map(ResourceKey::location)
                            .orElseGet(() -> entity.level().registryAccess()
                                    .registry(Registries.DAMAGE_TYPE)
                                    .map(registry -> registry.getKey(source.type()))
                                    .orElse(null));
                    if (damageTypeName != null) {
                        absorbRatio = MekanismConfig.gear.mekaSuitDamageRatios.get().get(damageTypeName);
                    }
                    if (absorbRatio == null) {
                        absorbRatio = MekanismConfig.gear.mekaSuitUnspecifiedDamageRatio.getAsFloat();
                    }
                    if (absorbRatio == 0) {
                        break;
                    }
                }
                float absorption = ((ItemMekaSuitArmorMixin) details.armor).mekalivingextension$getAbsorption() * absorbRatio;
                ratioAbsorbed += absorbDamage(details.usageInfo, amount, absorption,
                        ratioAbsorbed, MekanismConfig.gear.mekaSuitEnergyUsageDamage);
                if (ratioAbsorbed >= 1) {
                    break;
                }
            }
        }

        for (FoundArmorDetails details : armorDetails) {
            if (!details.usageInfo.energyUsed.isZero()) {
                if (energyUseCallbacks == null) {
                    details.energyContainer.extract(details.usageInfo.energyUsed,
                            Action.EXECUTE, AutomationType.MANUAL);
                } else {
                    energyUseCallbacks.add(() -> details.energyContainer.extract(
                            details.usageInfo.energyUsed, Action.EXECUTE, AutomationType.MANUAL));
                }
            }
        }

        return ratioAbsorbed;
    }

    private static <MODULE extends ICustomModule<MODULE>> ICustomModule.ModuleDamageAbsorbInfo getModuleDamageAbsorbInfo(
            IModule<MODULE> module, DamageSource damageSource) {
        return module.getCustomInstance().getDamageAbsorbInfo(module, damageSource);
    }

    private static float absorbDamage(EnergyUsageInfo usageInfo, float amount, float absorption,
                                       float currentAbsorbed, FloatingLongSupplier energyCost) {
        absorption = Math.min(1 - currentAbsorbed, absorption);
        float toAbsorb = amount * absorption;
        if (toAbsorb > 0) {
            FloatingLong usage = energyCost.get().multiply(toAbsorb);
            if (usage.isZero()) {
                return absorption;
            } else if (usageInfo.energyAvailable.greaterOrEqual(usage)) {
                usageInfo.energyUsed = usageInfo.energyUsed.plusEqual(usage);
                usageInfo.energyAvailable = usageInfo.energyAvailable.minusEqual(usage);
                return absorption;
            } else if (!usageInfo.energyAvailable.isZero()) {
                float absorbedPercent = usageInfo.energyAvailable.divide(usage).floatValue();
                usageInfo.energyUsed = usageInfo.energyUsed.plusEqual(usageInfo.energyAvailable);
                usageInfo.energyAvailable = FloatingLong.ZERO;
                return absorption * absorbedPercent;
            }
        }
        return 0;
    }


    private static class FoundArmorDetails {
        final IEnergyContainer energyContainer;
        final EnergyUsageInfo usageInfo;
        final ItemMekaSuitArmor armor;

        FoundArmorDetails(IEnergyContainer energyContainer, ItemMekaSuitArmor armor) {
            this.energyContainer = energyContainer;
            this.usageInfo = new EnergyUsageInfo(energyContainer.getEnergy());
            this.armor = armor;
        }
    }

    private static class EnergyUsageInfo {
        FloatingLong energyAvailable;
        FloatingLong energyUsed = FloatingLong.ZERO;

        EnergyUsageInfo(FloatingLong energyAvailable) {
            this.energyAvailable = energyAvailable.copy();
        }
    }
}