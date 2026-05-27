# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

- **Build mod jar:** `./gradlew build` (output in `build/libs/`)
- **Generate IDE runs (IntelliJ):** `./gradlew genIntellijRuns`
- **Generate IDE runs (Eclipse):** `./gradlew genEclipseRuns`
- **Refresh dependencies:** `./gradlew --refresh-dependencies`
- **Clean build:** `./gradlew clean`
- **Run client:** After `genIntellijRuns`, use the generated IntelliJ run config, or `./gradlew runClient` (if configured)
- **Run GameTests:** `./gradlew runGameTestServer`
- **Run data generation:** `./gradlew runData`
- **Check Java version:** The project targets Java 17, with ForgeGradle using a Java toolchain

## Project Structure

This is a Minecraft Forge mod (MDK template) for Minecraft 1.20.1 with Forge 47.4.10.

```
src/main/java/com/example/examplemod/
├── ExampleMod.java    # Main mod class annotated with @Mod
├── Config.java        # ForgeConfigSpec configuration handler

src/main/resources/
├── META-INF/mods.toml  # Mod metadata (modid, version, dependencies)
├── pack.mcmeta         # Resource pack metadata
```

### Key Architecture

- **Entry point:** `ExampleMod` (annotated with `@Mod("examplemod")`) registers blocks, items, and creative tabs via `DeferredRegister` in the constructor, which subscribes to the mod event bus
- **Registration pattern:** Uses `DeferredRegister` with `ForgeRegistries` for blocks/items, and Vanilla `Registries` for creative tabs. Register the `DeferredRegister` on the mod event bus in the constructor
- **Config:** `Config.java` uses `ForgeConfigSpec.Builder` to define config values, loaded via `ModConfigEvent` subscriber. Config types are registered in the main mod constructor via `context.registerConfig()`
- **Events:** Forge event bus (`MinecraftForge.EVENT_BUS`) for game events, mod event bus (`IEventBus` via `FMLJavaModLoadingContext`) for lifecycle events
- **Client events:** Handled via a static inner class `ClientModEvents` with `@Mod.EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT)`
- **Data generation:** Run `./gradlew runData` to auto-generate resources into `src/generated/resources/`
- **Resources:** `mods.toml` uses Gradle property expansion (`${mod_id}`, `${mod_version}`, etc.) defined in `gradle.properties`

### gradle.properties (Mod Identity)

Change these for a new mod: `mod_id`, `mod_name`, `mod_version`, `mod_group_id` (must match the package in `@Mod` annotation), `mod_authors`, `mod_description`.
