# Real Mekanism Framework — Direct Fabric Port Roadmap

Goal: port the ACTUAL Mekanism tile/machine/GUI/transmitter/generator framework to Fabric (not the
transitional scaffolding). Work from the real source in `neoforge/src/{main,api,generators}`. Hoist
loader-neutral code to `common/src/main` (same FQN); bridge NeoForge-coupled pieces per loader. NeoForge
must keep compiling + behaving identically (user runs it; cannot runtime-test here).

Scale: ~144 tiles, ~232 GUI classes, 26 network classes, 26 generator tiles. Multi-session effort.

## Keystone: the capability seam
`TileEntityMekanism` already implements loader-neutral handler interfaces
(`IMekanismStrictEnergyHandler.getEnergyContainers(side)`, `IMekanismInventory.getInventorySlots(side)`,
fluid/chemical/heat). These are the single source of truth on both loaders.
- The NeoForge `*HandlerManager` + `BasicSidedCapabilityResolver` + `CapabilityCache` chain is typed on
  `net.neoforged.neoforge.capabilities.BlockCapability` → CANNOT hoist; stays NeoForge-only.
- New `ICapabilityExposureService` (`:common`): `attachCapabilities(tile)` + `registerForType(type, supportedTypes)`.
  - NeoForge impl: existing managers + `RegisterCapabilitiesEvent` (unchanged behavior).
  - Fabric impl: `MekanismFabricEnergy/Heat/Chemical/Fluid.SIDED.registerForBlockEntity(...)` wrapping the
    tile's `getEnergyContainers(side)` via the hoisted `ProxyStrictEnergyHandler` (identical sided filtering).
- Net: one tile code path; NeoForge reaches it via manager.resolve()→Proxy→holder, Fabric via BlockApiLookup
  provider→Proxy→holder. No tile code imports BlockCapability or BlockApiLookup.

## VERIFIED coupling reality (corrects the recon's "hoist storage first" optimism)
The storage layer is NOT a clean pre-hoistable leaf — it is entangled with the tile core + has NeoForge
item-transfer coupling. Verified:
- `MachineEnergyContainer` imports `TileEntityMekanism`, `TileComponentUpgrade`, `TileEntityFactory`,
  `TileEntityProgressMachine`, `Attribute`/`AttributeEnergy` → depends on the tile core + attribute system.
- `IInventorySlot` (api) imports NeoForge `FluidStack`, `IFluidHandler`, `IItemHandler`;
  `BasicInventorySlot` imports NeoForge `transfer.access.ItemAccess`, `transfer.item.ItemResource`,
  `ItemStackResourceHandler` → the inventory slot layer needs an ITEM-HANDLER ABSTRACTION first
  (mirror the energy/fluid/chemical split: a `:common` item-handler interface + per-loader impls).
- Clean (no NeoForge, but consumed by the tile): `MachineEnergyContainer` itself, the 5 holder interfaces
  (`I{Energy,Inventory,Heat,Fluid,Chemical}*Holder`).
CONCLUSION: there is no small isolated leaf to hoist first. The first CODE stage must port the
**item-handler abstraction + attribute system + tile core** together (they form one connected graph), then
the capability seam. Plan a fresh multi-session push for this; do not start it piecemeal.

### Stage 1 progress + verified slot-impl cascade
DONE (committed): `IInventorySlot` (javadoc-only NeoForge refs stripped), `IHolder` + the 4 container-holder
contracts (`I{Energy,Inventory,Heat,Chemical}*Holder`) → all in `:common`, all 3 modules green.
DEFERRED leaf: `IFluidTankHolder` (needs `IExtendedFluidTank` hoisted first).

`BasicInventorySlot` is the slot-impl CASCADE POINT — verified couplings to resolve together next session:
- **itemAccess machinery** (NeoForge `ItemAccess`/`ItemStackResourceHandler`/`ItemResource`): field (line 33)
  + `itemAccess()` getter (338) + `ResourceHandlerWrapper` inner class (342-363). `setStackUnchecked(ItemStack)`
  is PUBLIC, so extractable. ONLY external caller is `FusionReactorMultiblockData` (generators, deferred
  multiblock) — 2 calls. PLAN: remove the machinery from the `:common` BasicInventorySlot; add a NeoForge-side
  `SlotResourceHandler extends ItemStackResourceHandler` (wraps an `IInventorySlot` via getStack/setStackUnchecked/
  isItemValid) + a helper `itemAccess(BasicInventorySlot)`; repoint the 2 fusion-reactor calls.
