# Plan: Refactor SearchBarWidget onto EditBox

## Context

`SearchBarWidget` (`src/main/java/com/sanhiruzu/ami/client/overlay/SearchBarWidget.java`)
currently extends `AbstractWidget` and manually reimplements text-input mechanics:
character insertion, backspace, cursor rendering, scroll offset, and focus management.
This produced a real bug (space characters not rendered) and will continue to produce
edge-case bugs as features are added.

Minecraft already ships `net.minecraft.client.gui.components.EditBox` which handles
all of that correctly: cursor movement, left/right arrows, Home/End, Ctrl+arrow word
jump, Ctrl+A select-all, Ctrl+V paste, Ctrl+X/C cut/copy, Shift+arrow selection,
and mid-string insertion. The only genuinely custom part of `SearchBarWidget` is
the tokenized color rendering driven by `TokenColorizer`.

The goal of this refactor is to make `SearchBarWidget` extend `EditBox` so it inherits
all input handling for free, and override only the rendering to keep the colorized spans.

---

## Files to modify

| File | Change |
|------|--------|
| `src/main/java/com/sanhiruzu/ami/client/overlay/SearchBarWidget.java` | Full rewrite |
| `src/main/java/com/sanhiruzu/ami/client/InventoryOverlayHandler.java` | Simplify focus management |

No other files need changes. The public API surface (`getQuery()`, `setQuery()`, `clear()`,
`addToHistory()`, `Listener`, `updateBounds()`, `getBounds()`) must remain identical so
callers in `OverlayWidgetManager` compile without changes.

---

## Step 1 — Rewrite SearchBarWidget

### Constructor

Change the superclass from `AbstractWidget` to `EditBox`. The `EditBox` constructor
signature is:

```java
EditBox(Font font, int x, int y, int width, int height, Component hint)
```

Because `updateBounds()` sets the real position and size immediately after construction,
pass zeros for x/y/w/h in the super call. Example:

```java
public SearchBarWidget(Listener listener) {
    super(Minecraft.getInstance().font, 0, 0, 160, 14, Component.empty());
    this.listener = listener;
    setMaxLength(256);
    setResponder(this::onTextChanged);
    setBordered(false);   // we draw our own border in renderWidget
}
```

`setResponder` fires on every text change (including paste, cut, ctrl+backspace, etc.)
and replaces all the scattered `listener.onQueryChanged(query)` calls.

### Remove these fields and methods

- `String query` — replaced by `EditBox.getValue()` / `setValue()`
- `deleteChar()` — `EditBox.keyPressed(BACKSPACE, ...)` handles this
- `computeVisibleText()` — `EditBox` tracks scroll offset internally
- The manual cursor blink render block — `EditBox` renders its own cursor

### Keep these fields

- `List<String> history`, `int historyIndex`, `String liveQuery` — history is custom, not in EditBox
- `long lastClickTime`, `boolean highlight` — double-click highlight toggle
- `List<TokenColorizer.ColorSpan> colorSpans` — needed for colorized rendering

### getQuery / setQuery / clear

```java
public String getQuery() { return getValue(); }

public void setQuery(String q) {
    setValue(q == null ? "" : q);
    updateColorSpans();
}

public void clear() {
    setValue("");
    super.setFocused(false);
    historyIndex = -1;
    liveQuery = "";
    highlight = false;
    colorSpans = List.of();
}
```

### keyPressed

`EditBox.keyPressed()` handles Backspace, Delete, Ctrl+A, Ctrl+V, Ctrl+X, Ctrl+C,
Home, End, and all arrow keys. Override only to intercept the keys that need custom
AMI behavior, and call `super.keyPressed()` for everything else:

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (!isFocused()) return false;

    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
        setFocused(false);
        return false;   // propagate so the screen can close
    }
    if (keyCode == GLFW.GLFW_KEY_ENTER) {
        setFocused(false);
        return true;
    }
    if (keyCode == GLFW.GLFW_KEY_TAB) {
        return false;   // propagate for atlas-cycling keybind
    }
    if (keyCode == GLFW.GLFW_KEY_UP) {
        navigateHistory(+1);
        return true;
    }
    if (keyCode == GLFW.GLFW_KEY_DOWN) {
        navigateHistory(-1);
        return true;
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

Extract the Up/Down history logic into a `navigateHistory(int direction)` helper
(same logic as before, just moved).

### charTyped

Remove the override entirely. `EditBox.charTyped()` already inserts printable ASCII
and calls the responder. Deleting our override achieves the same result with no code.

If there are Unicode or non-ASCII considerations (currently the filter is `c >= 32 && c < 127`),
keep the same constraint by calling `setFilter(s -> s.chars().allMatch(c -> c >= 32 && c < 127))`
in the constructor.

### onTextChanged (new private method)

This replaces all the scattered `listener.onQueryChanged(query)` calls:

```java
private void onTextChanged(String newValue) {
    historyIndex = -1;
    updateColorSpans();
    if (listener != null) listener.onQueryChanged(newValue);
}
```

### renderWidget

`EditBox` renders its own text and cursor via `renderWidget`. We must override it
completely to draw AMI's custom background/border/highlight, and then draw colorized
text ourselves using `getValue()` and `getCursorPosition()`.

**Scroll offset problem:** `EditBox`'s internal `displayPos` (the index of the first
visible character) is private. Compute it independently: start from the cursor position
and walk left until the rendered width fits, similar to the old `computeVisibleText()`
but anchored at the cursor rather than always at the end:

