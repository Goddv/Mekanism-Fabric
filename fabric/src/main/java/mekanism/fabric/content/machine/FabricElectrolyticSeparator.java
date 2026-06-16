package mekanism.fabric.content.machine;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.common.registration.MekanismBlockHolder;
import mekanism.common.registration.MekanismBlockRegister;
import mekanism.fabric.chemical.MekanismFabricChemical;
import mekanism.fabric.energy.MekanismFabricEnergy;
import mekanism.fabric.fluid.MekanismFabricFluid;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Transitional Fabric bring-up: registers the REAL Mekanism Electrolytic Separator block ({@code electrolytic_separator},
 * its real id from {@code MekanismBlocks}) — the first FLUID-input machine on Fabric (fluid input &rarr; two chemical
 * outputs). Block+item registration goes through the loader-neutral {@code :common} {@link MekanismBlockRegister}; the
 * block-entity type + capability wiring stay loader-specific.
 *
 * <p>The bundled {@code electrolytic_separator} blockstate ({@code facing}-only variants) + block/item models are
 * parseable vanilla JSON (no NeoForge custom loader), so they are used as-is. Exposes three capabilities on the BE: the
 * fluid INPUT tank via {@link MekanismFabricFluid#SIDED} (insert), the two chemical OUTPUT tanks via
 * {@link MekanismFabricChemical#SIDED} (extract), and energy via {@link MekanismFabricEnergy#SIDED} (sink). Adds the
 * machine to the Mekanism creative tab.
 *
 * <p>Recipes are the Fabric-only in-code {@link FabricSeparatingRecipes} (NOT a {@code RecipeType}); the real
 * {@code BasicElectrolysisRecipe} embeds a {@code FluidStackIngredient} and remains on the NeoForge hoist wall.
 */
public final class FabricElectrolyticSeparator {

    private static final String MODID = "mekanism";

    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    /** The Electrolytic Separator block (fluid input -> two chemical outputs). Facing-only blockstate (no active). */
    public static final MekanismBlockHolder<ElectrolyticSeparatorBlock, BlockItem> ELECTROLYTIC_SEPARATOR = BLOCKS.register(
          "electrolytic_separator", properties -> new ElectrolyticSeparatorBlock(properties
                .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final RegistrySupplier<BlockEntityType<ElectrolyticSeparatorBlockEntity>> BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "electrolytic_separator"), () ->
                FabricBlockEntityTypeBuilder.create(ElectrolyticSeparatorBlockEntity::new,
                      ELECTROLYTIC_SEPARATOR.block()).build());

    private FabricElectrolyticSeparator() {
    }

    public static void init() {
        // Build the in-code electrolysis recipe list (reads the demo chemical holder; chemical registry already init'd).
        FabricSeparatingRecipes.init();

        BLOCKS.register();      // finalizes the block + block-item
        BE_TYPES.register();

        // Fluid input tank (insert), two chemical output tanks (extract), energy sink.
        MekanismFabricFluid.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
              Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        CreativeTabRegistry.append(tab, ELECTROLYTIC_SEPARATOR.item().get());
    }
}
