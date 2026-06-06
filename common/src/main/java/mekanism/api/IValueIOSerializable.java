package mekanism.api;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Loader-neutral replacement for NeoForge's {@code net.neoforged.neoforge.common.util.ValueIOSerializable}:
 * the serialize/deserialize contract (over vanilla {@link ValueOutput}/{@link ValueInput}) shared by
 * Mekanism's containers, handlers, tile components and saved data.
 *
 * <p>Lives in {@code :common} so the contract is identical on every loader.
 */
public interface IValueIOSerializable {

    void serialize(ValueOutput output);

    void deserialize(ValueInput input);
}
