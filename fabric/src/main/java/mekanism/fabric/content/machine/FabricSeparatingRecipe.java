package mekanism.fabric.content.machine;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.fluid.IFluidStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Transitional Fabric-side representation of one electrolysis (electrolytic-separator) recipe: a fluid input is split,
 * consuming energy each tick, into TWO chemical outputs (left + right).
 *
 * <p>This is deliberately NOT the real Mekanism {@code BasicElectrolysisRecipe}. That class embeds a
 * {@code FluidStackIngredient}, which is welded to NeoForge's {@code FluidStack}/{@code FluidIngredient} (the 183-file
 * wall) and cannot be hoisted byte-identically to {@code :common} yet. So, like the early chemical machines did with a
 * Fabric-only recipe shape, this machine carries its own minimal record and resolves recipes via a plain in-code lookup
 * ({@link FabricSeparatingRecipes}) — no {@code mekanism:separating} {@code RecipeType} is registered (nothing to
 * conflict with). The full {@code FluidStackIngredient} re-architecture remains the documented wall.
 *
 * <p>The input is matched by a {@link TagKey} of {@link Fluid} (e.g. {@code minecraft:water}) plus a required amount in
 * the Fabric fluid unit (droplets), so tag-based matching works without binding to a loader-specific fluid-stack type.
 *
 * @param inputTag     fluid tag the input tank must match (e.g. {@code minecraft:water})
 * @param inputAmount  fluid amount consumed per completed operation, in droplets ({@code FluidConstants.BUCKET}-scaled)
 * @param energyPerTick energy drained from the machine's energy container each processing tick
 * @param leftOutput   the LEFT chemical output produced on completion (its type + amount)
 * @param rightOutput  the RIGHT chemical output produced on completion (its type + amount)
 */
public record FabricSeparatingRecipe(TagKey<Fluid> inputTag, long inputAmount, long energyPerTick,
                                     ChemicalStack leftOutput, ChemicalStack rightOutput) {

    /** True if {@code fluid} (the input tank's current contents) matches this recipe's input tag + has enough amount. */
    public boolean matches(IFluidStack fluid) {
        return !fluid.isEmpty() && fluid.is(inputTag) && fluid.getAmount() >= inputAmount;
    }
}
