# EMI Contribution Handoff — Adding Suppression API

## Project Context
EMI is a mod item browser for NeoForge/Fabric that provides recipe viewing and item searching. AMI (Automated Materials Index) is a competing fullscreen inventory overlay that wants to completely hide EMI's UI when active. Currently, AMI uses fragile mixin-based workarounds to suppress EMI rendering. This handoff describes how to add a **public, stable suppression/disabled API to EMI** so that mods like AMI can signal "I own the screen, please hide yourself entirely" without relying on internal method names.

**Goal:** Add a clean public API to EMI's `EmiRegistry` or `EmiScreenManager` that allows third-party mods to suppress EMI rendering during a given screen lifecycle.

---

## Why This Matters

### Current Problem
AMI uses `EmiScreenManagerMixin` to inject into EMI's internal methods:
- `render(EmiDrawContext, int, int, float)` 
- `drawBackground(EmiDrawContext, int, int, float)`
- `drawForeground(EmiDrawContext, int, int, float)`
- `renderWidgets(EmiDrawContext, int, int, float, EmiScreenBase)` — private method
- `addWidgets(Screen)`
- `isDisabled()`

This works *partially* but has issues:
- Method names are internal; any EMI update can break this
- Mixin ordering and timing is fragile
- EMI's search bar still renders in some cases (timing of `addWidgets` vs `isAmiEnabled()` flag)
- No public contract; this is a hack, not a feature

### Proposed Solution
Expose a simple, public API:
```java
// Option A: Add to EmiRegistry (preferred)
EmiRegistry.setSuppressed(ModContainer container, boolean suppressed);
EmiRegistry.isSuppressed();

// Option B: Add to EmiScreenManager
EmiScreenManager.setScreenSuppressed(boolean);
```

When suppressed:
- `EmiScreenManager.addWidgets()` returns early (never adds search bar, buttons, tree)
- `EmiScreenManager.render()` returns early (no rendering)
- All EMI UI is hidden, but EMI's internal state is preserved
- The suppression applies only to the current screen; when the screen closes, suppression clears

---

## Files to Modify in EMI Repository

### 1. `xplat/src/main/java/dev/emi/emi/api/EmiRegistry.java`
**Current state:** Registers plugins, recipes, stacks.
**Change:** Add suppression API (static fields + methods).

```java
// Add to EmiRegistry:
private static boolean screenSuppressed = false;

public static void setSuppressed(boolean suppressed) {
    screenSuppressed = suppressed;
}

public static boolean isSuppressed() {
    return screenSuppressed;
}
```

### 2. `xplat/src/main/java/dev/emi/emi/screen/EmiScreenManager.java`
**Current state:** 
- Line 623: `if (isDisabled())` check in `render()` method
- Line 909: `addWidgets(Screen screen)` public static
- `renderWidgets()` at line 682 (private)

**Changes needed:**

At line 623 in `render()`, change:
```java
// Current:
if (isDisabled()) {
    // render baking/reload message
    return;
}

// To:
if (isDisabled() || EmiRegistry.isSuppressed()) {
    // render baking/reload message
    return;
}
```

At line 909 in `addWidgets()`, change:
```java
// Current:
public static void addWidgets(Screen screen) {
    if (isDisabled()) return;  // existing check
    // ... add all EMI widgets
}

// To:
public static void addWidgets(Screen screen) {
    if (isDisabled() || EmiRegistry.isSuppressed()) return;  // add suppression check
    // ... add all EMI widgets
}
```

### 3. `neoforge/src/main/java/dev/emi/emi/platform/neoforge/EmiClientNeoForge.java` (optional)
**Current state:** Event listeners at lines 46-47 call EMI's render methods.
**Consider:** Hook `ScreenEvent.Init.Post` to clear suppression flag, or add a screen-close listener.

```java
// Pseudo-code:
@SubscribeEvent
public static void onScreenOpen(ScreenEvent.Init.Post event) {
    // When a new screen opens, clear the suppression flag
    if (suppressed) {
        EmiRegistry.setSuppressed(false);
    }
}
```

Or, better: Let the consuming mod (AMI) manage the flag. If AMI clears it on screen close, EMI doesn't need to do anything.

---

## Implementation Steps

1. **Fork EMI** from https://github.com/emilyploszaj/emi
   - Clone locally
   - Create a branch: `feature/suppression-api`

2. **Add suppression field to `EmiRegistry`** (see above)
   - Make it a simple static boolean with getter/setter
   - Add JavaDoc explaining it applies only to the current screen and is cleared on screen close

3. **Update `EmiScreenManager.render()` and `addWidgets()`** to check `EmiRegistry.isSuppressed()`
   - Both should return early (before any rendering/widget addition) if suppressed
   - Keep the existing `isDisabled()` checks; `isSuppressed()` is a separate concern

