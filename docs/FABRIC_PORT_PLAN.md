# Mekanism — NeoForge → Fabric Multi-Loader Port Plan

**Status:** Phase 1 in progress — the Architectury restructure is done and the **NeoForge target fully builds** (compile + 6 jars) under Architectury Loom no-remap, replacing ModDevGradle. Branch `feature/fabric-architectury-port`.
**Approach:** Architectury multi-loader (`:common` + `:neoforge` + `:fabric`) — keep NeoForge working, add Fabric, stay mergeable with upstream Mekanism.
**Target:** Minecraft 26.1.2, Java 25.
**Source branch:** `26.1`.

---

## 1. Executive summary

This is a large but **tractable** port. The deciding factors:

1. **Toolchain is GO.** MC 26.1 is a real CalVer release and the *first fully deobfuscated* Minecraft version — Yarn/Intermediary are frozen and unnecessary; we use Mojang mappings + Parchment with a **no-remap** Loom pipeline. Every required Fabric-side component exists for 26.1 / Java 25 (see §3).
2. **Mekanism already has the abstraction seams** that make a multi-loader split feasible without rewriting game logic — energy, chemicals, capabilities, networking, and config all funnel through a small number of chokepoint classes. Roughly **80% of the codebase is loader-neutral** once those chokepoints are abstracted.
3. **The cost is concentrated**, not spread evenly: fluids (API-level `FluidStack`), capability invalidation on Fabric, a mixin suite for Forge-only game events, and custom client rendering (transmitter models, MekaSuit armor, OBJ loader) account for the majority of the work.

**Rough effort:** multi-month for one experienced multi-loader modder; the plan is structured so NeoForge keeps working at every step and Fabric comes up incrementally (first load → first working machine → fluids → full client).

**Guiding principles**
- **Concentrate loader code.** Put NeoForge/Fabric specifics behind Architectury `@ExpectPlatform` stubs and a handful of service interfaces. Keep edits to common game-logic files minimal so upstream merges stay clean.
- **Reuse Mekanism's own playbooks.** `ChemicalStack` (self-owned stack with no loader parent) is the template for a portable `FluidStack`; the `IEnergyCompat` list is the template for adding Team Reborn Energy; `BlockEnergyCapabilityCache` is the template for the Fabric capability cache.
- **Finish the in-flight 26.1 TODOs cross-loader.** Many hard client paths (OBJ loading, custom `RenderType`, item colors, `DataBasedModelLoader`) are currently stubbed `//TODO - 26.1`. Re-implement them with both loaders in mind rather than retrofitting later.
- **NeoForge is the reference loader.** When behavior is ambiguous, NeoForge defines correct; Fabric matches it.

---

## 2. Current architecture (what we're porting from)

- **Build:** `net.neoforged.moddev` (ModDevGradle) v2.0.141 — a **single Gradle project** with many source sets, *not* a multi-project build. Gradle 9.5, daemon + toolchain pinned to Java 25.
- **Modules are source sets** under `src/<name>/`: `api`, `main`, plus secondary modules `additions`, `generators`, `tools`. Extra source sets layered on each: `datagen<Module>`, `test` (JUnit 6 + jqwik), `gameTest`, and IntelliJ-only `runMain`/`runData` run-aggregator sets.
- **Scale:** ~253k LOC, ~2,430 Java files, ~692 files importing `net.neoforged.*`.
- **Entrypoints:** four `@Mod` modules, each with a paired `dist=CLIENT` `@Mod`, plus conditional integration `@Mod`s (TOP, CrafTweaker). Modern injected-constructor pattern `(ModContainer, IEventBus)`.
- **Annotation processor** (`:annotation-processor`): pure JSR-269, generates `ComputerMethodFactory`/per-module `ComputerMethodRegistry` + `META-INF/services` entries via the `-AmekanismModule=...` arg. **Loader-agnostic** — runs unchanged under Loom.
- **Custom buildSrc tasks:** `AllJar`, `MergeModuleResources` (merges ATs / `neoforge.mods.toml` / atlases / tags / services across modules), `OptimizePng`, `OutputChangelog`.
- **In-flight state:** the 26.1 branch is mid-port — `gametests.yml` push trigger is disabled "re-enable when compiles"; numerous `//TODO - 26.1` stubs exist (OBJ loader, RenderType, item colors, fluid `legacy_fluid` capability placeholder). **Assume the NeoForge baseline does not cleanly build yet.**

