# AMI UI Theming & Layout Guide

**Purpose:** This document defines the visual GUI elements of the AMI (Automated Materials Index) inventory overlay and maps them to the "CSS-like" properties in the mod's configuration (`AMIConfig`). Use this as a reference when tweaking the layout or colors.

## Architecture Overview
*   **Values live in:** `AMIConfig.java` (These are what the user actually edits).
*   **Renderer uses:** `AMITheme.java` (Translates config to render-ready values).
*   **Hot-reloading:** The system hot-reloads every frame, meaning changes to config values apply instantly.
*   **Colors:** Defined as Hex values. Use **ARGB** (e.g., `0x4DFFB7C5` for 30% opacity pink) for backgrounds and hover states, and **RGB** (e.g., `0xFFDDDDDD`) for text.

---

## Visual Elements Map

### 1. The Global Overlay (`OverlayWidgetManager`)
This is the absolute background that sits behind all widgets.
*   `PALETTE_OVERLAY_BG`: The background dimming color over the Minecraft world/inventory (e.g., `0x99000000`).

### 2. The Search Bar (`SearchBarWidget`)
The text input field usually located at the bottom center of the screen.
*   `searchBarBg`: The main background color of the input field.
*   `searchBarBorder`: The outline/border color of the input field.
*   `searchText`: The color of the active text typed by the user.
*   `searchPlaceholder`: The color of the hint text (e.g., "Filter...") when the bar is empty.
*   `searchBarWidth`: Controls the width (in pixels) of the search bar.

### 3. The Main Results Panel (`UniversalResultsPanel`)
The large panel usually on the right side of the screen containing the facets, toolbar, and list of items.
*   `padding`: The global internal margin (inset in pixels) applied to the edges of the panel, pushing the Facet Bar, Toolbar, and List View inward. (Default: 6px).
*   `elementGap`: The vertical spacing (in pixels) between major structural components inside the panel (e.g., between the Facet Bar and Toolbar). (Default: 4px).
*   `cardBg`: The background color of the main inner content area where the results are rendered.

### 4. The Facet Bar (`FacetBar`)
The horizontal strip of quick-filter badges (e.g., "STR", "WPN") pinned to the top of the Results Panel.
*   *Note: Currently uses `padding` for margins and `cardBg` for its background to match the panel. Icon colors are currently hardcoded per facet.*

### 5. The Toolbar & Dropdowns (`ResultsToolbar`, `Dropdown`)
The row below the Facet Bar containing the View Mode toggle (Grid/List), Sort toggle, and Dropdown menus (Sort, Group, Mod Filters).
*   *Note: Inherits layout spacing from `elementGap`.*

### 6. The List View / Rich Cards (`ResultsTreeView`)
The scrollable list of items/results. This uses a "Rich Card" layout for each row.
*   `rowHeight`: The total vertical height (in pixels) of a single item row. Crucial for ensuring two-line typography (Name + Subtitle) fits without overlapping. (Default: 24px).
*   `iconSize`: The uniform width and height (in pixels) of the rendered item icons on the left of the card. (Default: 16px).
*   `cardBgHover`: The highlight color drawn behind an entire item row when the user's mouse hovers over it. Usually a semi-transparent ARGB value (e.g., `0x4DFFB7C5`).
*   `cardTextName`: The primary typography color used for the Item Name (Top line).
*   `cardTextSubtitle`: The secondary typography color used for the Mod Namespace / Subtitle (Bottom line).

### 7. Group Headers (`ResultsTreeView`)
When items are grouped (e.g., by Mod or Variant), this is the collapsible header row (e.g., "▶ minecraft (14)").
*   `groupHeaderBg`: The background color of the header row.
*   `groupHeaderText`: The text color of the header title.

### 8. The Scrollbar
The vertical scrollbar on the right edge of the List View or Grid View.
*   `scrollbarBg`: The background track of the scrollbar.
*   `scrollbarThumb`: The normal color of the scrollbar draggable handle.
*   `scrollbarThumbHover`: The color of the scrollbar handle when dragged or hovered.