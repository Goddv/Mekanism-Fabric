package mekanism.api.recipes.basic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mekanism.api.ItemStackTemplateHelper;
import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Contract;


@NothingNullByDefault
public class BasicChemicalCrystallizerRecipe extends ChemicalCrystallizerRecipe {

    private static final Identifier CRYSTALLIZING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "crystallizing");

    protected final ChemicalStackIngredient input;
    protected final ItemStackTemplate output;

    /**
     * @param input  Input.
     * @param output Output.
     */
    public BasicChemicalCrystallizerRecipe(ChemicalStackIngredient input, ItemStackTemplate output) {
        this.input = Objects.requireNonNull(input, "Input cannot be null.");
        Objects.requireNonNull(output, "Output cannot be null.");
        this.output = output;
    }

    @Contract(value = "_ -> new", pure = true)
    @Override
    public ItemStackTemplate getOutput(ChemicalStack input) {
        return output;
    }

    @Override
    public List<ItemStack> getOutputDefinition() {
        return Collections.singletonList(output.create());
    }

    @Override
    public boolean test(ChemicalStack stack) {
        return input.test(stack);
    }

    @Override
    public boolean testType(ChemicalStack stack) {
        return input.testType(stack);
    }

    @Override
    public ChemicalStackIngredient getInput() {
        return input;
    }

    /**
     * For Serializer usage only. Do not modify the returned stack!
     *
     * @return the uncopied output definition
     */
    public ItemStackTemplate getOutputRaw() {
        return this.output;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicChemicalCrystallizerRecipe> getSerializer() {
        return (RecipeSerializer<BasicChemicalCrystallizerRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(CRYSTALLIZING_ID);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicChemicalCrystallizerRecipe other = (BasicChemicalCrystallizerRecipe) o;
        return input.equals(other.input) && ItemStackTemplateHelper.matches(output, other.output);
    }

    @Override
    public int hashCode() {
        int hash = input.hashCode();
        hash = 31 * hash + ItemStackTemplateHelper.hashItemAndComponents(output);
        hash = 31 * hash + output.count();
        return hash;
    }
}