---

## 3. Toolchain & versions (GO / NO-GO) — CONFIRMED

**Verdict: GO.** Pins below are confirmed against live Maven/Modrinth/Fabric metadata (June 2026); locked into `gradle.properties` during Phase 1.

| Component | Pin | Note |
|---|---|---|
| Minecraft | `26.1.2` | real CalVer release; **first deobfuscated MC** (no Yarn/Intermediary). |
| Java | `25` | required by 26.1; CI + Gradle toolchain already on it. |
| NeoForge (current) | `26.1.2.73` | via MDG 2.0.141 today; MDG is replaced by Architectury Loom in Phase 1. |
| **Architectury Loom** | `1.14.476` | latest on `maven.architectury.dev`; the only Loom fork doing NeoForge. Wraps Fabric Loom in **no-remap** mode for deobf 26.1. |
| **architectury-plugin** | `3.5.166` | `@ExpectPlatform` bytecode transform; convention `a.b.c.<platform>.FooImpl`. |
| **Architectury API** | `20.0.5` | dual-loader `20.0.5+fabric` / `20.0.5+neoforge` for 26.1.2. |
| **Fabric Loader** | `0.19.3` | latest stable. |
| **Fabric API** | `0.150.0+26.1.2` | latest for 26.1.2. |
| **Team Reborn Energy** | `5.0.0` | built for MC 26.1 / loader 0.18.4 / fabric-api 0.140.3+26.1 — Fabric energy bridge. |
| Gradle | `9.5` | already in the wrapper. |

**Landmine (live):** 26.1 is deobfuscated, so the build must use the **no-remap** path — Architectury Loom 1.14.476 (wrapping Fabric Loom) with Mojang + Parchment mappings, **no Yarn**. Any pre-26.1 Architectury guide assuming Yarn remapping is wrong.

> On-machine validation (archloom 1.14.476 building a 26.1 dual-loader skeleton) is the bridge task into Phase 1 — scaffolded at `/tmp/mek-archloom-probe`.

---

## 4. Target project structure

```
Mekanism-Fabric/
├── settings.gradle           # include('common','neoforge','fabric','annotation-processor')
├── build.gradle              # root shell: architectury-plugin + dev.architectury.loom on subprojects
├── gradle.properties         # + fabric_loader, fabric_api, architectury, trenergy versions
├── annotation-processor/     # unchanged (pure JSR-269)
├── buildSrc/                 # AllJar/MergeModuleResources re-pointed at Loom remapped outputs
├── common/                   # loader-neutral: api + main game logic + datagen + JUnit tests
│   └── src/{main,api,datagen,test}/...   # @ExpectPlatform stubs for loader specifics
├── neoforge/                 # NeoForge entrypoints, AT files, neoforge.mods.toml, gameTest,
│   └── src/main/...          #   *Impl classes, capability registration, FluidType, ModConfigSpec
└── fabric/                   # Fabric entrypoints, fabric.mod.json, Team Reborn bridge,
    └── src/main/...          #   *Impl classes, BlockApiLookup wiring, mixins, accesswidener
```

**Source-set → project mapping**
- `src/api`, `src/main` → `:common` (loader specifics extracted behind `@ExpectPlatform`/services).
- `src/datagen/*` → `:common` (shared) with loader-specific generators in platforms as needed.
- `src/test` (JUnit/jqwik) → `:common`.
- `src/gameTest` → `:neoforge` (gametest is a Forge/Neo concept; Fabric uses its own harness if desired).
- Secondary modules (`additions`/`generators`/`tools`) — each split common/neoforge/fabric **or** kept as additional source sets within each platform project. This 3 loaders × 4 modules × {main,datagen} matrix is the largest mechanical chunk; decide the exact shape in Phase 1.
- Delete `runMain`/`runData` marker sets — Loom generates run configs.

**Loader-coupled build pieces**
- **Access Transformers** (4 `.cfg` files) → NeoForge keeps ATs; Fabric needs equivalent `accesswidener` files (manual translation).
- `META-INF/neoforge.mods.toml` → NeoForge only; add `fabric.mod.json` per module on Fabric.
- Publish blocks (`addModLoader('NeoForge')`) → duplicate per loader with correct loader tags to the same CurseForge/Modrinth projects.

---

## 5. System-by-system migration reference

