package com.mekalivingextension.mekalivingextension;

import com.mekalivingextension.mekalivingextension.mixin.ItemMekaSuitArmorMixin;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPITags;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.datamaps.MekaSuitAbsorption;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.ICustomModule.ModuleDamageAbsorbInfo;
import mekanism.api.gear.IModule;
import mekanism.api.math.MathUtils;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.util.StorageUtils;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public class MekaSuitDamageHelper {

    public static float getDamageAbsorbed(LivingEntity entity, DamageSource source, float amount) {
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
                            ModuleDamageAbsorbInfo damageAbsorbInfo = getModuleDamageAbsorbInfo(module, source);
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
                    if (source.is(Tags.DamageTypes.IS_TECHNICAL) ||
                            !source.is(MekanismAPITags.DamageTypes.MEKASUIT_ALWAYS_SUPPORTED)
                                    && source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                        break;
                    }
                    MekaSuitAbsorption absorptionData = IMekanismDataMapTypes.INSTANCE
                            .getMekaSuitAbsorption(entity.registryAccess(), source.typeHolder());
                    if (absorptionData == null) {
                        absorbRatio = MekanismConfig.gear.mekaSuitUnspecifiedDamageRatio.get();
                    } else {
                        absorbRatio = absorptionData.absorption();
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
            details.drainEnergy();
        }

        return Math.min(ratioAbsorbed, 1);
    }

    private static <MODULE extends ICustomModule<MODULE>> ModuleDamageAbsorbInfo getModuleDamageAbsorbInfo(
            IModule<MODULE> module, DamageSource damageSource) {
        return module.getCustomInstance().getDamageAbsorbInfo(module, damageSource);
    }

    private static float absorbDamage(EnergyUsageInfo usageInfo, float amount, float absorption,
                                       float currentAbsorbed, LongSupplier energyCost) {
        absorption = Math.min(1 - currentAbsorbed, absorption);
        float toAbsorb = amount * absorption;
        if (toAbsorb > 0) {
            long usage = MathUtils.ceilToLong(energyCost.getAsLong() * toAbsorb);
            if (usage == 0L) {
                return absorption;
            } else if (usageInfo.energyAvailable >= usage) {
                usageInfo.energyUsed += usage;
                usageInfo.energyAvailable -= usage;
                return absorption;
            } else if (usageInfo.energyAvailable > 0L) {
                float absorbedPercent = (float) (usageInfo.energyAvailable / (double) usage);
                usageInfo.energyUsed += usageInfo.energyAvailable;
                usageInfo.energyAvailable = 0L;
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

        public void drainEnergy() {
            energyContainer.extract(usageInfo.energyUsed, Action.EXECUTE, AutomationType.MANUAL);
        }
    }

    private static class EnergyUsageInfo {
        long energyAvailable;
        long energyUsed = 0L;

        EnergyUsageInfo(long energyAvailable) {
            this.energyAvailable = energyAvailable;
        }
    }
}