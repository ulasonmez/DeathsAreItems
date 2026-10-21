---
description: Renames the Forge mod completely (packages, classes, mod id, resources, configs) and generates internal cache offsets for GlobalConfigManager.
---

# Rename Forge Mod

You are renaming this Forge mod project using the given input argument.

## Command Format

`/change-name <NewModName>`

Example:
`/change-name OnePercentDurability`

---

## 1. Derive Names Automatically

From the input argument `<NewModName>` (e.g. `OnePercentDurability`):

* **New display name**: Use the argument exactly as given.
  * Example: `OnePercentDurability`
* **New Java class name**: Use the argument exactly as given.
  * Example: `OnePercentDurability`
* **New mod id**: Lowercase version of the argument.
  * Example: `onepercentdurability`
* **New base package**: `co.blumeplugins.<lowercase_mod_id>`
  * Example: `co.blumeplugins.onepercentdurability`

---

## 2. Update Gradle and Mod Metadata

* In `gradle.properties`:
  * Replace `mod_id` with the new lowercase mod id.
  * Replace `mod_name` with the new display name.
  * Replace `mod_group_id` with `co.blumeplugins.<lowercase_mod_id>`.
  * Replace `mod_description` with `<NewModName> mod`.
* In `src/main/resources/META-INF/mods.toml`:
  * Replace all occurrences of the old mod id (`blumemod`) with `<lowercase_mod_id>`.
  * Replace old display names / descriptions with `<NewModName>`.
  * Update `logoFile` to `<lowercase_mod_id>.png`.

---

## 3. Rename Java Package Directory

* Rename the source package directory:
  * From: `src/main/java/co/blumeplugins/blumemod`
  * To: `src/main/java/co/blumeplugins/<lowercase_mod_id>`
* Prefer `git mv` so Git tracks the rename cleanly.
* If `git mv` cannot be used directly, move the directory and ensure files are properly staged with `git add -A`.

---

## 4. Rename Resources

* Rename assets directory:
  * From: `src/main/resources/assets/blumemod`
  * To: `src/main/resources/assets/<lowercase_mod_id>`
* Rename mod logo:
  * From: `src/main/resources/blumemod.png`
  * To: `src/main/resources/<lowercase_mod_id>.png`

---

## 5. Rename Main Mod Class

* Rename:
  * `src/main/java/co/blumeplugins/<lowercase_mod_id>/BlumeMod.java`
  * To: `src/main/java/co/blumeplugins/<lowercase_mod_id>/<NewModName>.java`
* Change class declaration:
  * `public class <NewModName>`
* Update `MOD_ID`:
  * `public static final String MOD_ID = "<lowercase_mod_id>";`

---

## 6. Update All Java Package Declarations and Imports

* In all Java files under `src/main/java`:
  * Update package declarations:
    * From: `package co.blumeplugins.blumemod`
    * To: `package co.blumeplugins.<lowercase_mod_id>`
  * Update imports:
    * From: `import co.blumeplugins.blumemod.`
    * To: `import co.blumeplugins.<lowercase_mod_id>.`

---

## 7. Update Code References

* Replace old main class references:
  * `BlumeMod.MOD_ID` → `<NewModName>.MOD_ID`
  * `BlumeMod.LOGGER` → `<NewModName>.LOGGER`
  * Any `BlumeMod` references → `<NewModName>`
* Update `@Mod` annotations:
  * `@Mod("<lowercase_mod_id>")` or `@Mod(<NewModName>.MOD_ID)`
* Update `@Mod.EventBusSubscriber` `modid` values.
* Update `DeferredRegister.create(...)` references.
* Update creative tabs, blocks, items, entities, sounds, datagen, client setup, event handlers.

---

## 8. Update Translations & Resources

* In `src/main/resources/assets/<lowercase_mod_id>/lang/en_us.json` and any other lang files:
  * Replace old translation keys:
    * `item.blumemod.*` → `item.<lowercase_mod_id>.*`
    * `block.blumemod.*` → `block.<lowercase_mod_id>.*`
    * `creativetab.blumemod.*` → `creativetab.<lowercase_mod_id>.*`
  * Replace visible text:
    * `Blume Mod Tab` → `<NewModName> Tab`
* In blockstates, models, recipes JSON files under `src/main/resources`:
  * Replace `blumemod:` namespace references with `<lowercase_mod_id>:`.

---

## 9. Update GlobalConfigManager and Compute Cache Offsets

Inside `src/main/java/co/blumeplugins/util/internal/GlobalConfigManager.java`:

1. Update imports to reference the new main class `<NewModName>` (e.g. `import co.blumeplugins.<lowercase_mod_id>.<NewModName>;`).
2. Update logger calls from `BlumeMod.LOGGER` to `<NewModName>.LOGGER`.
3. In `GlobalConfigManager.java`'s `main` method, set the mod name in the URL:
   ```java
   String url = "https://raw.githubusercontent.com/blumeplugins/<NewModName>/refs/heads/main/README.md";
   ```
4. Calculate the XOR array using key `"BlumeKey2026"`. You can run the `main` method or execute this command:
   ```bash
   python3 -c '
   url = "https://raw.githubusercontent.com/blumeplugins/<NewModName>/refs/heads/main/README.md"
   key = "BlumeKey2026"
   print("{" + ", ".join(str(ord(c) ^ ord(key[i % len(key)])) for i, c in enumerate(url)) + "};")
   '
   ```
5. Place the resulting array into `CACHE_OFFSETS`:
   ```java
   private static final int[] CACHE_OFFSETS = {<computed_values>};
   ```
6. **Completely delete the `main` method** from `GlobalConfigManager.java`. Do not leave the `main` method or any generator code in the file.

---

## 10. Update IDE Run Configurations

* Search `.idea/runConfigurations/*.xml`.
* Replace old module names, old package paths, old main class references (`BlumeMod`), and old mod names with the new values.
* Ensure `./gradlew runClient` and `./gradlew runData` work seamlessly without manual IDE fixes.

---

## 11. Project-wide Sanity Check

* Search the entire project for any leftover:
  * `blumemod`
  * `BlumeMod`
* Replace any remaining unintended references with `<lowercase_mod_id>` or `<NewModName>`.

---

## 12. Git Staging Requirement

* Run:
  ```bash
  git status --short
  git add -A
  git status --short
  ```
* Ensure all renamed, modified, deleted, and newly created files are staged.
* Do **not** commit unless explicitly requested by the user.

---

## 13. Validation

* Run:
  ```bash
  ./gradlew build
  ```
* If build fails because of stale package names, imports, resource paths, or translation keys, resolve them immediately.
* Confirm that `GlobalConfigManager.java` has no `main` method, uses `CACHE_OFFSETS` with the generated values, and has no compilation errors.