package mekanism.common.content.blocktype;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral core of {@link BlockType}: the class-keyed attribute map plus its accessors. Split into {@code :common}
 * so loader-neutral blocks (e.g. {@code BlockResource} via {@code BlockMekanismBase}) can reach the attribute lookup
 * without dragging the NeoForge-coupled {@code BlockTypeBuilder}/{@code BlockTypeTile} layer. The ctor is package-private
 * (house style) so every instance is constructed through {@link BlockType} (or a subtype) — this keeps
 * {@code BlockType.get(Block)}'s downcast provably safe.
 */
public class BlockTypeBase {

    private final ILangEntry description;

    protected final Map<Class<? extends Attribute>, Attribute> attributeMap = new HashMap<>();

    BlockTypeBase(ILangEntry description) {
        this.description = description;
    }

    public boolean has(Class<? extends Attribute> type) {
        return attributeMap.containsKey(type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <ATTRIBUTE extends Attribute> ATTRIBUTE get(Class<ATTRIBUTE> type) {
        return (ATTRIBUTE) attributeMap.get(type);
    }

    public void add(Attribute... attrs) {
        for (Attribute attr : attrs) {
            attributeMap.put(attr.getClass(), attr);
        }
    }

    @SafeVarargs
    public final void remove(Class<? extends Attribute>... attrs) {
        for (Class<? extends Attribute> attr : attrs) {
            attributeMap.remove(attr);
        }
    }

    public Collection<Attribute> getAll() {
        return attributeMap.values();
    }

    @NotNull
    public ILangEntry getDescription() {
        return description;
    }
}