| System | NeoForge today | Fabric/Architectury target | Existing seam (leverage) | Difficulty |
|---|---|---|---|---|
| **Registration** | `MekanismDeferredRegister extends DeferredRegister`, `MekanismDeferredHolder` | Architectury `DeferredRegister`/`RegistrySupplier` behind the same facade | `registration/` core wrappers — retarget once, ~80% of 262 sites unchanged | Low–Med |
| **Custom registries** (chemical, module, chemical_ingredient_type) w/ client sync | `RegistryBuilder(...).sync(true)` + `NewRegistryEvent` | `FabricRegistryBuilder.createSimple(...).buildAndRegister()` + sync; reconcile defaulted registry | `MekanismAPI` (`src/api`) → service split | **High** |
| **Datapack registry** (robit_skin) | `DataPackRegistryEvent.NewRegistry(direct, network, builder)` | Fabric `DynamicRegistries.register` / `registerSynced` | `DatapackDeferredRegister` | Med |
| **Data components** | custom `DataComponentDeferredRegister` (~65 sites) | Architectury DeferredRegister of `DATA_COMPONENT_TYPE` | already custom wrapper | Low |
| **Data attachments** | `neoforge.attachment.AttachmentType` (6 sites) | Fabric `AttachmentRegistry` / Architectury shim | `MekanismAttachmentTypes` | Low–Med |
| **Data maps** | `registries.datamaps.DataMapType` (6 types) | **No Fabric equiv** → reload listener / datapack JSON | `DataMapTypeRegister` | Med |
| **Capabilities (block)** | `BlockCapability<H,Direction>` + `RegisterCapabilitiesEvent` | `BlockApiLookup<H,Direction>` registered eagerly at init | `MultiTypeCapability`, `CapabilityCache`, resolvers, `WorldUtils.getCapability` (~6 chokepoints) | **High** |
| **Capability cache + invalidation** | `BlockCapabilityCache` w/ invalidation listener; `level.invalidateCapabilities(pos)` | `BlockApiCache` + **hand-built push-invalidation bus** (no Fabric analog) | `BlockEnergyCapabilityCache` (their own copy = template) | **Critical** |
| **Bounding-block / multiblock proxy** | `event.setProxyable(...)`, `TileEntityBoundingBlock.proxyCapability`, `IOffsetCapability` | `BlockApiLookup` fallback providers resolving controller | `Capabilities.registerCapabilities` | Med–High |
| **Item capabilities** | `ItemCapability` + `transfer.access.ItemAccess` (50 files) | `ItemApiLookup` + `ContainerItemContext` (model mismatch) | `ContainerType`, `ChemicalUtil` | Med |
| **Energy** | new `transfer.energy` API, `long` Joules, `IStrictEnergyHandler`, FE compat | add `TeamRebornEnergyCompat` to the compat list; expose TR/E `EnergyStorage`; 1 J = 1 E default | `IEnergyCompat` list + `EnergyCompatUtils` (zero-Neo API) | **Low–Med** |
| **Fluid stack** | `IExtendedFluidTank extends IFluidTank`, `FluidStack` in 183 files | self-owned portable `FluidStack` (or `dev.architectury.fluid.FluidStack`); strip NeoForge supertypes from API | `ChemicalStack` playbook | **Highest** |
| **Fluid registration** | `FluidType`, `BaseFlowingFluid`, `NeoForgeRegistries.FLUID_TYPES` | no `FluidType` — `FluidVariantAttributes` + per-loader split | `FluidDeferredRegister` | High |
| **Fluid units** | 1000 mB/bucket (`int`) | 81000 droplets/bucket (`long`) — ×81 exact, ÷81 lossy → clamp like FE bridge | energy rounding pattern | Med–High |
| **Networking** | `PayloadRegistrar` + `RegisterPayloadHandlersEvent`; 57 packet records | Architectury `NetworkManager.registerReceiver` / `sendTo*` | `BasePacketHandler.PacketRegistrar`, `PacketUtils` (records portable) | Low–Med |
| **Config-phase packets** | `RegisterConfigurationTasksEvent` | **No equiv** → move to play-phase on join, or mixin | `PacketHandler` | Med |
| **Mod lifecycle events** | mod bus (`FMLCommonSetupEvent`, register events) | `ModInitializer`/`ClientModInitializer` + Architectury registries | per-`@Mod` constructors | Low–Med |
| **Game events (mappable)** | tick/lifecycle/login/commands/right-click/death/reload | Architectury `dev.architectury.event.*` | `CommonPlayerTracker`, tick handlers | Low–Med |
| **Game events (NO Fabric equiv)** | damage/invuln/fall/jump/break-speed/attribute-modifier/entity-tick/break-block/finalize-spawn | **mixin suite** on `LivingEntity`/`Player`/`Block` | `CommonPlayerTickHandler`, `CommonWorldTickHandler` | **High** |
| **Config** | `ModConfigSpec` + `ModConfig` | keep on NeoForge; Fabric backs the `Cached*Value` layer with night-config directly | `IMekanismConfig`/`BaseMekanismConfig`/`Cached*Value` | Med |
| **GUI / screens** | `RegisterMenuScreensEvent`; near-vanilla base classes | `MenuScreens.register` / Architectury `MenuRegistry`; extended menu → `registerExtended` | `GuiMekanism`→`AbstractContainerScreen`, `GuiElement`→`AbstractWidget` | Low (extended-data: Med) |
| **Renderers (BER/entity/layers)** | `EntityRenderersEvent.*` | Fabric `BlockEntityRendererFactories`, `EntityRendererRegistry`, `EntityModelLayerRegistry`, feature-renderer callback | `ClientRegistration` | Med |
| **Custom block models** (transmitters, energy cube) | `DynamicBlockStateModel` + per-pos `ModelData` | `fabric-renderer-api` / `ModelLoadingPlugin` + `RenderAttachmentBlockEntity` | `TransmitterBlockStateModel` (389 ln) | **High** |
| **Armor/gear rendering** (MekaSuit, jetpack…) | `IClientItemExtensions` / `ISpecialGear` | Fabric `ArmorRenderer.register` / `HumanoidArmorLayer` mixins | `ISpecialGear`, `MekanismArmorLayer` | **High** |
| **OBJ models** | NeoForge `ObjLoader` (currently commented out) | **bundle an OBJ→quad loader** (no Fabric OBJ loader) | `MekanismModelCache` | High |
| **Color handlers** | new `BlockTintSource`/`ItemTintSource` codec system | classic `ColorProviderRegistry` per loader / Architectury `ColorHandlerRegistry` | `ClientRegistration` | Med |
| **Fluid rendering** | `IClientFluidTypeExtensions`, `RegisterFluidModelsEvent` | `FluidRenderHandlerRegistry` + fog mixins | `ClientRegistrationUtil.registerFluidExtensions` | Med–High |
| **HUD / item decorators** | `RegisterGuiLayersEvent`/`GuiLayer`, `IItemDecorator` | `HudLayerRegistrationCallback`/`HudRenderCallback`, `ItemRenderEvents`/mixin | `registerOverlays` | Med |

