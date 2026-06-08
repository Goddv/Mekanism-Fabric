package mekanism.fabric.content.machine;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.common.registration.MekanismBlockHolder;
import mekanism.common.registration.MekanismBlockRegister;
import mekanism.fabric.energy.MekanismFabricEnergy;
import mekanism.fabric.recipe.MekanismRecipeTypesRegistrar;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Transitional Fabric bring-up: registers REAL Mekanism machine blocks (enrichment_chamber, crusher, energized_smelter,
 * osmium_compressor, combiner) on Fabric — real ids whose facing/active blockstates + models + textures are already
 * bundled — sharing one {@link MachineBlock} class + {@link MachineBlockEntity}. Block+item registration now goes through
 * the loader-neutral {@code :common} {@link MekanismBlockRegister} framework; the shared block-entity type + capability
 * wiring stay loader-specific. Adds the machines to the Mekanism creative tab. The processing loop is a demo; the real
 * recipe/GUI systems replace it once the machine framework is migrated to :common.
 */
public final class FabricRealMachines {

    private static final String MODID = "mekanism";

    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    private static final List<MekanismBlockHolder<MachineBlock, BlockItem>> MACHINES = new ArrayList<>();

    private static final String[] MACHINE_NAMES = {
          "enrichment_chamber", "crusher", "energized_smelter", "osmium_compressor", "combiner"
    };

    static {
        for (String name : MACHINE_NAMES) {
            Supplier<RecipeType<ItemStackToItemStackRecipe>> recipeType = recipeTypeFor(name);
            boolean vanillaSmelting = name.equals("energized_smelter");
            MACHINES.add(BLOCKS.register(name, properties -> new MachineBlock(properties
                  .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL), recipeType, vanillaSmelting)));
        }
    }

    /**
     * Maps each machine to the item&rarr;item recipe type it processes. The compressor/combiner have no item&rarr;item
     * type (they need chemical/dual-item recipe types not yet ported) so they get {@code null} (no processing for now).
     */
    @Nullable
    private static Supplier<RecipeType<ItemStackToItemStackRecipe>> recipeTypeFor(String name) {
        return switch (name) {
            case "enrichment_chamber" -> MekanismRecipeTypesRegistrar.ENRICHING_TYPE::get;
            case "crusher" -> MekanismRecipeTypesRegistrar.CRUSHING_TYPE::get;
            case "energized_smelter" -> MekanismRecipeTypesRegistrar.SMELTING_TYPE::get;
            default -> null;
        };
    }

    /** Single block-entity type shared by all machine blocks. */
    public static final RegistrySupplier<BlockEntityType<MachineBlockEntity>> BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "machine"), () ->
                FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new,
                      MACHINES.stream().map(MekanismBlockHolder::block).toArray(Block[]::new)).build());

    private FabricRealMachines() {
    }

    /** A representative machine block (enrichment_chamber) for the dev self-test. */
    public static MekanismBlockHolder<MachineBlock, BlockItem> enrichmentChamber() {
        return MACHINES.getFirst();
    }

    public static void init() {
        BLOCKS.register();      // finalizes machine blocks + their block-items
        BE_TYPES.register();
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BE_TYPE.get());
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        for (MekanismBlockHolder<MachineBlock, BlockItem> machine : MACHINES) {
            // Pass the resolved BlockItem (an ItemLike) — items are finalized above; the holder is both ItemLike and
            // Supplier, which would make the append(...) overload ambiguous.
            CreativeTabRegistry.append(tab, machine.item().get());
        }
    }
}
