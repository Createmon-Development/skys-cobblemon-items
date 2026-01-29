package com.skys.cobblemoncosmetics.fluid;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

public class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(Registries.FLUID, SkysCobblemonCosmetics.MOD_ID);

    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, SkysCobblemonCosmetics.MOD_ID);

    // Custom texture for liquid assimilation fluid
    private static final ResourceLocation ASSIMILATION_STILL = ResourceLocation.fromNamespaceAndPath(
        SkysCobblemonCosmetics.MOD_ID, "block/liquid_assimilation_still");
    private static final ResourceLocation ASSIMILATION_FLOWING = ResourceLocation.fromNamespaceAndPath(
        SkysCobblemonCosmetics.MOD_ID, "block/liquid_assimilation_flow");

    // Liquid Assimilation fluid type with black cement appearance
    public static final DeferredHolder<FluidType, FluidType> LIQUID_ASSIMILATION_FLUID_TYPE =
        FLUID_TYPES.register("liquid_assimilation", () ->
            new FluidType(FluidType.Properties.create()
                .density(1200)
                .viscosity(1500)
                .canSwim(false)
                .canDrown(true)
                .canPushEntity(true)
            ) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return ASSIMILATION_STILL;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return ASSIMILATION_FLOWING;
                        }

                        @Override
                        public int getTintColor() {
                            return 0xFF1A1A1A; // Dark black/gray tint
                        }
                    });
                }
            });

    // Source fluid
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_ASSIMILATION_SOURCE =
        FLUIDS.register("liquid_assimilation", () ->
            new BaseFlowingFluid.Source(liquidAssimilationProperties()));

    // Flowing fluid
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_ASSIMILATION_FLOWING =
        FLUIDS.register("liquid_assimilation_flowing", () ->
            new BaseFlowingFluid.Flowing(liquidAssimilationProperties()));

    // Fluid properties factory method
    private static BaseFlowingFluid.Properties liquidAssimilationProperties() {
        return new BaseFlowingFluid.Properties(
            LIQUID_ASSIMILATION_FLUID_TYPE,
            LIQUID_ASSIMILATION_SOURCE,
            LIQUID_ASSIMILATION_FLOWING
        ).slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered custom fluids");
    }
}