- **container-slot types** (mutual dep): `BasicInventorySlot.createContainerSlot()` returns
  `InventoryContainerSlot` (extends vanilla Slot, implements `IInsertableSlot`; deps: `ContainerSlotType`[11L],
  `SlotOverlay`[40L], `ISupportsWarning`[14L], `BasicInventorySlot`). All NeoForge-clean but pull in
  `IInsertableSlot` (container subsystem). Hoist these 5 together with BasicInventorySlot.
- **slot subclasses**: `InputInventorySlot`/`OutputInventorySlot` clean (only `ContainerSlotType`).
  `EnergyInventorySlot` is NOT clean — pulls `EnergyCompatUtils` (NeoForge energy-item compat), `MekanismRecipeType`,
  `ItemStackToEnergyRecipe`, `Mekanism` → DEFER until the energy-item-cap compat + recipe-cache land.
So next session = BasicInventorySlot (minus itemAccess) + InventoryContainerSlot + ContainerSlotType + SlotOverlay
+ ISupportsWarning + IInsertableSlot + InputInventorySlot + OutputInventorySlot, as one coordinated commit.

CONCRETE next-session first tasks (in order):
1. Item-handler abstraction: `:common` `IMekanismItemHandler`/slot interfaces decoupled from NeoForge
   `IItemHandler`/`ItemResource`; per-loader impls (NeoForge `IItemHandler`, Fabric fabric-transfer
   `Storage<ItemVariant>`). Then `BasicInventorySlot` + Input/Output/EnergyInventorySlot hoist.
2. Attribute/BlockType system: `Attribute`, `Attributes`, `AttributeEnergy`, `AttributeStateFacing/Active`,
   `BlockTypeTile`/`MachineType` (the declaration system every tiled block needs).
3. Tile core: `TileEntityUpdateable` + `TileEntityMekanismBase` (minus BlockCapability cache) + component
   system; constructor calls `ICapabilityExposureService.attachCapabilities(this)`.
4. Capability seam (keystone) + `MachineEnergyContainer` + holders hoist alongside the tile core.

## Dependency order (stages)
1. **Storage/holders** (ENTANGLED with tile core — see above; port together, not first):
   `MachineEnergyContainer`, `BasicInventorySlot` +
   Input/Output/EnergyInventorySlot, holder interfaces (`IEnergyContainerHolder`, `IInventorySlotHolder`,
   `IChemicalTankHolder`, `IFluidTankHolder`, `IHeatCapacitorHolder`), `EnergyContainerHelper`,
   `InventorySlotHelper`, `ContainerType`, `IContentsListener`.
2. **Recipe-cache**: move `CachedRecipe`/`OneInputCachedRecipe`/`IInputHandler`/`IOutputHandler`/
   `InputHelper`/`OutputHelper`/`ICachedRecipeHolder` (neoforge/src/api → common/src/api). Hoist
   `RecipeCacheLookupMonitor`, `IRecipeLookupHandler`, `ISingleRecipeLookupHandler`,
   `InputRecipeCache.SingleItem`, `MekanismRecipeType`, `IMekanismRecipeTypeProvider`. **MekanismRecipeType
   has real coupling** (FMLEnvironment.dist, ServerLifecycleHooks, MekanismClient, DeferredHolder) → split
   client/server world access behind a service. Bridge `CommonWorldTickHandler.flushTagAndRecipeCaches`
   (minimal common static flag; Fabric may leave false for the slice).
3. **Capability seam** (KEYSTONE, highest risk): `ICapabilityExposureService` + 2 impls + `ProxyStrictEnergyHandler`/Proxy* hoist.
4. **Tile core**: hoist `TileEntityUpdateable`; create `TileEntityMekanismBase` = `TileEntityMekanism` minus
   the `BlockCapability` cache + invalidate*. Keep `CapabilityTileEntity` (BlockCapability machinery)
   NeoForge-only; NeoForge `TileEntityMekanism` extends base + CapabilityTileEntity. Constructor calls
   `ICapabilityExposureService.attachCapabilities(this)` AFTER all `getInitial*Containers` run. Hoist the
   component system (`ITileComponent`, `TileComponentConfig/Ejector/Upgrade/Security/Frequency`).