---

## 6. Phased execution plan

Each phase keeps **NeoForge building** and adds Fabric capability incrementally. Phases are ordered by dependency; some sub-streams within a phase can parallelize.

### Phase 0 — Toolchain spike & baseline (de-risk first)
**Goal:** prove the bleeding-edge toolchain and get a green starting point.
- Confirm the exact **Architectury Loom** version from `archloom-example-mod` (branch `architectury-loom`); stand up a throwaway dual-loader hello-world on MC 26.1 / Java 25 with the no-remap Loom and confirm `runClient` works on **both** loaders.
- Get the **existing NeoForge baseline to compile** (resolve the `//TODO - 26.1` blockers enough to build; re-enable gametests when green).
- Lock all dependency versions in `gradle.properties`.
**Exit:** NeoForge builds clean; a separate Architectury hello-world runs on Fabric + NeoForge 26.1.

### Phase 1 — Gradle restructure to Architectury
**Goal:** convert the single MDG project into `:common`/`:neoforge`/`:fabric` with **NeoForge fully working through Architectury Loom** (no Fabric game code yet).
- New `settings.gradle` + root/sub `build.gradle`; swap MDG → architectury-plugin + Architectury Loom.
- Move source sets into projects per §4; decide secondary-module shape (split vs. in-platform source sets).
- Re-plumb the annotation processor onto `:common` with the `-AmekanismModule` arg; ensure generated `META-INF/services` land in each platform jar (update `MergeModuleResources`).
- Carry ATs + `neoforge.mods.toml` to `:neoforge`; wire datagen/gametest/test runs.
- Keep buildSrc tasks working against Loom remapped outputs.
**Exit:** `:neoforge` produces the same working jars as before, built via Architectury. `:fabric` exists and compiles an empty mod.

