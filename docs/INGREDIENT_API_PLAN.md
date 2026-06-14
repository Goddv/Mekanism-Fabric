# Ingredient API → :common — staged port plan (scope+design+verify workflow, corrected)

Source: workflow `s-machines-ingredient-scope` (6 scouts → architect → 3 adversarial verifiers).
The verifiers found **2 fatal defects** in the first design; this doc is the **corrected** plan.

## The three sub-walls (very different heights)

| Sub-wall | State | Height |
|---|---|---|
| **ITEM** | `ItemStackIngredient`+`InputIngredient`+`IItemStackIngredientHelper`+`IIngredientCreator` already in `:common`. Only `IItemStackIngredientCreator` left (3 neoforged, each in ONE branch). | **low** |
| **CHEMICAL** | Mekanism-owned. `ChemicalIngredient` is `sealed` over 6 subtypes; 4 subtypes + both creators are neoforged-clean; the rest use NeoForge codec/composition helpers. | **medium** |
| **FLUID** | `FluidStackIngredient`+`IFluidStackIngredientCreator` wrap NeoForge's whole fluid-ingredient system (8 neoforged). **No vanilla/fabric/architectury fluid-ingredient analog exists.** | **high → DEFERRED** |

## Ordering (corrected)

`S1 (item)` → `S0 (chemical access keystone)` → `S2+S3 merged (atomic chemical sealed-cluster hoist + registry seams + codec shim)` → `S4 (chemical creators + ChemicalStackIngredient)` → `S5 (fluid, deferred)`.

**Key correction:** S1 is INDEPENDENT of S0 (verified: `IItemStackIngredientCreator` references neither `IngredientCreatorAccess` nor `IMekanismAccess`). S0 is a prerequisite for the CHEMICAL wall only.

**Honest value note:** Fabric item→item machines (enrichment_chamber/crusher/energized_smelter incl. vanilla-furnace fallback) ALREADY process real recipes today. Nothing in `:common`/`fabric` calls `item().from()`. So S1 unblocks no *immediately observable* capability — it is structural completion of the item ingredient API in `:common` (needed for the later recipe-class hoist + prefab chain). The first NEW observable Fabric capability is a **chemical recipe processing** at the end of the chemical chain (S0→S2/S3→S4).

---

## S1 — ITEM creator → :common (LOW risk, byte-identical, FIRST increment)

Move `neoforge/src/api/java/mekanism/api/recipes/ingredients/creator/IItemStackIngredientCreator.java` → `common/.../creator/IItemStackIngredientCreator.java`. It extends `IIngredientCreator<Item,ItemStack,ItemStackIngredient>` (already `:common`); keep all default methods on **vanilla** `Ingredient.of(item)/of(items)/of(HolderSet)`. Route the 3 neoforged branches:

- `from(ItemStack,int)` line ~54 `DataComponentIngredient.of(false, patch, holder)` → new helper `buildComponentIngredient(patch, holder)` on the existing `IItemStackIngredientHelper` seam; NeoForge impl body **moved VERBATIM** (`DataComponentIngredient.of(false, patch, holder)`); Fabric impl: vanilla `DataComponentExactPredicate`-backed Ingredient OR `throw` (no Mekanism recipe observed emitting a component item-ingredient — verify; if none, throw is fine).
- `from(HolderGetter,int,List<TagKey>)` line ~206 `new OrHolderSet<>(...)` → helper `combineTags(List<HolderSet>)`; NeoForge body VERBATIM; Fabric: composite/`AnyOf` HolderSet OR `throw` (no runtime caller observed — verify it's datagen-only).
- `from(SizedIngredient)` line ~241 → **DROP from the common interface**; keep as a **neoforge-only overload** on the concrete `ItemStackIngredientCreator` (only namer is `CrTUtils`, NeoForge-only; note: `WrappedSmelterRecipe` uses `from(Ingredient)`, NOT this overload).

NeoForge concrete `ItemStackIngredientCreator` (codec()/streamCodec() already return common `ItemStackIngredient` codecs) becomes the service unchanged. New trivial `FabricItemStackIngredientCreator` + `fabric` META-INF/services descriptor. `NeoItemStackIngredientHelper` + `FabricItemStackIngredientHelper` gain the 2 helper methods.

Byte-identity: only NeoForge bytecode change is the 2 added helper methods (produced-`Ingredient`-identical) + the interface relocating FQN-identically. Validate: 8 neoforge srcsets + common + fabric compile; FabricRecipeSelfTest builds `from(item)`/`from(tag)` and round-trips through `ItemStackIngredient.CODEC`.

---

## S0 — CHEMICAL access keystone: split `IMekanismAccess` + NEW `:common` creator facade (MEDIUM)

**Why:** chemical leaf CODECs call `IngredientCreatorAccess.chemical()` at static-init; `IngredientCreatorAccess.INSTANCE`→`IMekanismAccess.INSTANCE`, and `IMekanismAccess` imports `mezz.jei.*` + `dev.emi.*` (forbidden in `:common`).

**Corrected mechanics (do NOT relocate `IngredientCreatorAccess`):**
1. Companion-split `IMekanismAccess`: NEW `:common` base interface (e.g. `mekanism.api.IMekanismAccessBase`) declaring ONLY `itemStackIngredientCreator()/chemicalStackIngredientCreator()/chemicalIngredientCreator()` (OMIT `fluidStackIngredientCreator()` — its return type is neoforged; no `:common` caller of `fluid()` exists, verified) + `INSTANCE = MekanismAPIBase.getService(IMekanismAccessBase.class)`. NeoForge `IMekanismAccess extends IMekanismAccessBase` keeps `jeiHelper()/emiHelper()/fluidStackIngredientCreator()`.
2. Leave `IngredientCreatorAccess` in `neoforge/src/api` UNCHANGED (preserves the static `fluid()` FQN the neoforge test + datagen depend on — byte-identical).
3. NEW `:common` static facade `mekanism.api.recipes.ingredients.creator.CommonIngredientCreatorAccess` (or similar) exposing `item()/chemicalStack()/chemical()` routed through `IMekanismAccessBase.INSTANCE`. The chemical leaves' CODECs (S2) point HERE.
4. **Descriptors (the invisible runtime trap):** KEEP existing `neoforge/.../META-INF/services/mekanism.api.IMekanismAccess` (do NOT rename — 54+ importers resolve through it). ADD `neoforge/.../META-INF/services/mekanism.api.IMekanismAccessBase` → `mekanism.common.service.MekanismAccess` (one impl satisfies both lookups since `MekanismAccess implements IMekanismAccess extends IMekanismAccessBase`). ADD `fabric/.../META-INF/services/mekanism.api.IMekanismAccessBase` → NEW `FabricMekanismAccess` (there is currently NO Fabric `IMekanismAccess` impl/descriptor at all).
5. **Static-init ordering:** Fabric creator service MUST be ServiceLoader-discoverable at first chemical-CODEC class-load (not an `init()` side-effect). Self-test must assert BOTH `IMekanismAccess.INSTANCE` (NeoForge) AND `IMekanismAccessBase.INSTANCE` (both loaders) resolve non-null.

---

## S2+S3 MERGED — atomic chemical sealed-cluster hoist (MEDIUM, wire-parity-critical)

**Must be ONE commit** (sealed base + all 6 permitted subtypes hoist together — JLS forbids splitting a sealed hierarchy across the `:common`/`neoforge-api` module boundary).

**Prereq (do first, byte-identical on neoforge):** reroute `TagChemicalIngredient`: `MekanismAPI.CHEMICAL_REGISTRY` → `IChemicalRegistryProvider.INSTANCE.chemicalRegistry()` (existing `:common` seam, wired both loaders), `MekanismAPI.logger` → `MekanismAPIBase.logger`. Add NEW `IChemicalIngredientTypeRegistryProvider` seam (mirror `IChemicalRegistryProvider`) so `CHEMICAL_INGREDIENT_TYPES.byNameCodec()` is reachable from `:common` (it lives ONLY on neoforge `MekanismAPI`, built with neoforged `RegistryBuilder`). Fabric impl wraps a Fabric-built type registry populated by a **Fabric `DeferredMapCodecRegister` analog** (architectury) under identical ids (net-new).

**Codec shim** `:common` reimplementing `NeoForgeExtraCodecs` (~90 lines, copy 3 bodies VERBATIM — pure DFU/vanilla):
- `aliasedFieldOf(codec, primary, ...aliases)` — write `primary` (`"children"`) as canonical, accept aliases (`"ingredients"`) on decode (`mapWithAlternative` chain).
- `xor(a,b)` — named inner `XorMapCodec`: both-present ⇒ `DataResult.error`; else first/second/neither ⇒ `apply2((x,y)->y, ...)` fallthrough.
- `dispatchMapOrElse(keyCodec, keyFn, valueFn, fallback)` — anonymous MapCodec whose decode branches on `input.get("type") != null` (the typeless ⇒ `SINGLE_OR_TAG` fallback in `ChemicalIngredientCreator.MAP_CODEC_NONEMPTY` depends on exactly this), `keys()` = concat-distinct.

Hoist `ChemicalIngredient` (sealed base) + `Single/Empty/Intersection/Difference/Compound/Tag ChemicalIngredient`. Per-file edits (NOT literal git mv): import-swap `MekanismAPI`→`MekanismAPIBase` for `EMPTY_CHEMICAL_KEY`/`logger`/`CHEMICAL_REGISTRY_NAME` (all inherited on base); DROP javadoc-only neoforged `@see` imports; `ChemicalIngredient`'s `@see MekanismAPI#CHEMICAL_INGREDIENT_TYPES` → fully-qualified text `{@link mekanism.api.MekanismAPI#...}` or drop (member is NOT on base; javadoc is non-strict — non-fatal). CODECs point at the new `CommonIngredientCreatorAccess` + the codec shim + the type-registry seam.

**Validation:** new self-test code (the chemical self-test does NOT exercise ingredients today): encode compound chemical ingredient → assert emitted key is `"children"`; decode an `"ingredients"`-keyed object; decode a typeless single/tag via fallback. **Parity:** diff encode/decode against a known-good NeoForge-emitted JSON fixture, not just self-round-trip.

---

## S4 — chemical creators + `ChemicalStackIngredient` → :common (MEDIUM)

Hoist `IChemicalIngredientCreator` + `IChemicalStackIngredientCreator` (both neoforged-clean) + `ChemicalStackIngredient` (zero neoforged; `SerializerHelper.POSITIVE_LONG_CODEC` already common). The heavy `ChemicalIngredientCreator` impl (xor/dispatchMapOrElse) is loader-neutral after S2's shim + S3's type-registry seam — **prefer keeping it a per-loader SERVICE (lazy static-init after registries built) over hoisting to `:common`**, to dodge the static-init ordering hazard (its static CODECs touch the type registry + `chemical()` at class-load). Reroute `ChemicalInputCache` (neoforged-clean) to `:common` to unblock the chemical recipe-lookup index. Self-test: build of()/tag()/compound()/difference()/intersection(), wrap in `ChemicalStackIngredient`, assert `test(Holder<Chemical>)` + CODEC/STREAM_CODEC round-trip; assert `ChemicalInputCache` indexes a chemical recipe.

---

## S5 — FLUID ingredient (DEFERRED, HIGH, net-new design)

No vanilla/fabric/architectury fluid-ingredient analog (javap-confirmed). Must author a Mekanism-owned `FluidIngredient` family from scratch (mirror `ChemicalIngredient`) over `Holder<Fluid>`/`TagKey<Fluid>`, re-type `FluidStackIngredient` to `InputIngredient<Fluid, IFluidStack>` (the matched-stack side `IFluidStack` already exists in `:common` from the Fluid-gate work), author codecs byte-identical to `SizedFluidIngredient.CODEC/STREAM_CODEC` (HARD wall), add `IFluidStackIngredientHelper` service, re-author `FluidInputCache.mapInputs` per loader. **Initial Fabric: STUB** (creator throws/returns empty — safe: `IngredientCreatorAccess.fluid()` already routes through a service; no `:common`/Fabric caller; NeoForge keeps its real `SizedFluidIngredient` impl untouched). Gates the 5 fluid-bearing recipe types (Electrolysis/FluidToFluid/Rotary/PressurizedReaction/FluidChemicalToChemical).

## Deferred / out-of-scope for reaching a processing machine
- Fluid ingredients (S5) entirely; Fabric fluid creator stubbed.
- `from(SizedIngredient)` item overload permanently neoforge-only.
- CraftTweaker (`CrTUtils`) ingredient bridges stay neoforge (CrT is neoforge-only).
- JEI/EMI `getRepresentations()` display layer (21 JEI + 4 EMI files) — after the ingredient walls.