5. **Machine hierarchy** (verified ZERO neoforge imports): `TileEntityConfigurableMachine`,
   `TileEntityRecipeMachine`, `TileEntityProgressMachine`, `TileEntityElectricMachine`, then
   `TileEntityEnrichmentChamber` (3 lines: getRecipeType → ENRICHING). Hoist nearly verbatim.
6. **Container + sync**: `MekanismContainer`, `MekanismTileContainer`, `ITrackableContainer`, `Syncable*`,
   `SyncMapper`, `PropertyData`. Only coupling: FluidStack (use IFluidStack) + `PacketDistributor` →
   `IContainerSyncSender` service (NeoForge PacketDistributor / Fabric ServerPlayNetworking).
7. **GUI**: `GuiElement`, `IGuiWrapper`, `GuiTexturedElement`, `GuiMekanism(Tile)`, `GuiConfigurableTile`,
   `GuiElectricMachine` + elements (`GuiProgress`, `GuiVerticalPowerBar`, `GuiEnergyTab`, `GuiUpArrow`,
   `GuiSlot`). VERIFIED `GuiGraphicsExtractor` + `RenderPipelines` are vanilla `net.minecraft.client.*` →
   render code hoists intact. Only per-loader: screen REGISTRATION (Fabric `MenuScreens.register` in client entrypoint).
8. **Registration glue**: tile/menu types via Architectury DeferredRegister (proven); per-loader cap exposure + screen reg.
9. **Solar generator**: `TileEntityGenerator` + `TileEntitySolarGenerator` (verified zero-neoforge except
   push-cache → reuse `EnergyTransferHelper`/`MekanismFabricEnergy.SIDED`). Minimal real power source.
10. **Real transmitter network** (DEFERRED past first slice): `DynamicBufferedNetwork`/`EnergyNetwork`/
    `Transmitter` graph hoists, but acceptor-cache (BlockCapabilityCache→BlockApiLookup), `ServerTickEvent`
    network tick, and `ChunkTicketLevelUpdatedEvent` (no Fabric equivalent) need new abstractions. For the
    first slice REUSE the proven transitional Fabric `CableBlockEntity`/`EnergyTransferHelper` pull as the wire.

## First vertical slice
Real `TileEntityEnrichmentChamber` on the real minimal framework + real `GuiElectricMachine` + sync, powered
by real `TileEntitySolarGenerator` through the existing Fabric cable. ~55–70 files, 2–4 focused sessions:
- S1 = storage/holders + recipe-cache + capability seam (stages 1–3, gates everything).
- S2 = tile base + prefabs + chamber (4–5).
- S3 = container/sync + GUI (6–7).
- S4 = registration + solar + end-to-end wiring (8–9). Retire transitional MachineBlockEntity/Menu/Screen.

## Per-loader splits (services)
- `ICapabilityExposureService` (cap registration: RegisterCapabilitiesEvent vs BlockApiLookup).
- `IContainerSyncSender` (PacketDistributor vs ServerPlayNetworking).
- Energy emit for generators (BlockEnergyCapabilityCache vs EnergyTransferHelper).
- Heat adjacent-tile lookup (BlockCapabilityCache vs MekanismFabricHeat.SIDED).
- Capability invalidation on side-config change (NeoForge real / Fabric no-op — BlockApiLookup re-queries).
- Screen registration; computer-method registry (Fabric stub).

## Deferred
Real transmitter network; 25 other generators; factories; multiblocks; full config/upgrade/security/frequency
GUIs; capability invalidation for interactive side rotation; computer integration; transmitter model-data render.

## Risks
- Capability exposure semantics drift (null-side read-only vs sided) — both loaders MUST use the same `ProxyStrictEnergyHandler`.
- BlockCapability type leakage during the tile-constructor split (manager fields/`addCapabilityResolvers` must move to the NeoForge subclass).
- Container sync packet format must match so the client GUI bar/arrow actually sync.
- NeoForge regression (cannot runtime-test) — rely on `:neoforge:build` + behavioral reasoning; the Stage 3–4 manager split is the danger zone.

---

## CORE-MOVE CLOSURE ANALYSIS (computed, deterministic graph walk)