### Phase 2 — Platform abstraction layer (NeoForge side only)
**Goal:** introduce the `@ExpectPlatform`/service seams; implement the NeoForge side (mostly wrapping existing code). **No behavior change on NeoForge.**
- Seams for: registration facade, capability/lookup token + cache + invalidation, energy compat registration, network registrar + send, event registration, config registration + paths (`FMLPaths`), IMC, fluid registration, model/render registration.
- Move `MekanismAPI` custom-registry creation behind a service.
- This phase is where the architecture is *designed*; getting the seams right minimizes later churn.
**Exit:** NeoForge runs exactly as before, but all loader-specific calls route through abstractions.

### Phase 3 — Fabric bring-up (it loads)
**Goal:** minimal Fabric mod loads in a client.
- `fabric.mod.json` per module (main + client entrypoints; conditional integrations via `FabricLoader.isModLoaded`); `ModInitializer`/`ClientModInitializer` implementations.
- Registration via Architectury `DeferredRegister`; custom registries via `FabricRegistryBuilder` (+ sync); data components/attachments.
- Config service backed by night-config; accesswidener files.
**Exit:** Fabric client launches with all Mekanism blocks/items/creative tabs registered and placeable (no machine logic yet).

### Phase 4 — Core transfer systems on Fabric (first working machine)
**Goal:** energy/chemical/heat/item transfer functional on Fabric.
- Capabilities → `BlockApiLookup`/`ItemApiLookup`/`EntityApiLookup`; eager registration.
- **Rebuild the capability cache + invalidation bus** on Fabric (port `BlockEnergyCapabilityCache` pattern; add neighbor/BE-load notification since Fabric lacks `invalidateCapabilities`). This unblocks transmitter networks.
- `TeamRebornEnergyCompat` in the `IEnergyCompat` list; bidirectional Joules↔TR/E with simulate-clamp-execute.
- Chemical + heat lookups; item-handler exposure via `ContainerItemContext`.
- Bounding-block/multiblock proxy via fallback providers.
**Exit:** an energy cube charges, a basic machine accepts energy + chemicals and processes on Fabric; a cable/pipe network transfers.

### Phase 5 — Fluids
**Goal:** the highest-cost system.
- Replace API-level `FluidStack` and the `extends IFluidTank/IFluidHandler` supertypes with a **portable fluid stack** (ChemicalStack playbook) across the 183-file surface; provide NeoForge `FluidStack`↔portable adapters at the loader boundary.
- Split fluid **registration**: NeoForge keeps `FluidType`/`BaseFlowingFluid`; Fabric uses `FluidVariantAttributes` + flowing-fluid setup.
- Droplet↔mB conversion with clamping; fluid item interactions (`FluidStorage.ITEM`/`ContainerItemContext`).
**Exit:** fluid tanks, pipes, and fluid-using machines work on both loaders; `FluidStackIngredient` recipes resolve.

### Phase 6 — Networking & events
**Goal:** packets + game-event behavior on Fabric.
- `BasePacketHandler`/`PacketUtils` → Architectury `NetworkManager`; reimplement transmitter-range bundling; relocate config-phase packets to play-phase.
- Mappable events → Architectury events / Fabric callbacks; `@EventBusSubscriber` auto-registration → explicit registration from initializers (audit so none are silently missed).
- **Mixin suite** for Forge-only events: `EntityInvulnerabilityCheck`/`LivingIncomingDamage`/`LivingShieldBlock`, `LivingFall`, `LivingJump`, `BreakSpeed`, `ItemAttributeModifier`, `EntityTick` (radiation), `BreakBlock`/`BlockDrops`, `FinalizeSpawn`.
- Custom Mekanism events (`EnergyTransferEvent` etc.) → Architectury `Event<>` or direct calls.
**Exit:** MekaSuit damage absorption, jetpack flight, fall negation, dig-speed mods, conditional gear attributes, and all client/server packets work on Fabric.

