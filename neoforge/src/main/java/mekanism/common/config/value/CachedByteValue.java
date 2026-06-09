package mekanism.common.config.value;

import mekanism.api.functions.ByteSupplier;
import mekanism.common.config.IMekanismConfig;

public class CachedByteValue extends CachedValue<Byte> implements ByteSupplier {

    private boolean resolved;
    private byte cachedValue;

    private CachedByteValue(IMekanismConfig config, IConfigValue<Byte> internal) {
        super(config, internal);
    }

    public static CachedByteValue wrap(IMekanismConfig config, IConfigValue<Byte> internal) {
        return new CachedByteValue(config, internal);
    }

    public static CachedByteValue wrap(IMekanismConfig config, net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Byte> internal) {
        return wrap(config, new NeoConfigValue<>(internal));
    }

    public byte getOrDefault() {
        if (resolved || isLoaded()) {
            return get();
        }
        return internal.getDefault();
    }

    public byte get() {
        if (!resolved) {
            //If we don't have a cached value or need to resolve it again, get it from the actual ConfigValue
            cachedValue = internal.get();
            resolved = true;
        }
        return cachedValue;
    }

    @Override
    public byte getAsByte() {
        return get();
    }

    public void set(byte value) {
        internal.set(value);
        cachedValue = value;
    }

    @Override
    protected boolean clearCachedValue(boolean checkChanged) {
        if (!resolved) {
            //Isn't cached don't need to clear it or run any invalidation listeners
            return false;
        }
        byte oldCachedValue = cachedValue;
        resolved = false;
        //Return if we are meant to check the changed ones, and it is different then it used to be
        return checkChanged && oldCachedValue != get();
    }
}