Computed the transitive must-move-together closure of the blocktype+attribute cluster (BFS following each neoforge file's loader-CLEAN, not-yet-in-`:common` mekanism imports; stop at loader-coupled files = seam boundaries, and at `:common` files = resolved):

- **381 files** must move together (loader-clean, not yet in `:common`) — essentially the entire Mekanism CONTENT layer: BlockType/attributes, all `TileEntity*` concretes, all containers + the Syncable* framework, multiblock, transmitters/network, QIO, frequencies, filters, items, criteria triggers.
- **169 loader-coupled SEAM boundaries** the closure hits — the per-loader walls.
- **~60 unresolved** (recipe types not yet hoisted, api interfaces, vanilla-ish).

**Why so large:** `MekanismBlockTypes` (963 lines) imports every tile type → pulls in the whole content layer; `BlockType`/`Attribute` are referenced everywhere; `FactoryType`→`MekanismBlockTypes` is a key explosion edge. The naive "hoist the cluster" = "hoist all content" — NOT a single commit.

**KEY INSIGHT — per-file loader coupling is TINY:** `TileEntityMekanism` (1695 lines) has only **2** `net.neoforged` imports (`BlockCapabilityCache` for adjacent-heat lookup; `FluidStack` for 2 fluid-collection methods). Most closure files are loader-clean and blocked only by a handful of central seam classes. So the megaproject is **mostly mechanical** once the big seams exist.

### The ~6 big seams that gate the content layer (build each green, in-place, NeoForge-identical):
1. **Registration-object seam** — `TileEntityTypeRegistryObject`→`ITileHolder` ✅ DONE; `ContainerTypeRegistryObject`→`IContainerTypeHolder`+`IGuiOpener` service (TODO, design in regobj-seam workflow output); `SoundEventRegistryObject` already `:common`; `BlockRegistryObject`: AttributeUpgradeable ✅, FactoryType deferred (datagen `FactoryRecipeProvider` uses `getBaseBlock()` as RO for recipe key).
2. **Capability seam** — `EnergyHandlerManager`/`HeatHandlerManager`/`ChemicalHandlerManager` + `Capabilities` + `BlockCapabilityCache` (all typed on NeoForge `BlockCapability`) → hoisted tile implements loader-neutral handler getters directly; `ICapabilityExposureService` (NeoForge keeps managers+`RegisterCapabilitiesEvent`, Fabric registers `BlockApiLookup`); adjacent-heat lookup → service. THE keystone for machine functionality.
3. **MekanismConfig seam** — NeoForge config API → a `:common` config abstraction (HARD).
4. **MekanismUtils split** — extract the loader-clean subset to `:common`; keep NeoForge bits (Capabilities/EnergyCompat) behind the existing services. Also `WorldUtils` (922 lines, 6 net.neoforged) same treatment.
5. **Container networking seam** — `MekanismContainer.broadcastChanges()` `PacketDistributor` → `IContainerSyncSender` (NeoForge PacketDistributor / Fabric ServerPlayNetworking).
6. **Fluid completion** — `FluidStack`→`IFluidStack` across tiles/tanks (the ~163-file fluid gate, partially done).

### Staged execution order (each stage = green commits; cluster hoist is the LAST stage):
- **S0 (done):** registration-object seams (ITileHolder, AttributeUpgradeable, AttributeEnergy→ConstantPredicatesBase).
- **S1:** MekanismUtils/WorldUtils clean-subset split + remaining leaf/utility hoists inside the closure (shrinks 381).
- **S2:** Capability seam (managers + Capabilities + adjacent-heat) — the keystone; unblocks the storage layer (MachineEnergyContainer, *HandlerManager) + TileEntityMekanism's cap deps.
- **S3:** MekanismConfig abstraction + container-networking seam + container seam (IContainerTypeHolder/IGuiOpener).
- **S4:** Fluid completion (FluidStack→IFluidStack in the closure tiles/tanks).
- **S5:** Hoist TileEntityMekanism + the tile components + MekanismContainer + the now-unblocked closure chunks.
- **S6:** Hoist the blocktype/attribute cluster + a first real machine (EnrichmentChamber) + its container/GUI; wire Fabric capability exposure via `ICapabilityExposureService`.

The registry hubs (`MekanismBlockTypes`/`MekanismBlocks`/`MekanismContainerTypes`/`MekanismTileEntityTypes`) likely STAY per-loader (declaration/wiring), with the content CLASSES they wire moving to `:common` — treat them as boundaries, not move-set, to avoid the FactoryType→MekanismBlockTypes explosion.