### Phase 7 — Client & rendering
**Goal:** full client parity. Sequence easy→hard.
- Screens/menus first (near-vanilla) via `MenuRegistry`; keybinds, particles, sounds.
- BER/entity renderers/layers; HUD overlays; color handlers (revert to classic providers per loader if needed).
- **Hard:** custom block models (transmitter `DynamicBlockStateModel` + per-pos data → Fabric renderer API / render-attachment); energy-cube geometry; **OBJ loader** (bundle one); **MekaSuit/armor** rendering (`ArmorRenderer`/mixins); fluid rendering + fog; special item renderers; item-model properties; custom texture atlas (Robit).
- Finish the `//TODO - 26.1` client stubs cross-loader as part of this.
**Exit:** visual parity on Fabric for blocks, transmitters, machines, GUIs, armor, fluids, HUD.

### Phase 8 — Secondary modules
**Goal:** apply the patterns to `generators`, `tools`, `additions`.
- Mostly mechanical reuse of Phases 2–7 seams.
- `additions` needs Fabric equivalents for NeoForge `BiomeModifier`/`StructureModifier` (its datapack-serializer registries) — Fabric biome modification API / mixins.
- Per-module packet handlers (generators), client registration, datagen.
**Exit:** all four content modules functional on both loaders.

### Phase 9 — Datagen, tests, integrations, polish
- Datagen runs on both loaders (or NeoForge-authoritative + verified parity); regenerate assets/data.
- JUnit/jqwik in `:common`; gametests on NeoForge; optional Fabric test harness.
- Integrations: JEI/EMI plugins are loader-neutral; verify Curios (Fabric: Trinkets/accessories bridge), TOP, CrafTweaker, Jade/WTHIT, AE2, CC:Tweaked per availability.
- Performance pass on the Fabric capability-cache/invalidation hot paths.
**Exit:** green tests; integrations working where the dependency exists on Fabric.

### Phase 10 — Build, publish, CI
- Dual-loader artifacts; `addModLoader('NeoForge')` + `'Fabric'`; per-loader uploads to existing CurseForge/Modrinth projects with correct loader tags.
- CI: build/test both loaders; gametests (NeoForge); publish workflow per loader.
**Exit:** reproducible dual-loader release pipeline.

---

## 7. Risk register (ranked)

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| 1 | **Capability invalidation gap on Fabric** (no `invalidateCapabilities`; `BlockApiCache` has no listener) | Transmitter networks / multiblocks break or thrash | Port `BlockEnergyCapabilityCache` pattern into a Mekanism-owned invalidation bus (neighbor + BE-load notifications). Prototype in Phase 4 before scaling. |
| 2 | **`FluidStack` welded into the public API** (183 files, supertype constraints) | Touches recipes, tanks, pipes, GUIs, addon API | Self-owned portable fluid stack (ChemicalStack playbook) + boundary adapters; do it as one decisive API substitution in Phase 5, not piecemeal. |
| 3 | **Forge-only game events** (damage/fall/jump/break-speed/attribute) | Core gameplay (MekaSuit, tools) | Dedicated mixin suite; treat as first-class deliverable in Phase 6; cross-check each against NeoForge behavior. |
| 4 | **Custom client models + MekaSuit + OBJ loader** | Visual parity; large rewrite | Sequence last (Phase 7); bundle an OBJ loader; lean on the fact these are already mid-migration so design cross-loader now. |
| 5 | **Custom registries with client sync** (chemical/module) | The mod's identity | Reconcile defaulted-registry + sync semantics early (Phase 3) with a focused spike. |
| 6 | **Architectury Loom 26.1 version unconfirmed** | Blocks the whole build | Resolve in Phase 0 from the live example repo before committing build files. |
| 7 | **NeoForge baseline may not compile** (mid-26.1) | False "regression" signals | Establish green baseline in Phase 0; tag it as the comparison reference. |
| 8 | **Secondary-module × loader matrix** | Mechanical sprawl | Decide module shape in Phase 1; script the repetitive splits. |
| 9 | **Upstream merge drift** (we track upstream Mekanism) | Maintenance cost | Concentrate loader code in platform projects + chokepoints; keep common files close to upstream. |
| 10 | **NeoForge-only subsystems** (data maps, chunk-ticket controllers, biome/structure modifiers) | Niche features | Per-feature Fabric reimplementation; isolate behind services; some may ship NeoForge-first. |

---

## 8. Open decisions to confirm

