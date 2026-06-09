# Config Gate Plan (MekanismConfig → loader-neutral)

The gate blocking the GUI/slot/container layer (`MekanismContainer` → slot classes → `IInsertableSlot` → `SelectedWindowData` → `MekanismConfig`), `TileEntityMekanism`'s hoist (`MekanismConfig.general.blockDeactivationDelay`, `client.enableMachineSounds`), and ~33 of the 381-closure files. From the `config-gate-plan` workflow (5 agents, adversarially verified).

## The ONE seam (verified by grep)
Every `Cached*Value` touches NeoForge through exactly the `internal` field (`ModConfigSpec.ConfigValue<T>`), on which the ONLY ops anywhere are `internal.get()` (8×), `internal.getDefault()` (8×), `internal.set(T)` (9×). Spec-BUILDING (`ModConfigSpec.Builder` define/push/pop/comment) is separately isolated to the 9 config-class ctors + 2 static helpers (`CachedLongValue.define*`, `CachedOredictionificatorConfigValue.define`).

So: introduce `:common` `IConfigValue<T> { T get(); T getDefault(); void set(T); }`; retype `CachedValue.internal` to it. The 13 `Cached*Value` classes stay CONCRETE (478+ call sites + bare-supplier consumers depend on their types: `CachedIntValue` IS-A `IntSupplier`+`LongSupplier`, `CachedLongValue` IS-A `LongSupplier`, `CachedFloatValue` IS-A `FloatSupplier`) — only the `internal` field type flips. All accessor bodies (get/getOrDefault/getAs*/set/clearCachedValue/resolve/encode) become pure Java → movable to `:common` wholesale.

## Design
- **Read abstraction:** `IConfigValue<T>` (:common). NeoForge `NeoConfigValue<T>` wraps `ModConfigSpec.ConfigValue<T>` (3-line passthrough). Fabric `DefaultConfigValue<T>` holds default + in-memory current (defaults-first).
- **MekanismConfig.X.Y access:** TYPED FIELDS, not a facade — the 9 config instances + their `CachedXValue` fields move to `:common`, so `MekanismConfig.general.X.get()` and bare-supplier passes both keep working.
- **Spec-build seam:** `:common` `IConfigBuilder` abstracts the `ModConfigSpec.Builder` surface (define/defineInRange/defineEnum/defineListAllowEmpty/push/pop/comment/translation/worldRestart/gameRestart/build), each define* returning an `IConfigValue<T>`. NeoForge impl delegates to `ModConfigSpec.Builder` (identical .toml). Fabric impl no-ops comment/push/pop/restart, records default, returns `DefaultConfigValue`.
- **IMekanismConfig split:** `:common` keeps read-path members (isLoaded/addCachedValue/getFileName/getTranslation/save/clearCache); NeoForge `INeoMekanismConfig` adds getConfigSpec/getConfigType (persistence).
- **Fabric defaults-first:** `getOrDefault()` already falls back to `getDefault()` when `!isLoaded()`; Fabric `isLoaded()=true` + `DefaultConfigValue.get()==getDefault()` → both read paths return declared defaults with ZERO edits to the 13 value classes. `set()` is in-memory, `save()` no-op (known Fabric limitation: client GUI config edits reset on restart until a real Fabric config impl lands behind the same interfaces). NeoForge `.toml` byte-identical (NeoConfigValue IS ModConfigSpec.ConfigValue verbatim; Builder calls untouched).

## Slice order (each green, NeoForge byte-identical)
- **M1 (NeoForge-only proof):** create `:common` `IConfigValue<T>` + NeoForge `NeoConfigValue<T>`; retype `CachedValue.internal` + the 13 value classes' `internal`/ctor/primary-`wrap` to `IConfigValue<T>`; **add a transitional NeoForge-only `wrap(IMekanismConfig, ModConfigSpec.ConfigValue<T>)` overload to each that does `return wrap(config, new NeoConfigValue<>(cv))`** → the ~279 caller `wrap(config, builder.defineX(...))` sites + the 2 define-helpers are UNCHANGED (their `ConfigValue` hits the transitional overload). Touches ~15 files (2 new + 13 value classes), 0 caller churn. (The transitional overloads are deleted in M3.) ← executing.
- **M2:** split `IMekanismConfig` → `:common` read-interface + NeoForge `INeoMekanismConfig`.
- **M3:** MOVE the 13 value classes → `:common` (delete the transitional overloads; migrate the ~279 callers to explicit `new NeoConfigValue<>(...)` OR route define* through M4's IConfigBuilder first). Keep `CachedLongValue.define*`/`Oredictionificator.define` ModConfigSpec refs as NeoForge shims until M4.
- **M4:** `:common` `IConfigBuilder` seam + NeoForge impl wrapping `ModConfigSpec.Builder`; retarget `IConfigTranslation.applyToBuilder`, the define-helpers, `WorldConfig.OreConfig/SaltConfig`, `ClientConfig.ConfigSaveData`.
- **M5:** MOVE the 9 config classes + `MekanismConfig` facade + `BaseMekanismConfig` → `:common` (ctors build via `IConfigBuilder`); keep registerConfigs/onConfigLoad/`MekanismConfigHelper` on NeoForge / behind `INeoMekanismConfig`. Unblocks `SelectedWindowData` → the slot layer → `MekanismContainer`, + `TileEntityMekanism`'s config refs.
- **M6 (Fabric):** `DefaultConfigValue` + Fabric `IConfigBuilder` (defaults-first) + Fabric `IMekanismConfig` impls + Fabric entrypoint instantiating the 9 configs; register via META-INF/services.

## Risks
- `CachedEnumValue.wrap` is bounded `& TranslatableEnum` + takes `ModConfigSpec.EnumValue<T>` (build-time only); relocate that to `IConfigBuilder.defineEnum` at M4 — don't move CachedEnumValue to :common before M4 (or keep a NeoForge wrap shim).
- `CachedMapConfigValue.internal` is `ConfigValue<List<? extends String>>` (wildcard) — verify generic capture flows through `NeoConfigValue` (same shape compiles today).
- ~279 wrap sites across **20** files incl. additions/generators/tools + `world.height` (ConfigurableVerticalAnchor/HeightRange) + `mekanism.tools.common` MaterialCreator/VanillaPaxelMaterialCreator (30+5, easy to miss — NOT in a config dir) — the transitional overload sidesteps these in M1; M3 must cover all 20.
- 15 client-GUI `.set()`+`save()` sites: Fabric in-memory/no-op (behavioral diff, Fabric-only).
- `WorldConfig` (nested OreConfig/SaltConfig push/pop over `EnumUtils.ORE_TYPES`) is the hardest M4/M5 ctor — preserve push/pop structure for .toml identity; verify byte-diff.
- `FluidType.BUCKET_VOLUME` (=1000) default constant in GeneralConfig → replace with literal/`:common` constant at M5.
- `isLoaded()=true` on Fabric drives `getOrDefault` + clearCache listener path — verify no NPE on the never-reloaded path.
