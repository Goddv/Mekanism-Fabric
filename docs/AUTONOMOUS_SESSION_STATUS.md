# Autonomous Session Status (user away)

Branch `26.1-fabric`, working tree clean, all work pushed to `origin/26.1-fabric`.
HEAD = `aaf4283943`. Both loaders compile green; Fabric self-tests 11/11 PASS.

## What I did this autonomous session

Mandate: "continue autonomously until you finish port." I made all the **safe,
byte-identical, both-loaders-validated** progress I could, and **stopped before**
the remaining work — which carries NeoForge-runtime risk that can only be
validated by your in-game testing. I did **not** land anything blind that could
crash NeoForge at launch.

### Commits (newest first)
| Commit | What | Files |
|---|---|---|
| `aaf4283943` | Frontier hoist (pure leaves unblocked by the reroutes) | 3 |
| `96169cc4a5` | `Mekanism`→`MekanismAPIBase` reroute (+ `rl()` made public) | 6 |
| `c0bd118603` | Base-split hoist via `*Base` static-access rewrite | 13 |
| `084eb01eac` | (prior) Registration S-BLOCKMEK-SEAM: `ISecurityPacketSender` | seam |
| `5ec3c56f81`/`983aa7e084`/`4fc6a22b81` | (prior) Frontier cascade | 64 |

**This session added 22 files to `:common`** (391 → **414** `.java`).

### The two hoist techniques used (both byte-identical for NeoForge)
1. **Same-FQN pure hoist** (`git mv`, no edits) — referencers resolve via the
   common compile classpath.
2. **Static-base reroute** — a file blocked only by a utility class that has a
   `:common` `*Base` split (`ConstantPredicates`/`EnumUtils`/`MekanismUtils`/
   `WorldUtils`/`MekanismAPI`→`*Base`, or `Mekanism`→`MekanismAPIBase`) is
   converted by rewriting `Class.member` → `ClassBase.member` (verified the
   member lives in the Base first), then hoisted. Compiles to the **same**
   `invokestatic` target — zero runtime change.

### Files I deliberately did NOT move (hidden couplings the compile gate caught)
These look loader-clean to an import scan but secretly depend on NeoForge:
- **Outlines** — NeoForge-patched 5-arg `BlockStateModel.collectParts`
- **GuiUtils / IFancyFontRenderer** — `net.minecraft.client.gui.GuiGraphicsExtractor`
  (a NeoForge-coadded class inside the `net.minecraft` namespace)
- **FuelInventorySlot** — NeoForge-patched `ItemStack.getBurnTime(RecipeType,FuelValues)`
- **LockData** — neoforge-only `ItemStackTemplate(Holder<Item>,…)` constructor
- **CrTConstants** — imports `com.blamejared.crafttweaker`

Every exclusion was caught by the compile gate and reverted cleanly — nothing
broken landed.

## Why I stopped here (the frontier is dry)

Leaf/seam hoisting is **exhausted**: 0 pure leaves and 0 clean reroute
candidates remain. The only files left in `:neoforge` that *look* movable are
blocked by genuinely-NeoForge members (e.g. `MekanismAPI.CHEMICAL_REGISTRY`,
`EnumUtils.CABLE_TIERS`, `MekanismUtils.getResource`/`ResourceType`).

The **entire remaining port** is now the registration/cycle gates documented in
`docs/REGISTRATION_GATE_PLAN.md`. Every one of these carries NeoForge **launch
risk that cannot be validated in this environment** (the classic trap: they
compile green but can crash NeoForge at construction — see the 🛑 Architectury
custom-registry timing rule). So they need you running NeoForge in-game.

## Resume plan for when you're back (do these WITH in-game NeoForge testing)
In dependency order, lowest-risk first:
1. **S-RESOURCE** — `BlockMekanism`/`BlockResource` + the 6 resource storage
   blocks via the proven `MekanismBlockRegister` (vanilla block registry →
   NeoForge-safe; skips the SECURITY/REDSTONE/UPGRADES cycle branches).
2. **S-DATACOMP** — `MekanismDataComponents` → the `:common`
   `DataComponentDeferredRegister` (proven on Fabric).
3. **S-CHEM** — chemicals, **using the RegistrarBuilder / RegisterEvent timing
   fix** (NOT the no-arg `register()` — that crashes NeoForge for custom
   registries; bytecode-verified, documented in REGISTRATION_GATE_PLAN.md).
4. **S-FLUID-SEAM** then **S-TILE/CONTAINER → S-BLOCKS/ITEMS** (the cycle core, last).

After each: `./gradlew :neoforge:assemble :fabric:jar`, then you launch NeoForge
in-game; I keep `:fabric:runServer` self-tests green on my side.

## Validation status
- All 8 NeoForge source sets + `:common` + `:fabric` compile green.
- `:fabric:runServer`: 11/11 self-tests PASS (Config, Energy, Heat,
  DataComponent, TextFoundation, Chemical, Fluid, Recipe, Power, AutoIo,
  Registration), clean `Done (…)!` boot, 0 FAIL, no exceptions.
- NeoForge in-game behavior: **byte-identical by construction** for everything
  this session (pure moves + inherited-static reroutes), but **please still
  smoke-test** a world load since I can't run NeoForge here.
