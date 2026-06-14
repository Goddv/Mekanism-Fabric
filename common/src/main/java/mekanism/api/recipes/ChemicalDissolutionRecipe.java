package mekanism.api.recipes;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public abstract class ChemicalDissolutionRecipe extends ItemStackChemicalToObjectRecipe<ChemicalStack> {

    private static final Identifier DISSOLUTION_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "dissolution");
    private static final Identifier CHEMICAL_DISSOLUTION_CHAMBER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_dissolution_chamber");

    @SuppressWarnings("unchecked")
    @Override
    public final RecipeType<ChemicalDissolutionRecipe> getType() {
        return (RecipeType<ChemicalDissolutionRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(DISSOLUTION_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CHEMICAL_DISSOLUTION_CHAMBER_ID));
    }
}
