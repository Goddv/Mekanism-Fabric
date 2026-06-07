package mekanism.common.registration;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.Supplier;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Loader-neutral deferred register wrapping an Architectury {@link DeferredRegister}. The Architectury-based counterpart
 * of {@link MekanismDeferredRegister}; per-type registers (sounds, game events, ...) extend this as they are migrated off
 * the NeoForge framework. Call {@link #register()} once per loader (NeoForge mod constructor / Fabric entrypoint) to
 * finalize the deferred entries into the active registry.
 */
@NothingNullByDefault
public class MekanismRegister<T> {

    protected final String modid;
    protected final DeferredRegister<T> registrar;

    protected MekanismRegister(String modid, ResourceKey<Registry<T>> registryKey) {
        this.modid = modid;
        this.registrar = DeferredRegister.create(modid, registryKey);
    }

    protected <X extends T> RegistrySupplier<X> doRegister(String name, Supplier<? extends X> supplier) {
        return registrar.register(Identifier.fromNamespaceAndPath(modid, name), supplier);
    }

    /** Finalizes the deferred registrations into the active registry (hooks into whichever loader is running). */
    public void register() {
        registrar.register();
    }
}