```java
private int computeDisplayStart(Font font, int maxTextWidth) {
    String value = getValue();
    int cursor = getCursorPosition();
    // Ensure the cursor is always visible: find the leftmost displayPos such that
    // the substring [displayPos, cursor] fits within maxTextWidth.
    int displayPos = Math.min(cursor, value.length());
    while (displayPos > 0 && font.width(value.substring(displayPos, cursor)) < maxTextWidth) {
        displayPos--;
    }
    // If the full string fits, start from 0.
    if (font.width(value) <= maxTextWidth) return 0;
    return displayPos;
}
```

Then `renderWidget` follows the same structure as the current implementation (background,
border, optional highlight, colorized text, cursor), substituting `getValue()` for
`query` and `getCursorPosition()` for the end-of-string cursor assumption.

The `drawColorizedText` method stays as-is (it was just fixed to handle inter-span gaps).
Pass `displayStart` as the `scrollStart` argument.

**Cursor rendering:** Draw it at `textX + font.width(visibleText.substring(0, cursorInVisible)) + 1`
where `cursorInVisible = getCursorPosition() - displayStart`.

**setBordered(false):** Because we handle the border ourselves, disable EditBox's own
border drawing in the constructor with `setBordered(false)`.

---

## Step 2 — Simplify InventoryOverlayHandler

### Focus re-assertion loop (lines 91–99 in onRenderPost)

This block exists because EMI's `addWidgets` mixin auto-focuses its own search bar
every time the screen is reinit'd, which clears AMI's focus. The `EmiScreenManagerMixin`
now cancels `addWidgets` entirely when AMI is active, so EMI can no longer steal focus.

Verify that this block is no longer needed by testing:
1. Open inventory with EMI present
2. Click AMI search bar
3. Type several characters including spaces
4. Open a different container screen — focus should reset cleanly without the re-assertion loop

If the re-assertion loop is still needed (e.g. for non-EMI mods), keep it. If not, remove it.

### searchBarInputActive flag

This flag tracks whether the user has clicked the search bar so `onKeyPressed` and
`onCharTyped` know to route input to it. `EditBox` manages `isFocused()` correctly
itself, so `searchBarInputActive` should be replaceable with just `searchBar.isFocused()`.

Audit the three event handlers that read this flag:
- `onCharTyped` — replace `searchBarInputActive` with `searchBar.isFocused()`
- `onKeyPressed` — same
- `onMouseButtonPressed` — the set/clear of `searchBarInputActive` becomes the set/clear of focus only

If every read of `searchBarInputActive` is replaced by `searchBar.isFocused()`, the field
can be deleted. Be conservative: keep it if any timing edge case between mouse events and
`isFocused()` state appears during testing.

---

## What NOT to change

- `InventoryOverlayHandler.onMouseButtonPressed` — the logic that calls
  `searchBar.mouseClicked()` and cancels the event is still needed because some
  container screens override `mouseClicked` without calling super. `EditBox` handles
  the double-click-to-select-all internally now, but we need the event cancellation.
- `EmiSearchSyncBridge`, `EmiRecipeBridge`, `RecipeViewerBridge` — unchanged.
- `AmiEmiPlugin` exclusion area registration — unchanged.
- `EmiScreenManagerMixin` — unchanged.
- `TokenColorizer` — unchanged.

---

## Testing checklist

After the refactor, verify:

- [ ] Typing letters appears correctly
- [ ] Space inserts a visible space between words
- [ ] Backspace deletes one character
- [ ] Ctrl+Backspace deletes the whole last word
- [ ] Left/Right arrow keys move the cursor (mid-string insertion works)
- [ ] Home/End jump to start/end
- [ ] Ctrl+A selects all (then typing replaces)
- [ ] Ctrl+V pastes from clipboard
- [ ] Ctrl+X cuts selected text
- [ ] Up/Down arrows navigate search history
- [ ] Escape unfocuses and propagates (inventory does not close if Escape is the close key)
- [ ] Tab propagates (atlas cycling still works)
- [ ] 'E' while typing does not close inventory
- [ ] TokenColorizer color spans render correctly with spaces between tokens
- [ ] Placeholder text shows when empty and unfocused
- [ ] Search sync with EMI still works bidirectionally
- [ ] EMI sidebar is suppressed when AMI is active (SUPPRESS_RECIPE_VIEWERS=true)
- [ ] EMI sidebar shows when AMI is toggled off
