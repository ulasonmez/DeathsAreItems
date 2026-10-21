package co.blumeplugins.deathsareitems.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // Example recipe:
        // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EXAMPLE_ITEM.get())
        //         .pattern("XXX")
        //         .pattern("XXX")
        //         .pattern("XXX")
        //         .define('X', Items.DIAMOND)
        //         .unlockedBy("has_diamond", has(Items.DIAMOND))
        //         .save(pWriter);
    }
}