1. **Secondary-module layout** — split each into common/neoforge/fabric, or keep as in-platform source sets? (Phase 1)
2. **Fluid stack type** — Mekanism-owned portable `FluidStack` vs. `dev.architectury.fluid.FluidStack`? (Phase 5; leaning Mekanism-owned for API control, matching chemicals.)
3. **Fabric accessories** — bridge Curios usage to Trinkets/accessories, or drop curio integration on Fabric initially? (Phase 9)
4. **Datagen authority** — NeoForge-authoritative with parity checks, or fully dual? (Phase 9)
5. **Release cadence** — ship Fabric alpha after Phase 4/5 (core works) or hold for full client parity (Phase 7)?

---

## 9. Status & next step

**Phase 0 — DONE.**
- Toolchain confirmed **GO**, all versions pinned (§3).
- The **full NeoForge baseline compiles green** on this machine — every source set (api, main, additions, generators, tools + datagen + test + gameTest) builds; warnings only. The dominant warnings are the deprecated `net.neoforged.neoforge.fluids.capability` API and the `legacy_fluid` capability placeholder — i.e. the Phase 5 fluid surface. The CI "gametests disabled until it compiles" note is stale for *compilation* (gameTest compiles; whether gametests *pass* at runtime is unverified).
- A throwaway Architectury 26.1 dual-loader skeleton (`/tmp/mek-archloom-probe`) validates archloom 1.14.476 end-to-end and serves as the verified build-file template for Phase 1.

**Phase 1 — NeoForge build conversion DONE (neoforge-first).** The MDG single-project is restructured into Architectury `:common`/`:neoforge`/`:fabric` (no-remap, loom `1.14-SNAPSHOT`). All Mekanism source moved to `neoforge/src/`; **`:neoforge` compiles all 13 source sets and builds all 6 jars** — the annotation processor, the 4 classic-FML access transformers (via `loom.neoForge.accessTransformer`), and `mergeModuleResources`/`allJar` all work. Loom run configs (client/server/data/gameTest) are wired and configure cleanly. `:common`/`:fabric` are minimal stubs.

Build fixes applied (root `build.gradle`/`settings.gradle`, `neoforge/build.gradle`, `buildSrc/build.gradle`): plugins declared at root + applied per-subproject (so Architectury sees Loom on a shared classloader); `buildSrc` gson bumped to 2.13.2 (Java-25 final-field reflection); `minecraft`+`neoForge` deps + NeoForged repo; disabled-integration deps gated (CrT) + dogforce repo broadened for `rhinolib`; extra source sets extended from loom's `minecraftNamed*`/`forge*` configs in `afterEvaluate`; `logo.png` repointed to root.

