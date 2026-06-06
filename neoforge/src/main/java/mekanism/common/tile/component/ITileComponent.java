package mekanism.common.tile.component;

import java.util.List;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import mekanism.api.IValueIOSerializable;
import org.jetbrains.annotations.NotNull;

public interface ITileComponent extends IValueIOSerializable {

    String getComponentKey();

    default void read(@NotNull ValueInput input) {
        input.child(getComponentKey()).ifPresent(this::deserialize);
    }

    default void write(@NotNull ValueOutput output) {
        String key = getComponentKey();
        ValueOutput child = output.child(key);
        serialize(child);
        //TODO - 26.1: Do we want to just store it regardless and use this.serialize(output.child(getComponentKey()))?
        if (child.isEmpty()) {
            output.discard(key);
        }
    }

    default void applyImplicitComponents(@NotNull DataComponentGetter input) {
    }

    default void collectImplicitComponents(DataComponentMap.Builder builder) {
    }

    default void addRemapEntries(List<DataComponentType<?>> remapEntries) {
    }

    /**
     * Called when the tile is removed, both permanently and during unloads.
     */
    default void invalidate() {
    }

    /**
     * Called when the tile is permanently removed
     */
    default void removed() {
    }

    default void trackForMainContainer(MekanismContainer container) {
    }

    default void addToUpdateTag(@NotNull ValueOutput output) {
    }

    default void readFromUpdateTag(@NotNull ValueInput input) {
    }
}