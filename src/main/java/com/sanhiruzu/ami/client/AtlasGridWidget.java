package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class AtlasGridWidget {
    public enum Mode { ITEMS, ATLAS, SEARCH }

    private static final int ITEM_SIZE       = 16;
    private static final int PADDING         = 2;
    private static final int HEADER_HEIGHT   = 14;
    private static final int SEARCH_BAR_HEIGHT = 12;
    private static final int ROW_HEIGHT      = 11;
    private static final int SWATCH_SIZE     = 6;
    private static final int SWATCH_GAP      = 3;
    private static final int DIM_BADGE       = 4;
    private static final int ARROW_W         = 9;

    private int x, y, width, height;
    private Mode mode = Mode.ITEMS;
    private Component modeLabel = Component.translatable("ami.gui.items");
    private NodeType currentAtlasType = null;

    private final List<ItemStack> itemEntries = new ArrayList<>();

    // Search state
    private String searchQuery = "";
    private boolean searchFocused = false;
    private final List<AtlasGroup> searchGroups = new ArrayList<>();

    // Indexing state
    private boolean indexingInProgress = false;

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    /** Height available for scrollable content — below header, above search bar. */
    private int contentAreaHeight() {
        return height - HEADER_HEIGHT - 4 - SEARCH_BAR_HEIGHT - 4;
    }

    /** One group per namespace in the atlas list, or per NodeType in search results. */
    static final class AtlasGroup {
        final String namespace;
        final String displayName;
        final List<SearchNode> entries = new ArrayList<>();
        boolean expanded = true;

        AtlasGroup(String namespace) {
            this.namespace = namespace;
            this.displayName = RegistryUtils.modDisplayName(namespace);
        }

        AtlasGroup(String namespace, String explicitDisplayName) {
            this.namespace = namespace;
            this.displayName = explicitDisplayName;
        }
    }

    private final List<AtlasGroup> atlasGroups = new ArrayList<>();

    private int scrollOffset = 0;

    // Scrollbar drag state
    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    // Navigation arrow positions (set during header render)
    private int leftArrowX = -1;
    private int rightArrowX = -1;

    // Deferred tooltips — collected during render, drawn last
    private ItemStack pendingItemTooltip = null;
    private List<Component> pendingTooltipLines = null;

    public AtlasGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // -------------------------------------------------------------------------
    // Data setters
    // -------------------------------------------------------------------------

    public void setItemEntries(List<ItemStack> items) {
        itemEntries.clear();
        itemEntries.addAll(items);
        mode = Mode.ITEMS;
        scrollOffset = 0;
    }

    public void setAtlasEntries(List<SearchNode> entries, Component label, NodeType type) {
        // Preserve which groups the user has already collapsed
        Set<String> collapsed = new HashSet<>();
        for (AtlasGroup g : atlasGroups) {
            if (!g.expanded) collapsed.add(g.namespace);
        }

        atlasGroups.clear();
        Map<String, AtlasGroup> byNamespace = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            byNamespace.computeIfAbsent(entry.id().getNamespace(), AtlasGroup::new).entries.add(entry);
        }
        atlasGroups.addAll(byNamespace.values());

        for (AtlasGroup g : atlasGroups) {
            if (collapsed.contains(g.namespace)) g.expanded = false;
        }

        mode = Mode.ATLAS;
        modeLabel = label;
        currentAtlasType = type;
        scrollOffset = 0;
    }

    public void setItemModeLabel(Component label) {
        modeLabel = label;
    }

    // =========================================================================
    // Search-related methods
    // =========================================================================

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        searchGroups.clear();

        for (NodeType type : NodeType.values()) {
            List<SearchNode> typeResults = results.getOrDefault(type, new ArrayList<>());
            if (typeResults.isEmpty()) continue;

            String groupName = type.displayName().getString();
            AtlasGroup group = new AtlasGroup(type.name(), groupName);
            group.entries.addAll(typeResults);
            searchGroups.add(group);
        }

        mode = Mode.SEARCH;
        modeLabel = Component.translatable("ami.gui.search_results");
        currentAtlasType = null;
        scrollOffset = 0;
        searchQuery = query;
    }

    public void clearSearch() {
        searchQuery = "";
        searchFocused = false;
    }

    public void typeCharacter(char c) {
        if (c >= 32 && c < 127) {
            searchQuery += c;
        }
    }

    public void deleteSearchChar() {
        if (!searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        }
    }

    public boolean isSearchFocused() {
        return searchFocused;
    }

    public void setSearchFocused(boolean focused) {
        this.searchFocused = focused;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setIndexingInProgress(boolean inProgress) {
        this.indexingInProgress = inProgress;
    }

    public boolean isSearchBarHovered(double mouseX, double mouseY) {
        int searchBarY = y + height - SEARCH_BAR_HEIGHT - 3;
        int searchBarH = SEARCH_BAR_HEIGHT + 1;
        return mouseX >= x + 1 && mouseX < x + width - 1
                && mouseY >= searchBarY && mouseY < searchBarY + searchBarH;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pendingItemTooltip = null;
        pendingTooltipLines = null;

        // Panel background
        g.fill(x, y, x + width, y + height, AMITheme.PANEL_BG);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, AMITheme.PANEL_INNER);

        // Header bar — amber tint when cheat mode active
        boolean cheat = AMICheatMode.isEnabled();
        g.fill(x, y, x + width, y + HEADER_HEIGHT + 2, cheat ? AMITheme.CHEAT_HEADER_BG : AMITheme.HEADER_BG);
        g.fill(x, y + HEADER_HEIGHT + 2, x + width, y + HEADER_HEIGHT + 3, cheat ? AMITheme.CHEAT_HEADER_SEP : AMITheme.HEADER_SEP);

        var font = Minecraft.getInstance().font;

        // Navigation arrows
        leftArrowX = x + 2;
        rightArrowX = x + width - 2 - ARROW_W;

        boolean leftHovered = isLeftArrowHovered(mouseX, mouseY);
        boolean rightHovered = isRightArrowHovered(mouseX, mouseY);

        // Draw arrows
        g.drawString(font, "◀", leftArrowX, y + 3, leftHovered ? AMITheme.ARROW_HOVER : AMITheme.ARROW_NORMAL, false);
        g.drawString(font, "▶", rightArrowX, y + 3, rightHovered ? AMITheme.ARROW_HOVER : AMITheme.ARROW_NORMAL, false);

        // Center text: "TabName (count) [pos/total]"
        MutableComponent centerText = modeLabel.copy()
                .append(Component.literal(" (" + entryCount() + ")"));

        if (currentAtlasType != null && mode != Mode.SEARCH) {
            NodeType[] atlas = NodeType.atlasValues();
            int idx = 0;
            for (int i = 0; i < atlas.length; i++) {
                if (atlas[i] == currentAtlasType) { idx = i; break; }
            }
            int position = idx + 1;
            centerText.append(Component.literal(" [" + position + "/" + atlas.length + "]")
                    .withStyle(s -> s.withColor(0xAAAAAA)));
        }

        if (cheat) {
            centerText.append(Component.literal(" [!]").withStyle(s -> s.withColor(AMITheme.CHEAT_INDICATOR)));
        }

        // Draw centered, clamped between arrow areas
        int textWidth = font.width(centerText);
        int centerX = x + (width - textWidth) / 2;
        centerX = Math.max(leftArrowX + ARROW_W + 2, Math.min(centerX, rightArrowX - textWidth - 2));
        g.drawString(font, centerText, centerX, y + 2, cheat ? AMITheme.CHEAT_INDICATOR : AMITheme.HEADER_TEXT, false);

        // Search bar always at bottom (persistent, like JEI/EMI)
        renderSearchBar(g, mouseX, mouseY);

        if (mode == Mode.ITEMS) {
            renderItemGrid(g, mouseX, mouseY);
        } else {
            renderAtlasList(g, mouseX, mouseY);
        }

        renderScrollBar(g, mouseX, mouseY);

        if (pendingItemTooltip != null) {
            g.renderTooltip(Minecraft.getInstance().font, pendingItemTooltip, mouseX, mouseY);
        } else if (pendingTooltipLines != null) {
            g.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltipLines, mouseX, mouseY);
        }
    }

    private void renderSearchBar(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int searchBarY = y + height - SEARCH_BAR_HEIGHT - 3;
        int searchBarX = x + 2;
        int searchBarW = width - 4;

        // Background
        g.fill(searchBarX, searchBarY, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT,
                searchFocused ? 0xFF3A3A3A : 0xFF2A2A2A);
        g.fill(searchBarX + 1, searchBarY + 1, searchBarX + searchBarW - 1, searchBarY + SEARCH_BAR_HEIGHT - 1,
                0xFF1A1A1A);

        // Border
        int borderColor = searchFocused ? 0xFFAAAA44 : 0xFF555555;
        g.fill(searchBarX, searchBarY, searchBarX + searchBarW, searchBarY + 1, borderColor);
        g.fill(searchBarX, searchBarY + SEARCH_BAR_HEIGHT - 1, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT, borderColor);
        g.fill(searchBarX, searchBarY, searchBarX + 1, searchBarY + SEARCH_BAR_HEIGHT, borderColor);
        g.fill(searchBarX + searchBarW - 1, searchBarY, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT, borderColor);

        // Text rendering area
        int textX = searchBarX + 3;
        int textMaxW = searchBarW - 6;
        String displayText = searchQuery;
        if (searchQuery.isEmpty() && !searchFocused) {
            displayText = "Filter...";
            g.drawString(font, displayText, textX, searchBarY + 2, 0xFF666666, false);
        } else if (!searchQuery.isEmpty()) {
            g.drawString(font, displayText, textX, searchBarY + 2, 0xFFCCCCCC, false);
        }

        // Cursor blink
        if (searchFocused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + font.width(displayText) + 1;
            g.fill(cursorX, searchBarY + 2, cursorX + 1, searchBarY + SEARCH_BAR_HEIGHT - 2, 0xFFCCCCCC);
        }

        // Clear button (x)
        if (!searchQuery.isEmpty()) {
            int clearX = searchBarX + searchBarW - 9;
            boolean clearHovered = mouseX >= clearX && mouseX < searchBarX + searchBarW - 1
                    && mouseY >= searchBarY && mouseY < searchBarY + SEARCH_BAR_HEIGHT;
            g.drawString(font, "x", clearX + 1, searchBarY + 2, clearHovered ? 0xFFFF6666 : 0xFFCC6666, false);
        }
    }

    private void renderItemGrid(GuiGraphics g, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = contentAreaHeight();
        int perRow   = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
        int visRows  = contentH / (ITEM_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visRows * perRow, itemEntries.size()); i++) {
            int row  = (i - scrollOffset) / perRow;
            int col  = (i - scrollOffset) % perRow;
            int drawX = x + 4 + col * (ITEM_SIZE + PADDING);
            int drawY = contentY + row * (ITEM_SIZE + PADDING);
            ItemStack stack = itemEntries.get(i);

            g.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, AMITheme.SLOT_BG);
            boolean hovered = mouseX >= drawX && mouseX < drawX + ITEM_SIZE
                    && mouseY >= drawY && mouseY < drawY + ITEM_SIZE;
            if (hovered) {
                g.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, AMITheme.SLOT_HOVER);
                pendingItemTooltip = stack;
            }
            g.renderItem(stack, drawX, drawY);
            g.renderItemDecorations(Minecraft.getInstance().font, stack, drawX, drawY);
        }
    }

    private void renderAtlasList(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = contentAreaHeight();
        int visRows  = Math.max(1, contentH / ROW_HEIGHT);

        int textStartX = x + SWATCH_GAP + SWATCH_SIZE + SWATCH_GAP;
        int maxTextW   = width - (textStartX - x) - DIM_BADGE - 6;

        // Use searchGroups if in SEARCH mode, else use atlasGroups
        List<AtlasGroup> groups = (mode == Mode.SEARCH) ? searchGroups : atlasGroups;

        if (groups.isEmpty()) {
            Component message;
            if (indexingInProgress) {
                message = Component.translatable("ami.gui.indexing");
            } else {
                var index = GlobalIndex.getInstance();
                message = currentAtlasType != null && index.isLoading(currentAtlasType)
                        ? Component.translatable("ami.gui.loading")
                        : Component.translatable("ami.gui.empty_list");
            }
            g.drawString(font, message,
                    x + 4, y + HEADER_HEIGHT + 8, AMITheme.ENTRY_TEXT, false);

            // Hint to press Tab to cycle through tabs
            g.drawString(font, Component.translatable("ami.gui.cycle_hint", "TAB")
                    .withStyle(s -> s.withColor(0x888888)),
                    x + 4, y + HEADER_HEIGHT + 20, AMITheme.ENTRY_TEXT, false);
            return;
        }

        // Sample current biome/structure once per frame — cheap chunk lookups
        var currentBiomeId = currentBiomeId();
        var currentStructureIds = currentStructureIds();

        int row = 0;
        for (AtlasGroup group : groups) {
            // --- group header ---
            if (row >= scrollOffset && row < scrollOffset + visRows) {
                int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                boolean hovered = isRowHovered(mouseX, mouseY, drawY);

                g.fill(x + 1, drawY, x + width - 5, drawY + ROW_HEIGHT - 1,
                        hovered ? AMITheme.GROUP_BG_HOVER : AMITheme.GROUP_BG);

                String arrow = group.expanded ? "▼ " : "▶ ";
                String label = arrow + group.displayName + " (" + group.entries.size() + ")";
                g.drawString(font, label, x + 4, drawY + 2, AMITheme.GROUP_TEXT, false);
            }
            row++;

            if (!group.expanded) continue;

            // --- group entries ---
            for (SearchNode entry : group.entries) {
                if (row >= scrollOffset && row < scrollOffset + visRows) {
                    int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                    boolean hovered = isRowHovered(mouseX, mouseY, drawY);

                    boolean isCurrent = switch (entry.type()) {
                        case BIOME     -> entry.id().equals(currentBiomeId);
                        case STRUCTURE -> currentStructureIds.contains(entry.id());
                        default        -> false;
                    };

                    if (isCurrent) {
                        int accentBg = entry.type() == NodeType.BIOME
                                ? AMITheme.CURRENT_BIOME_BG : AMITheme.CURRENT_STRUCT_BG;
                        int accentBar = entry.type() == NodeType.BIOME
                                ? AMITheme.CURRENT_BIOME_ACCENT : AMITheme.CURRENT_STRUCT_ACCENT;
                        g.fill(x + 2, drawY, x + width - 6, drawY + ROW_HEIGHT, accentBg);
                        g.fill(x + 2, drawY, x + 4, drawY + ROW_HEIGHT, accentBar);
                    }
                    if (hovered) {
                        boolean cheatOn = AMICheatMode.isEnabled();
                        g.fill(x + 4, drawY, x + width - 6, drawY + ROW_HEIGHT,
                                cheatOn ? AMITheme.CHEAT_ENTRY_HOVER : AMITheme.ENTRY_HOVER);
                        pendingTooltipLines = buildTooltip(entry, Screen.hasShiftDown());
                    }

                    // Color swatch / temperature gauge
                    if (entry.type() == NodeType.BIOME) {
                        renderBiomeTempGauge(g, entry, drawY);
                    } else {
                        int swatchY = drawY + (ROW_HEIGHT - SWATCH_SIZE) / 2;
                        g.fill(x + SWATCH_GAP, swatchY,
                                x + SWATCH_GAP + SWATCH_SIZE, swatchY + SWATCH_SIZE, entry.color());
                    }

                    // Dimension badge (top-right of the row, only for non-overworld)
                    String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
                    if (!"overworld".equals(dim)) {
                        int badgeColor = "nether".equals(dim) ? AMITheme.DIM_NETHER : AMITheme.DIM_END;
                        int badgeX = x + width - DIM_BADGE - 6;
                        int badgeY = drawY + (ROW_HEIGHT - DIM_BADGE) / 2;
                        g.fill(badgeX, badgeY, badgeX + DIM_BADGE, badgeY + DIM_BADGE, badgeColor);
                    }

                    // Name — truncate to fit between swatch and badge
                    String name = entry.displayName();
                    while (font.width(name) > maxTextW && name.length() > 1) {
                        name = name.substring(0, name.length() - 1);
                    }
                    if (font.width(entry.displayName()) > maxTextW) name += "…";
                    g.drawString(font, name, textStartX, drawY + 2, AMITheme.ENTRY_TEXT, false);
                }
                row++;
            }
        }
    }

    private boolean isRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x + 2 && mouseX < x + width - 5
                && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT;
    }

    private List<Component> buildTooltip(SearchNode entry, boolean shifted) {
        List<Component> lines = new ArrayList<>();

        // Header: name + dimension tag if not overworld
        MutableComponent header = Component.literal(entry.displayName());
        String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
        if (!"overworld".equals(dim)) {
            Component dimLabel = Component.translatable("ami.dimension." + dim);
            header.append(Component.literal(" [").withStyle(s -> s.withColor(0x888888)))
                  .append(dimLabel)
                  .append(Component.literal("]").withStyle(s -> s.withColor(0x888888)));
        }
        lines.add(header);

        lines.add(Component.literal(entry.id().toString()).withStyle(s -> s.withColor(0x666666)));
        lines.add(Component.literal(RegistryUtils.modDisplayName(entry.id().getNamespace()))
                .withStyle(s -> s.withColor(0x888888)));

        if (shifted) {
            lines.add(Component.empty()); // blank separator between header block and details
            switch (entry.type()) {
                case BIOME     -> appendBiomeDetails(lines, entry);
                case ENTITY    -> appendEntityDetails(lines, entry);
                case STRUCTURE -> appendStructureDetails(lines, entry);
                case DIMENSION -> {} // No extra details for dimensions yet
                case ITEM, PLAYER -> {} // unreachable in atlas mode
            }
        } else {
            lines.add(Component.translatable("ami.tooltip.shift_for_details")
                    .withStyle(s -> s.withColor(0x555555)));
        }

        if (AMICheatMode.isEnabled()) {
            Component clickHint = switch (entry.type()) {
                case BIOME, STRUCTURE -> Component.translatable("ami.tooltip.cheat_locate");
                case ENTITY           -> Component.translatable("ami.tooltip.cheat_entity");
                case DIMENSION        -> Component.translatable("ami.tooltip.dimension_info");
                case ITEM, PLAYER     -> Component.empty(); // unreachable in atlas mode
            };
            lines.add(clickHint.copy().withStyle(s -> s.withColor(AMITheme.CHEAT_INDICATOR)));
        }

        return lines;
    }

    private void appendBiomeDetails(List<Component> lines, SearchNode entry) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var biomeKey = ResourceKey.create(Registries.BIOME, entry.id());
        mc.level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
            reg.getHolder(biomeKey).ifPresent(holder -> {
                var biome = holder.value();
                float temp = biome.getBaseTemperature();
                Component precip = !biome.hasPrecipitation() ? Component.translatable("ami.precipitation.none")
                              : temp < 0.15f ? Component.translatable("ami.precipitation.snow") : Component.translatable("ami.precipitation.rain");
                var effects = biome.getSpecialEffects();

                lines.add(Component.translatable("ami.tooltip.temperature")
                        .append(Component.literal(": " + String.format("%.2f", temp))
                                .withStyle(s -> s.withColor(tempColor(temp)))));

                lines.add(Component.translatable("ami.tooltip.precipitation")
                        .append(Component.literal(": ").append(precip)
                                .withStyle(s -> s.withColor(0xAAAAFF))));

                lines.add(Component.translatable("ami.tooltip.water_color")
                        .append(colorSwatch(effects.getWaterColor())));

                lines.add(Component.translatable("ami.tooltip.sky_color")
                        .append(colorSwatch(effects.getSkyColor())));

                // Tags — is_* descriptors first, capped at 6
                var tags = holder.tags()
                        .sorted((a, b) -> {
                            boolean aIs = a.location().getPath().startsWith("is_");
                            boolean bIs = b.location().getPath().startsWith("is_");
                            if (aIs != bIs) return aIs ? -1 : 1;
                            return a.location().toString().compareTo(b.location().toString());
                        })
                        .limit(6)
                        .toList();

                if (!tags.isEmpty()) {
                    lines.add(Component.translatable("ami.tooltip.tags")
                            .append(Component.literal(":").withStyle(s -> s.withColor(0x888888))));
                    for (var tag : tags) {
                        lines.add(Component.literal("  #" + tag.location())
                                .withStyle(s -> s.withColor(0x667766)));
                    }
                }
            })
        );
    }

    private void appendEntityDetails(List<Component> lines, SearchNode entry) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(entry.id()).ifPresent(entityType -> {
            Component category = switch (entityType.getCategory()) {
                case MONSTER                        -> Component.translatable("ami.entity_category.hostile");
                case CREATURE                       -> Component.translatable("ami.entity_category.passive");
                case AMBIENT                        -> Component.translatable("ami.entity_category.ambient");
                case WATER_CREATURE, WATER_AMBIENT,
                     UNDERGROUND_WATER_CREATURE     -> Component.translatable("ami.entity_category.aquatic");
                default                             -> Component.translatable("ami.entity_category.misc");
            };

            var dims = entityType.getDimensions();
            lines.add(Component.translatable("ami.tooltip.category")
                    .append(Component.literal(": ").append(category)
                            .withStyle(s -> s.withColor(0xAAAAFF))));
            lines.add(Component.translatable("ami.tooltip.size")
                    .append(Component.literal(String.format(": %.1f x %.1f (WxH)", dims.width(), dims.height()))
                            .withStyle(s -> s.withColor(0xAAAAFF))));
            if (entityType.fireImmune()) {
                lines.add(Component.translatable("ami.tooltip.fire_immune")
                        .withStyle(s -> s.withColor(0xFFAA44)));
            }
        });
    }

    private void appendStructureDetails(List<Component> lines, SearchNode entry) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var key = ResourceKey.create(Registries.STRUCTURE, entry.id());
        mc.level.registryAccess().registry(Registries.STRUCTURE).ifPresent(reg ->
            reg.getHolder(key).ifPresent(holder -> {
                var tags = holder.tags().limit(6).toList();
                if (!tags.isEmpty()) {
                    lines.add(Component.translatable("ami.tooltip.tags")
                            .append(Component.literal(":").withStyle(s -> s.withColor(0x888888))));
                    for (var tag : tags) {
                        lines.add(Component.literal("  #" + tag.location())
                                .withStyle(s -> s.withColor(0x667766)));
                    }
                }
            })
        );
    }

    /**
     * For biome rows: a 3px-wide vertical fill bar (temperature level) plus a 4px water-color swatch.
     * Both fit in x+3 … x+11, matching the standard swatch area so textStartX (x+12) is unchanged.
     *
     * Temperature is normalized from [-0.5, 2.0] to [0, 1] for the fill height.
     */
    private void renderBiomeTempGauge(GuiGraphics g, SearchNode entry, int drawY) {
        float temp;
        try {
            temp = Float.parseFloat(entry.meta(SearchNodeKeys.TEMPERATURE, "0.5"));
        } catch (NumberFormatException e) {
            temp = 0.5f;
        }
        float normalized = (Math.max(-0.5f, Math.min(2.0f, temp)) + 0.5f) / 2.5f;

        // Vertical fill bar: 3px wide, spans row interior height
        int gaugeX   = x + SWATCH_GAP;
        int gaugeTop = drawY + 1;
        int gaugeBot = drawY + ROW_HEIGHT - 1;
        int gaugeH   = gaugeBot - gaugeTop;
        int fillH    = Math.round(normalized * gaugeH);
        g.fill(gaugeX, gaugeTop, gaugeX + 3, gaugeBot, 0xFF1A1A1A);
        if (fillH > 0) {
            g.fill(gaugeX, gaugeBot - fillH, gaugeX + 3, gaugeBot, tempColor(temp));
        }

        // Water color swatch: 4px wide, vertically centered
        int swatchX = gaugeX + 4;
        int swatchY = drawY + (ROW_HEIGHT - SWATCH_SIZE) / 2;
        g.fill(swatchX, swatchY, swatchX + 4, swatchY + SWATCH_SIZE, entry.color());
    }

    /** " #RRGGBB" with the hex rendered in that colour. */
    private static MutableComponent colorSwatch(int rgb) {
        int opaque = 0xFF000000 | rgb;
        return Component.literal(" #" + String.format("%06X", rgb & 0xFFFFFF))
                .withStyle(s -> s.withColor(opaque));
    }

    /** Returns the resource location of the biome the player is currently standing in, or null. */
    private static net.minecraft.resources.ResourceLocation currentBiomeId() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        return mc.level.getBiome(mc.player.blockPosition())
                .unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    /**
     * Returns resource locations of structures whose starts are in the player's current chunk
     * and whose bounding box contains the player's position.
     *
     * Note: structure starts live in the origin chunk only. Large structures (mansions, strongholds)
     * will only match while the player is in that specific origin chunk.
     */
    private static java.util.Set<net.minecraft.resources.ResourceLocation> currentStructureIds() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return java.util.Set.of();

        var pos = mc.player.blockPosition();
        var chunk = mc.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        var reg = mc.level.registryAccess().registry(Registries.STRUCTURE).orElse(null);
        if (reg == null) return java.util.Set.of();

        var found = new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
        for (var entry : chunk.getAllStarts().entrySet()) {
            var start = entry.getValue();
            if (start.getPieces().isEmpty()) continue;
            if (!start.getBoundingBox().isInside(pos)) continue;
            reg.getResourceKey(entry.getKey())
                    .ifPresent(key -> found.add(key.location()));
        }
        return found;
    }

    private static int tempColor(float temp) {
        if (temp <= 0.0f) return 0xFF4488CC;
        if (temp <  0.3f) return 0xFF44AACC;
        if (temp <  0.6f) return 0xFF88CC44;
        if (temp <  1.0f) return 0xFFCCCC44;
        return 0xFFCC8844;
    }

    private void renderScrollBar(GuiGraphics g, int mouseX, int mouseY) {
        int total    = totalRows();
        int contentH = contentAreaHeight();
        int visible  = visibleRowCount(contentH);
        if (total <= visible) return;

        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
        int barW     = active ? 5 : 3;
        int barX     = x + width - 1 - barW;
        int barAreaY = y + HEADER_HEIGHT + 4;
        int thumbH   = Math.max(10, (visible * contentH) / total);
        int thumbY   = barAreaY + (scrollOffset * (contentH - thumbH)) / (total - visible);

        g.fill(barX, barAreaY, barX + barW, barAreaY + contentH, AMITheme.SCROLL_TRACK);
        g.fill(barX, thumbY,   barX + barW, thumbY + thumbH,
                active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB);
    }

    public boolean isScrollbarHovered(int mouseX, int mouseY) {
        int contentH = contentAreaHeight();
        if (totalRows() <= visibleRowCount(contentH)) return false;
        int barAreaY = y + HEADER_HEIGHT + 4;
        // 6px hover zone covers both normal (3px) and expanded (5px) widths
        return mouseX >= x + width - 6 && mouseX < x + width - 1
                && mouseY >= barAreaY && mouseY < barAreaY + contentH;
    }

    public boolean isLeftArrowHovered(int mouseX, int mouseY) {
        if (leftArrowX < 0) return false;
        return mouseX >= leftArrowX && mouseX < leftArrowX + ARROW_W
                && mouseY >= y && mouseY < y + HEADER_HEIGHT + 2;
    }

    public boolean isRightArrowHovered(int mouseX, int mouseY) {
        if (rightArrowX < 0) return false;
        return mouseX >= rightArrowX && mouseX < rightArrowX + ARROW_W
                && mouseY >= y && mouseY < y + HEADER_HEIGHT + 2;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    /** Returns true if the scrollbar was clicked and a drag has started. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (pendingItemTooltip != null && !pendingItemTooltip.isEmpty()) {
                AmiClipboardHelper.copyItemTooltipToClipboard(pendingItemTooltip);
                return true;
            } else if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
                AmiClipboardHelper.copyComponentsToClipboard(pendingTooltipLines);
                return true;
            }
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0 || !isScrollbarHovered((int) mouseX, (int) mouseY)) return false;
        scrollbarDragging = true;
        scrollbarDragStartY = (int) mouseY;
        scrollbarDragStartOffset = scrollOffset;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!scrollbarDragging || button != 0) return false;

        int contentH = contentAreaHeight();
        int total    = totalRows();
        int visible  = visibleRowCount(contentH);
        if (total <= visible) return true;

        int thumbH    = Math.max(10, (visible * contentH) / total);
        int dragRange = contentH - thumbH;
        if (dragRange <= 0) return true;

        int dy          = (int) mouseY - scrollbarDragStartY;
        int offsetDelta = (int) Math.round((double) dy * (total - visible) / dragRange);
        scrollOffset = Math.max(0, Math.min(total - visible, scrollbarDragStartOffset + offsetDelta));
        return true;
    }

    public void stopScrollbarDrag() {
        scrollbarDragging = false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Search bar always clickable (at bottom, regardless of mode)
        if (isSearchBarHovered(mouseX, mouseY)) {
            setSearchFocused(true);
            return true;
        }

        if (mode == Mode.ITEMS) {
            return AMICheatMode.isEnabled() && handleItemCheatClick(mouseX, mouseY);
        }

        if (mode == Mode.ATLAS || mode == Mode.SEARCH) {
            return handleAtlasClick(mouseX, mouseY);
        }

        return false;
    }

    private boolean handleItemCheatClick(double mouseX, double mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        if (mouseY < contentY) return false;

        int itemsPerRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
        int vRow = (int) ((mouseY - contentY) / (ITEM_SIZE + PADDING));
        int vCol = (int) ((mouseX - (x + 4)) / (ITEM_SIZE + PADDING));
        if (vCol < 0 || vCol >= itemsPerRow) return false;

        int idx = scrollOffset + vRow * itemsPerRow + vCol;
        if (idx < 0 || idx >= itemEntries.size()) return false;

        var itemId = BuiltInRegistries.ITEM.getKey(itemEntries.get(idx).getItem());
        if (itemId != null) {
            AMICheatMode.giveItem(itemId);
        }
        return true;
    }

    private boolean handleAtlasClick(double mouseX, double mouseY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = contentAreaHeight();
        int visRows  = Math.max(1, contentH / ROW_HEIGHT);

        // Mirror the render loop exactly — use the same isRowHovered test so
        // click and hover share identical pixel bounds, with no math inversion.
        List<AtlasGroup> groups = (mode == Mode.SEARCH) ? searchGroups : atlasGroups;
        int row = 0;
        for (AtlasGroup group : groups) {
            if (row >= scrollOffset && row < scrollOffset + visRows) {
                int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                if (isRowHovered(mx, my, drawY)) {
                    group.expanded = !group.expanded;
                    int maxScroll = Math.max(0, totalRows() - visRows);
                    scrollOffset = Math.min(scrollOffset, maxScroll);
                    return true;
                }
            }
            row++;

            if (group.expanded) {
                for (SearchNode entry : group.entries) {
                    if (row >= scrollOffset && row < scrollOffset + visRows) {
                        int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                        if (isRowHovered(mx, my, drawY) && AMICheatMode.isEnabled()) {
                            switch (entry.type()) {
                                case BIOME     -> AMICheatMode.locateBiome(entry.id());
                                case STRUCTURE -> AMICheatMode.locateStructure(entry.id());
                                case ENTITY    -> {} // future: summon
                                case DIMENSION -> {} // future: dimension tp
                                case ITEM, PLAYER -> {} // unreachable in atlas mode
                            }
                            return true;
                        }
                    }
                    row++;
                }
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentH = contentAreaHeight();
        int maxScroll = Math.max(0, totalRows() - visibleRowCount(contentH));
        scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollOffset - scrollDelta)));
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Total logical rows including group headers and expanded entries. */
    private int totalRows() {
        if (mode == Mode.ITEMS) {
            int perRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            return (itemEntries.size() + perRow - 1) / perRow;
        }
        List<AtlasGroup> groups = (mode == Mode.SEARCH) ? searchGroups : atlasGroups;
        int rows = 0;
        for (AtlasGroup g : groups) {
            rows++; // header
            if (g.expanded) rows += g.entries.size();
        }
        return rows;
    }

    private int visibleRowCount(int contentH) {
        if (mode == Mode.ITEMS) {
            int perRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            return perRow * (contentH / (ITEM_SIZE + PADDING));
        }
        return Math.max(1, contentH / ROW_HEIGHT);
    }

    /** Total flat entry count (for the header label). */
    private int entryCount() {
        if (mode == Mode.ITEMS) return itemEntries.size();
        List<AtlasGroup> groups = (mode == Mode.SEARCH) ? searchGroups : atlasGroups;
        return groups.stream().mapToInt(g -> g.entries.size()).sum();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX()          { return x; }
    public int getY()          { return y; }
    public int getWidth()      { return width; }
    public int getHeight()     { return height; }
    public int getEntryCount() { return entryCount(); }
}
