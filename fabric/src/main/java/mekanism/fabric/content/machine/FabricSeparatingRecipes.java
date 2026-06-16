package mekanism.fabric.content.machine;

import java.util.List;
import java.util.Optional;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.fluid.IFluidStack;
import mekanism.fabric.chemical.FabricChemicalRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;

/**
 * Transitional Fabric-only registry of {@link FabricSeparatingRecipe electrolysis recipes} for the Electrolytic
 * Separator, defined as a small hardcoded in-code list (no datapack JSON, no {@code RecipeType}) — the simplest shape
 * for this transitional fluid&rarr;chemical machine while the real {@code FluidStackIngredient} stays on the
 * {@code :common} hoist wall.
 *
 * <p>The single bundled recipe mirrors real Mekanism's water electrolysis (water &rarr; hydrogen + oxygen): one bucket of
 * {@code minecraft:water} &rarr; two chemical outputs. Real Mekanism hydrogen/oxygen are not registered on Fabric, so
 * both outputs use the registered {@link FabricChemicalRegistry#demo() demo chemical} (left + right), which is enough to
 * exercise the two-output fluid&rarr;chemical path end to end.
 */
public final class FabricSeparatingRecipes {

    /** Per-completion fluid drained: one bucket of water (in droplets, the Fabric fluid unit). */
    public static final long WATER_PER_OP = FluidConstants.BUCKET;
    /** Per-tick energy draw while electrolysing. */
    public static final long ENERGY_PER_TICK = 100L;
    /** Per-completion chemical produced on EACH output side. */
    public static final long OUTPUT_PER_OP = 100L;

    private static List<FabricSeparatingRecipe> recipes = List.of();

    private FabricSeparatingRecipes() {
    }

    /**
     * Builds the in-code recipe list. Must run after {@link FabricChemicalRegistry#init()} (it reads the demo chemical
     * holder for the outputs). Idempotent; called from {@link FabricElectrolyticSeparator#init()}.
     */
    public static void init() {
        Holder<mekanism.api.chemical.Chemical> demo = FabricChemicalRegistry.demo();
        recipes = List.of(new FabricSeparatingRecipe(
              FluidTags.WATER, WATER_PER_OP, ENERGY_PER_TICK,
              new ChemicalStack(demo, OUTPUT_PER_OP),   // left output  (stands in for hydrogen)
              new ChemicalStack(demo, OUTPUT_PER_OP))); // right output (stands in for oxygen)
    }

    /** First recipe whose input tag + amount the given input-tank fluid satisfies, if any. */
    public static Optional<FabricSeparatingRecipe> find(IFluidStack input) {
        for (FabricSeparatingRecipe recipe : recipes) {
            if (recipe.matches(input)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }
}
