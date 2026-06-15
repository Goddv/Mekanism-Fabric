package mekanism.fabric.content.generator;

import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.fabric.content.machine.gui.MachineGuiType;
import mekanism.fabric.content.machine.gui.MekanismMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: shared base for the real Mekanism generators (Solar/Wind/Heat/Bio), mirroring the
 * NeoForge {@code TileEntityGenerator} family in spirit. Holds a single {@link BasicEnergyContainer} reservoir, exposes
 * it as the strict-energy capability (registered against {@link mekanism.fabric.energy.MekanismFabricEnergy#SIDED}), and
 * provides the server-tick scaffolding: each tick the subclass {@link #generate(ServerLevel)} contributes energy into
 * the reservoir, and adjacent cables/machines pull it out (pull-based model, same as the existing demo generator/cable).
 *
 * <p>Like the demo {@code GeneratorBlockEntity}, a generator is a pure SOURCE: it rejects energy pushed in via the
 * capability so cables/neighbours can't slosh energy back into it; internal generation inserts on the container instance
 * directly. Solar/Wind have no inventory; Heat/Bio add a 1-slot fuel inventory in their own subclasses.
 */
public abstract class AbstractGeneratorBlockEntity extends BlockEntity implements IMekanismStrictEnergyHandler {

    /** Reservoir capacity shared by all four generators — generous enough to buffer several ticks of production. */
    protected static final long CAPACITY = 200_000L;

    protected final BasicEnergyContainer energy = BasicEnergyContainer.create(CAPACITY, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);

    protected AbstractGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Per-tick generation: the subclass inserts its produced energy into {@link #energy} when its conditions are met
     * (sun visible, sky visible + height, fuel burning, ...). Called once per server tick from the block ticker.
     */
    protected abstract void generate(ServerLevel level);

    public final void serverTick(ServerLevel level) {
        if (energy.getNeeded() > 0L) {
            generate(level);
        }
        // The generator is a reservoir: adjacent cables/machines pull from it (see insertEnergy below, which blocks
        // external insertion). Internal generation bypasses that by inserting on the container instance directly.
    }

    /** Convenience for self-tests / GUIs: current stored energy in the (single) reservoir. */
    public long getStoredEnergy() {
        return energy.getEnergy();
    }

    /** Energy fill as 0..1000 permille (for the GUI energy bar). */
    public int getEnergyStoredPermille() {
        long max = energy.getMaxEnergy();
        return max <= 0L ? 0 : (int) (energy.getEnergy() * 1000L / max);
    }

    /** GUI shape for this generator: passive (Solar/Wind) is energy-only; fuel generators override to {@code FUEL_GENERATOR}. */
    protected MachineGuiType menuGuiType() {
        return MachineGuiType.PASSIVE_GENERATOR;
    }

    /** Inventory backing the generator's menu: passive generators have none (an empty, position-validated container). */
    protected Container menuContainer() {
        return new SimpleContainer(0) {
            @Override
            public boolean stillValid(Player player) {
                return Container.stillValidBlockEntity(AbstractGeneratorBlockEntity.this, player);
            }
        };
    }

    /** Live ContainerData for the GUI: [0]=energy permille, [1]=progress permille (generators have no recipe progress). */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? getEnergyStoredPermille() : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return menuGuiType().dataSize();
            }
        };
    }

    /** The extended menu provider opened from the generator block's use handler (energy bar + any fuel slot). */
    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), menuContainer(), containerData(), menuGuiType());
    }

    /** Display title for the menu (the block's name). */
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- energy capability ----
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    // A generator is a SOURCE: reject energy pushed in via the capability. Both cap insert entry points are blocked;
    // internal generation inserts on the container instance directly so it bypasses these.
    @Override
    public long insertEnergy(int container, long amount, mekanism.api.Action action) {
        return amount;
    }

    @Override
    public long insertEnergy(long amount, mekanism.api.Action action) {
        return amount;
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
    }
}