**Remaining in Phase 1 / prerequisites for later phases:**
- **Dev runs blocked by an Architectury-Loom (no-remap, beta) NeoForge limitation** — `runClient/runServer/runData/runGameTest*` fail at JVM launch with `ClassNotFoundException: dev.architectury.transformer.TransformerRuntime` (same family as [archloom #298](https://github.com/architectury/architectury-loom/issues/298), NeoForge's `BootstrapLauncher` removal). Blocks all in-dev runs/datagen/gametests, but is **orthogonal to the port** (the built jars are correct) and only matters once Phase-2 `@ExpectPlatform` code needs dev-running. Revisit when archloom no-remap updates (or pin a compatible NeoForge).
- **Datagen run wiring (deferred; also blocked by the above):** re-point the loom `data` run from the IDE-only `runData` to `sourceSets.datagenMain`, and put `configurations.datagenNonMod` (yaml-ops) on the datagen source sets' runtime classpath (replaces MDG's `dataAdditionalRuntimeClasspath`).

**Next: Phase 2** — hoist loader-neutral code from `:neoforge` into `:common` behind `@ExpectPlatform`/services; then Phase 3 bring up `:fabric`. Work is on branch `feature/fabric-architectury-port`.

### Phase 2 — energy vertical slice (started)

Chosen as the first vertical slice (cleanest API + a pluggable `IEnergyCompat`). Ordered steps + status:

- ✅ **Energy API decoupled from NeoForge `FluidStack`** — removed the javadoc-only `{@link IFluidHandler#fill(FluidStack,…)}` references + imports from `IStrictEnergyHandler`, `ISidedStrictEnergyHandler`, `IEnergyContainer`. The energy API's ONLY remaining NeoForge coupling is `IEnergyContainer extends net.neoforged.neoforge.common.util.ValueIOSerializable` (a serialize/deserialize contract over **vanilla** `ValueOutput`/`ValueInput`). `:neoforge:compileJava` verified green.
- ✅ **Abstract `ValueIOSerializable` — DONE (cross-cutting, 19 files).** Created `mekanism.api.IValueIOSerializable` in `:common` (over vanilla `ValueOutput`/`ValueInput`) and swapped all 19 references (energy/chemical/heat/fluid/inventory containers, tile components, the container-creator framework, data classes). NeoForge integration boundary handled: NeoForge-patched `ValueOutput.putChild`/`ValueInput.readChild` (10 sites) inlined to vanilla `child()` + serialize/deserialize; `MeltdownLevelData`/`RadiationLevelData`/`MultiblockManager` kept on NeoForge's `ValueIOSerializable` (consumed by `AttachmentType.serializable(...)` — a loader-neutral attachment abstraction is later Phase-2 work). Whole build (all NeoForge source sets + `:fabric`) green; `IValueIOSerializable` bundles into the NeoForge jar. **Unblocks hoisting energy + chemical/heat/fluid/inventory containers into `:common`.**
- ✅ **`:common`→`:neoforge` consumption + bundling pipeline (main jar).** `:neoforge` declares `common`/`shadowCommon` configs; every source set's compile/runtime classpath `extendsFrom common`; the main `jar` bundles `common`'s `transformProductionNeoForge` output. Proven: `mekanism.multiloader.CommonHoistProbe` (in `:common`) compiles against the common classpath and lands in `Mekanism-*.jar`. **Required `org.gradle.jvmargs=-Xmx6G`** (the full archloom build + transform OOM'd the default daemon heap). TODO: also bundle common into `apiJar`/`allJar` (add a `commonOutput` input to `AllJar.groovy`) as API code is hoisted.
- ✅ **Fabric test build loads at runtime.** A minimal `:fabric` mod (`fabric/build/libs/fabric-10.8.0.jar`, entrypoint `mekanism.fabric.MekanismFabric`, `fabric.mod.json` id `mekanism`) loads under Fabric Loader on MC 26.1 — `runServer` booted 43 mods and fired our entrypoint. **Fabric dev-runs work** (`dev.architectury.transformer.TransformerRuntime` runs fine on Fabric), unlike NeoForge dev-runs — so the energy slice can be runtime-validated on Fabric.
- ✅ **Energy API hoisted into `:common`.** api-core foundation (`Action` [clean; `FluidAction`→NeoForge `FluidActions`], `AutomationType`, `IContentsListener`, `SerializationConstants`, `annotations.*` + `jsr305`) **plus** `mekanism.api.math`, `mekanism.api.container`, and 5 of 6 `mekanism.api.energy` interfaces (`IEnergyContainer`, `IStrictEnergyHandler`, `ISidedStrictEnergyHandler`, `IMekanismStrictEnergyHandler`, `IEnergyConversion`) now compile loader-neutrally in `:common` and bundle into both loaders' jars (incl. Fabric). Build (common+neoforge+fabric) green.
- [ ] **`MekanismAPI` split** (deferred, but needed for `IEnergyConversionHelper` + later chemical/module): `MekanismAPI` is mostly loader-clean (`getService` service-locator, constants, logger, vanilla `ResourceKey` registry-name keys) — only 5 fields are NeoForge (`new RegistryBuilder<>(...)` for chemical/chemical-ingredient/module/robit-skin-serializer registries + a `DeferredHolder`). Extract the clean parts into a `:common` `MekanismAPI`; move the 5 registry instances to a platform class (`@ExpectPlatform`/service). Then hoist `IEnergyConversionHelper`.
- [ ] **Abstract energy capability expose/query** behind `@ExpectPlatform`/a service in `:common`; NeoForge impl wraps the existing `Capabilities.STRICT_ENERGY`/`BlockCapability` + the `IEnergyCompat` list.
- [ ] **Fabric side:** bring up a loadable `:fabric` (entrypoint + `fabric.mod.json`); add Team Reborn Energy (`teamreborn:energy` 5.0.0) bridge implementing the energy service via `BlockApiLookup`; Joules↔E (1:1 default) reusing `IEnergyConversion`.
- [ ] **Validate at runtime via a Fabric run** — Fabric's dev launcher differs from NeoForge's, so it may sidestep the archloom `TransformerRuntime`/`BootstrapLauncher` issue blocking NeoForge dev-runs; worth testing as soon as `:fabric` loads.
