package com.skys.cobblemoncosmetics.recipe;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/**
 * Registers brewing stand recipes for the Liquid Assimilation Potion variants.
 * Uses NeoForge's BrewingRecipe class for proper container validation.
 */
public class ModBrewingRecipes {

    /**
     * Register the brewing recipes event listener on the GAME event bus.
     * Note: RegisterBrewingRecipesEvent is a game event, not a mod event.
     */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModBrewingRecipes::onRegisterBrewingRecipes);
    }

    private static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        SkysCobblemonCosmetics.LOGGER.info("Registering Liquid Assimilation brewing recipes...");
        var builder = event.getBuilder();

        // Debug: Log the item registry names to verify they're correct
        SkysCobblemonCosmetics.LOGGER.info("[Brewing Debug] Base potion: {}",
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.LIQUID_ASSIMILATION_POTION.get()));
        SkysCobblemonCosmetics.LOGGER.info("[Brewing Debug] Lingering potion: {}",
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()));
        SkysCobblemonCosmetics.LOGGER.info("[Brewing Debug] Creeping potion: {}",
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()));

        // Use NeoForge's BrewingRecipe class for proper container recognition
        // NOTE: Using Gunpowder instead of Dragon's Breath because dragon's breath
        // causes the brewing system to treat the output as a "lingering" type that
        // cannot be brewed further. Gunpowder creates "splash" type which CAN be brewed.

        // Base potion + Gunpowder = Lingering potion (throwable)
        builder.addRecipe(new BrewingRecipe(
            Ingredient.of(ModItems.LIQUID_ASSIMILATION_POTION.get()),
            Ingredient.of(Items.GUNPOWDER),
            new ItemStack(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get())
        ));

        // Lingering + Redstone = Extended Lingering
        builder.addRecipe(new BrewingRecipe(
            Ingredient.of(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()),
            Ingredient.of(Items.REDSTONE),
            new ItemStack(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED.get())
        ));

        // Lingering + Vine = Creeping
        builder.addRecipe(new BrewingRecipe(
            Ingredient.of(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()),
            Ingredient.of(Items.VINE),
            new ItemStack(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get())
        ));

        // Creeping + Redstone = Extended Creeping
        builder.addRecipe(new BrewingRecipe(
            Ingredient.of(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()),
            Ingredient.of(Items.REDSTONE),
            new ItemStack(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED.get())
        ));

        // Also add custom recipe versions for name preservation (creeping potion targeting)
        builder.addRecipe(new NamePreservingBrewingRecipe(
            ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get(),
            Items.VINE,
            ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()
        ));
        builder.addRecipe(new NamePreservingBrewingRecipe(
            ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get(),
            Items.REDSTONE,
            ModItems.CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED.get()
        ));

        SkysCobblemonCosmetics.LOGGER.info("Registered 6 Liquid Assimilation brewing recipes");
    }

    /**
     * Custom brewing recipe that preserves custom names from input to output.
     * Used for creeping potion variants where the name determines the target player.
     */
    private static class NamePreservingBrewingRecipe implements IBrewingRecipe {
        private final Item inputItem;
        private final Item ingredientItem;
        private final ItemStack output;

        public NamePreservingBrewingRecipe(Item input, Item ingredient, Item output) {
            this.inputItem = input;
            this.ingredientItem = ingredient;
            this.output = new ItemStack(output);
        }

        @Override
        public boolean isInput(ItemStack stack) {
            return !stack.isEmpty() && stack.is(inputItem);
        }

        @Override
        public boolean isIngredient(ItemStack stack) {
            return !stack.isEmpty() && stack.is(ingredientItem);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (isInput(input) && isIngredient(ingredient)) {
                ItemStack result = this.output.copy();

                // Preserve custom name from input (for creeping potion targeting)
                if (input.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    result.set(
                        net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                        input.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                    );
                }

                return result;
            }
            return ItemStack.EMPTY;
        }
    }
}