4. **Test with a consuming mod** (use AMI as the test case)
   - In AMI's `OverlayWidgetManager.renderAll()`, call `EmiRegistry.setSuppressed(true)` when the AMI panel is visible and `suppressRecipeViewers` is enabled
   - On screen close or AMI panel hide, call `EmiRegistry.setSuppressed(false)`
   - Verify EMI's search bar, buttons, and item panels are completely hidden
   - Verify no mixin hacks are needed

5. **Verify compatibility**
   - Run EMI's own tests (if any)
   - Test with other consuming mods (JEI, other overlays)
   - Ensure `setSuppressed(false)` fully restores EMI's UI

6. **Write/update JavaDoc** explaining:
   - What suppression does (hides all EMI UI for one screen)
   - When to call it (when your mod owns the screen)
   - When it clears (when the screen closes or you call `setSuppressed(false)`)
   - Example: consuming mod calls `setSuppressed(true)` in `onScreenOpen`, `setSuppressed(false)` in `onScreenClose` or when the mod's UI is hidden

7. **Submit PR to EMI**
   - Reference the need for a public API for mods that want to fully suppress EMI
   - Link to AMI as a use case
   - Keep the change minimal and backward-compatible (new public methods only, no breaking changes)

---

## EMI Repository Details

- **GitHub:** https://github.com/emilyploszaj/emi
- **Branch:** `main` or `1.21.1` (check what's active for NeoForge 1.21.1)
- **Build system:** Gradle (similar to AMI)
- **Relevant source paths:**
  - `xplat/src/main/java/dev/emi/emi/api/EmiRegistry.java`
  - `xplat/src/main/java/dev/emi/emi/screen/EmiScreenManager.java`
  - `neoforge/src/main/java/dev/emi/emi/platform/neoforge/EmiClientNeoForge.java`

---

## Key Vendored EMI Source Locations (for reference from this repo)

These are copies of EMI's source for understanding the current implementation:

- `vendor-sources/emi/xplat/src/main/java/dev/emi/emi/screen/EmiScreenManager.java` — render pipeline, `isDisabled()`, `addWidgets()`
- `vendor-sources/emi/xplat/src/main/java/dev/emi/emi/api/EmiRegistry.java` — public registration API
- `vendor-sources/emi/neoforge/src/main/java/dev/emi/emi/platform/neoforge/EmiClientNeoForge.java` — NeoForge event hooks
- `vendor-sources/emi/xplat/src/main/java/dev/emi/emi/mixin/ScreenMixin.java` — EMI's own mixin (calls `addWidgets` on screen init)

---

## Proposed API Design (Summary)

### Simple Option (Recommended)
```java
// EmiRegistry.java
public static void setSuppressed(boolean suppressed) { ... }
public static boolean isSuppressed() { ... }
```

**Pros:**
- Two-liner addition
- No configuration needed
- Works for any mod that needs to hide EMI
- Thread-safe (single boolean)

**Cons:**
- No per-mod tracking (all mods share one flag)
- Consuming mods must manage cleanup (clear flag when done)

### Advanced Option (Out of scope for this PR)
```java
// EmiRegistry.java
public static void registerScreenSuppressor(String modId, Supplier<Boolean> isSuppressedFn) { ... }
```

This would allow multiple mods to register suppression conditions and EMI would respect any of them. However, this is more complex and can be added later if needed.

---

## Testing in AMI After EMI PR Is Merged

Once the PR is merged and a new EMI release is published:

1. **Update AMI's build.gradle** to depend on the new EMI version
2. **Remove EmiScreenManagerMixin.java** entirely
3. **Update OverlayWidgetManager.renderAll()** or a render hook:
   ```java
   if (panelVisible && AmiConfig.suppressRecipeViewers && ModList.get().isLoaded("emi")) {
       EmiRegistry.setSuppressed(true);
   }
   if (!panelVisible || !AmiConfig.suppressRecipeViewers) {
       EmiRegistry.setSuppressed(false);
   }
   ```
4. **Remove the mixin registration** from `ami.mixins.json`
5. **Test:** Open inventory with AMI panel active — EMI should be completely hidden

---

### 1. EMI search bar still visible (UNSOLVED — BACKGROUND ONLY)
---

## Appendix: Background Context from AMI

The following is reference information about the mixin-based workaround currently in AMI. Once the EMI PR is merged, this will no longer be necessary.

### Current Mixin-Based Suppression (to be replaced)
**File:** `src/main/java/com/sanhiruzu/ami/mixin/EmiScreenManagerMixin.java`

The mixin tries to inject cancellations into:
- `render(EmiDrawContext, int, int, float)`
- `drawBackground(EmiDrawContext, int, int, float)`
- `drawForeground(EmiDrawContext, int, int, float)`
- `renderWidgets(EmiDrawContext, int, int, float, EmiScreenBase)` — private method
- `addWidgets(Screen)`
- `isDisabled()` returns true

**Why this approach is fragile:**
- Method names are internal; breaks if EMI updates
- Timing issues: EMI's `ScreenMixin` fires at `Screen.init()` return (before NeoForge events), so `addWidgets` may be called before AMI's `isAmiEnabled()` flag is set
- No public contract; this is a hack, not an official feature
- EMI's search bar still renders in some cases

### Known AMI UI Bugs (Separate from EMI Suppression)

#### Toolbar Fields button overlaps Category dropdown (PARTIALLY FIXED — needs testing)
**What the user sees:** The "Fields (3)" button text renders on top of the "Category" dropdown text in the results panel toolbar.

**Root cause:** When the toolbar is in overflow/scroll mode (`contentWidth > width`), the Fields button was positioned using `x + width` (visible pixel coordinates) instead of `x + contentWidth` (virtual scroll coordinates). This put the Fields button visually inside the Category dropdown.

**Fix applied:** In `ResultsToolbar.updateDropdownPositions()` at `src/main/java/com/sanhiruzu/ami/client/results/ResultsToolbar.java`:
```java
// Changed from:
fieldsPicker.updatePosition(x + width - FIELDS_BTN_W - 2, y + 3, FIELDS_BTN_W);
// To:
fieldsPicker.updatePosition(x + contentWidth - FIELDS_BTN_W - 2, y + 3, FIELDS_BTN_W);
```

The Fields button is intentionally inside the scrollable area (confirmed by user). The `rightReserved` calculation already accounts for it: `rightBound = x + contentWidth - (FIELDS_BTN_W + 5) - 4`, so the last dropdown ends 7px before the Fields button starts.

**Needs testing** to confirm.

---

### 3. Favorites panel toggle (FIXED)
When switching the favorites sidebar from list to grid view, no items appeared. Root cause: favorites items were grouped by category (GroupBy.CATEGORY default) with all groups collapsed, so no items showed in grid mode.

**Fix applied:** In `UniversalResultsPanel.refreshTree()`, the favorites panel now produces flat leaf nodes instead of grouped nodes:
```java
if (isFavoritesPanel) {
    List<TreeNode> leaves = source.stream()
            .map(n -> new TreeNode(Component.literal(n.displayName()), n))
            .collect(java.util.stream.Collectors.toList());
    treeView.setRootNodes(leaves);
    gridView.setRootNodes(leaves);
}
```

Also fixed: `updateLayout()` for favorites panels was not updating `treeView`, only `gridView`. Both are now updated.

---

### 4. Icon regression (FIXED)
Only ~1/50 item icons were showing. Caused by mid-frame GL framebuffer switching in `ItemIconCache.primeVisible()`. The `ItemIconCache` integration was removed from `ItemGridView` — all icons render via direct `g.renderItem()`. The cache fix is deferred.

---

## Architecture Notes

- **Config:** `AmiConfig.java` is a static-field config with no file persistence. Values are runtime-only, defaulting to the hardcoded values. There is a separate `AMIConfig.java` (NeoForge ModConfigSpec) but `AmiConfig` is what most code reads.
- **EMI integration:** `AmiEmiPlugin.java` registers a full-screen exclusion zone when `suppressRecipeViewers` is true. This only affects where EMI places its item list panels, not whether it renders at all.
- **JEI integration works correctly:** `JeiIngredientListOverlayMixin` and `JeiBookmarkOverlayMixin` inject into JEI's `drawScreen` method, which is JEI's own rendering path. EMI is different — it renders through vanilla screen events.
- **Left panel width** is capped by `containerLeftEdge - PANEL_MARGIN * 2` in `OverlayWidgetManager.computeLayouts()`. No additional margin is applied; if the user wants space for FTB buttons, they reduce `leftPanelWidth` in config.
- **ItemIconCache** exists (`src/main/java/com/sanhiruzu/ami/client/ItemIconCache.java`) with a fixed projection matrix (`setOrtho(0, 16, 16, 0, -100, 3000)`) but is not integrated anywhere currently. The comment in `ItemIconRenderer.java` explains the bypass.

## Key Files

```
src/main/java/com/sanhiruzu/ami/
  mixin/EmiScreenManagerMixin.java       ← EMI suppression (currently not fully working)
  client/overlay/OverlayWidgetManager.java ← layout engine
  client/UniversalResultsPanel.java      ← favorites fix is here
  client/results/ResultsToolbar.java     ← Fields button fix is here
  client/results/ItemGridView.java       ← grid rendering (cache integration removed)
  client/ItemIconCache.java              ← off-screen cache (not used, has fixed projection)
  compat/AmiEmiPlugin.java               ← EMI exclusion zone registration
  config/AmiConfig.java                  ← all runtime config fields
vendor-sources/emi/                      ← EMI source for reference
```
