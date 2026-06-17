package com.sanhiruzu.ami.client.screen;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.input.TextInputFilter;
import com.sanhiruzu.ami.client.widget.*;
import com.sanhiruzu.ami.compat.FtbQuestsRuntimeCompat;
import com.sanhiruzu.ami.config.*;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Advanced configuration screen for AMI with sidebar navigation, visual grouping, and interactive editors.
 */
public class AmiConfigScreen extends Screen {
    private final Screen parent;
    private final Map<Field, Object> originalValues = new HashMap<>();
    private EditBox searchBox;
    private CategoryList categories;
    private ConfigList list;
    private ConfigList.ConfigEntry focusedConfigEntry;
    private AmiDropdownPopup openDropdown;
    private Button revertBtn;
    private Button defaultsBtn;
    private boolean resetConfigScrollOnNextBuild;

    private String activeCategory = "general";
    private String searchQuery = "";

    private int sidebarWidth;
    private int listX;
    private int listWidth;
    private int contentTop;

    public AmiConfigScreen(Screen parent) {
        super(Component.translatable("ami.config.title"));
        this.parent = parent;
        captureOriginalValues();
    }

    private static List<AmiConfig.PanelContent> readSlots(Field field) {
        try {
            return new ArrayList<>(AmiConfig.parsePanelSlots((String) field.get(null)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void writeSlots(Field field, List<AmiConfig.PanelContent> slots) {
        try {
            field.set(null, AmiConfig.encodePanelSlots(slots));
        } catch (Exception ignored) {
        }
    }

    private static String panelContentLabel(AmiConfig.PanelContent content) {
        String key = switch (content) {
            case GRID -> "ami.config.panel.results_grid";
            case LIST -> "ami.config.panel.results_list";
            case COMPACT -> "ami.config.panel.results_compact";
            case FAVORITES -> "ami.gui.favorites";
            case LOOKUP_HISTORY -> "ami.gui.sidebar.lookup_history";
            case CRAFTING_HISTORY -> "ami.gui.sidebar.crafting_history";
            case CRAFTABLE -> "ami.gui.sidebar.craftable";
            case QUESTS -> "ami.gui.sidebar.quests";
            case EMPTY -> "ami.gui.sidebar.empty";
            default -> null;
        };
        if (key != null) return Component.translatable(key).getString();
        return content.name();
    }

    private static AmiConfig.PanelContent[] selectablePanelContents() {
        return new AmiConfig.PanelContent[]{
                AmiConfig.PanelContent.EMPTY, AmiConfig.PanelContent.FAVORITES, AmiConfig.PanelContent.GRID,
                AmiConfig.PanelContent.LIST, AmiConfig.PanelContent.COMPACT, AmiConfig.PanelContent.LOOKUP_HISTORY,
                AmiConfig.PanelContent.CRAFTING_HISTORY, AmiConfig.PanelContent.CRAFTABLE, AmiConfig.PanelContent.QUESTS
        };
    }

    private void captureOriginalValues() {
        originalValues.clear();
        for (Field field : AmiConfig.class.getFields()) {
            try {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    originalValues.put(field, field.get(null));
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        this.sidebarWidth = Math.min(130, Math.max(90, width / 6));
        this.listX = sidebarWidth + 1;
        this.listWidth = width - listX;
        boolean emiLoaded = Services.PLATFORM.isModLoaded("emi");
        int headerAvailable = Math.max(0, listWidth - 16);
        boolean stackedHeader = emiLoaded && headerAvailable < 230;
        this.contentTop = stackedHeader ? 64 : 40;

        // Search box and EMI shortcut share the header; stack on narrow widths.
        int headerX = listX + 8;
        int headerY = 10;
        int searchW = stackedHeader ? headerAvailable : Math.min(180, Math.max(120, headerAvailable - 104));
        searchW = Math.min(searchW, headerAvailable);
        searchBox = new EditBox(this.font, headerX, headerY, searchW, 20, Component.translatable("ami.config.search_placeholder"));
        searchBox.setFilter(TextInputFilter::isAllowedInput);
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        if (emiLoaded) {
            int emiX = stackedHeader ? headerX : width - 110;
            int emiY = stackedHeader ? headerY + 24 : headerY;
            int emiW = stackedHeader ? headerAvailable : 100;
            this.addRenderableWidget(Button.builder(Component.translatable("ami.config.emi_config"), b -> {
                try {
                    Class<?> cls = Class.forName("dev.emi.emi.screen.ConfigScreen");
                    this.minecraft.setScreen((Screen) cls.getConstructor(Screen.class).newInstance(this));
                } catch (Exception ignored) {
                }
            }).bounds(emiX, emiY, emiW, 20).build());
        }

        // Sidebar CategoryList.
        // NeoForge 1.21: 5-arg constructor (mc, width, height, y, itemHeight) where height = actual rendered height.
        categories = new CategoryList(this.minecraft, sidebarWidth, height - 40 - contentTop, contentTop, 24);
        this.addRenderableWidget(categories);

        // Main ConfigList. setX() repositions it to start after the sidebar.
        list = new ConfigList(this.minecraft, listWidth, height - 70 - contentTop, contentTop, 25);
        list.setX(listX);
        this.addRenderableWidget(list);

        buildSidebar();
        buildConfigUI();

        // Bottom buttons sized to the list area.
        int availableBtnWidth = Math.max(0, listWidth - 16);
        int spacing = Math.max(2, Math.min(10, availableBtnWidth / 40));
        int btnW = Math.min(100, (availableBtnWidth - (spacing * 2)) / 3);
        btnW = Math.max(24, btnW);
        while ((btnW * 3) + (spacing * 2) > availableBtnWidth && btnW > 16) {
            btnW--;
        }
        if ((btnW * 3) + (spacing * 2) > availableBtnWidth) {
            spacing = 0;
            btnW = availableBtnWidth / 3;
        }
        int totalW = (btnW * 3) + (spacing * 2);
        int startX = listX + 8 + Math.max(0, (availableBtnWidth - totalW) / 2);
        int btnY = height - 25;

        defaultsBtn = this.addRenderableWidget(Button.builder(Component.translatable("ami.config.defaults"), b -> resetToDefaults())
                .bounds(startX, btnY, btnW, 20).build());

        revertBtn = this.addRenderableWidget(Button.builder(getRevertLabel(), b -> revertChanges())
                .bounds(startX + btnW + spacing, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("ami.config.done"), b -> this.onClose())
                .bounds(startX + (btnW + spacing) * 2, btnY, btnW, 20).build());

        updateButtonStates();
    }

    private void buildSidebar() {
        categories.publicClearEntries();
        Map<String, ConfigGroup> seen = new java.util.LinkedHashMap<>();
        for (Field field : AmiConfig.class.getFields()) {
            ConfigGroup group = field.getAnnotation(ConfigGroup.class);
            if (group != null) seen.putIfAbsent(group.value(), group);
        }
        Set<String> visibleGroups = new LinkedHashSet<>();
        seen.values().stream()
                .sorted(Comparator.comparingInt(ConfigGroup::order))
                .filter(g -> !"sidepanels".equals(g.value()))
                .filter(g -> groupHasVisibleEntries(g.value()))
                .forEach(g -> visibleGroups.add(g.value()));
        if (!visibleGroups.contains(activeCategory) && !visibleGroups.isEmpty()) {
            activeCategory = visibleGroups.iterator().next();
            resetConfigScrollOnNextBuild = true;
        }
        seen.values().stream()
                .sorted(Comparator.comparingInt(ConfigGroup::order))
                .filter(g -> !"sidepanels".equals(g.value()))
                .filter(g -> visibleGroups.contains(g.value()))
                .forEach(g -> categories.publicAddEntry(categories.new CategoryEntry(g.value(), g.icon())));
    }

    private Component getRevertLabel() {
        int changes = countChanges();
        return changes > 0 ? Component.translatable("ami.config.revert_count", String.valueOf(changes)) : Component.translatable("ami.config.revert");
    }

    private int countChanges() {
        int count = 0;
        for (var entry : originalValues.entrySet()) {
            try {
                if (!Objects.equals(entry.getKey().get(null), entry.getValue())) {
                    count++;
                }
            } catch (Exception e) {
            }
        }
        return count;
    }

    private void updateButtonStates() {
        if (revertBtn != null) {
            revertBtn.active = countChanges() > 0;
            revertBtn.setMessage(getRevertLabel());
        }
    }

    private void onSettingChanged(Field field, Object value) {
        AMITheme.sync();
        applySearchSourceSettingChange(field, value);
        updateButtonStates();
    }

    private void applySearchSourceSettingChange(Field field, Object value) {
        ConfigValue configValue = field.getAnnotation(ConfigValue.class);
        if (configValue == null) {
            return;
        }

        switch (configValue.value()) {
            case "search.include-guides", "search.include-entities" -> {
                if (Boolean.TRUE.equals(value)) {
                    AmiIndexerService.getInstance().rebuild(false);
                }
                InventoryOverlayHandler.refreshOverlayResults();
            }
            case "search.include-quests" -> {
                FtbQuestsRuntimeCompat.refreshNow();
                InventoryOverlayHandler.refreshOverlayResults();
            }
            case "search.include-advancements", "search.include-players", "search.include-waypoints" ->
                    InventoryOverlayHandler.refreshOverlayResults();
            default -> {
            }
        }
    }

    private void applySearchSourceSettingsChanged() {
        FtbQuestsRuntimeCompat.refreshNow();
        if (AmiConfig.searchIncludeGuides || AmiConfig.searchIncludeEntities) {
            AmiIndexerService.getInstance().rebuild(false);
        }
        InventoryOverlayHandler.refreshOverlayResults();
    }

    private void onConfigDropdownOpened(AmiDropdownPopup dropdown) {
        if (openDropdown != null && openDropdown != dropdown) {
            openDropdown.close();
        }
        openDropdown = dropdown;
    }

    private void renderOpenDropdown(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (openDropdown != null && openDropdown.isOpen()) {
            openDropdown.renderDropdownList(g, mouseX, mouseY, partialTick);
        }
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.toLowerCase();
        resetConfigScrollOnNextBuild = true;
        buildConfigUI();
    }

    private void resetToDefaults() {
        AmiConfig.resetToDefaults();
        AMITheme.sync();
        applySearchSourceSettingsChanged();
        AmiConfigStore.save();
        captureOriginalValues();
        resetConfigScrollOnNextBuild = true;
        buildConfigUI();
        updateButtonStates();
    }

    private void revertChanges() {
        for (var entry : originalValues.entrySet()) {
            try {
                entry.getKey().set(null, entry.getValue());
            } catch (Exception e) {
            }
        }
        AMITheme.sync();
        applySearchSourceSettingsChanged();
        AmiConfigStore.save();
        captureOriginalValues();
        resetConfigScrollOnNextBuild = true;
        buildConfigUI();
        updateButtonStates();
    }

    private void buildConfigUI() {
        focusedConfigEntry = null;
        openDropdown = null;
        list.publicClearEntries();
        String currentGroup = null;
        String currentCompatSection = null;
        boolean groupVisible = false;

        for (Field field : AmiConfig.class.getFields()) {
            ConfigGroup group = field.getAnnotation(ConfigGroup.class);
            if (group != null) {
                if ("layout".equals(currentGroup) && groupVisible) {
                    addLayoutPanelSectionEntries();
                }
                currentGroup = group.value();
                currentCompatSection = null;
                groupVisible = !"sidepanels".equals(currentGroup)
                        && (searchQuery.isEmpty() ? currentGroup.equals(activeCategory) : true)
                        && groupHasRenderableEntries(currentGroup);

                if (groupVisible) {
                    Component groupText = Component.translatable("ami.config.group." + currentGroup);
                    list.publicAddEntry(list.new HeaderEntry(groupText));

                    if ("layout".equals(currentGroup)) {
                        addCustomizeLayoutEntry();
                    }
                }
            }

            if (groupVisible) {
                ConfigValue value = field.getAnnotation(ConfigValue.class);
                if (isVisibleConfigValue(field) && !isSidePanelField(field)) {
                    Component valueText = Component.translatable("ami.config.value." + value.value());
                    String compatSection = "compat".equals(currentGroup) ? compatSection(value.value()) : null;
                    Component compatSectionText = compatSection == null ? Component.empty() : Component.translatable("ami.config.compat." + compatSection);
                    boolean matchesSearch = searchQuery.isEmpty()
                            || valueText.getString().toLowerCase().contains(searchQuery)
                            || compatSectionText.getString().toLowerCase().contains(searchQuery);
                    if (matchesSearch) {
                        if (compatSection != null && !compatSection.equals(currentCompatSection)) {
                            currentCompatSection = compatSection;
                            list.publicAddEntry(list.new CompatSectionEntry(compatSectionText));
                        }
                        list.publicAddEntry(list.new SettingEntry(valueText, field));
                    }
                }
            }

            if (field.isAnnotationPresent(ConfigGroupEnd.class) && "binds".equals(currentGroup) && groupVisible) {
                addKeyMappingEntries();
            }
        }
        if ("layout".equals(currentGroup) && groupVisible) {
            addLayoutPanelSectionEntries();
        }
        if (resetConfigScrollOnNextBuild) {
            list.setScrollAmount(0.0);
            resetConfigScrollOnNextBuild = false;
        }
    }

    private void addKeyMappingEntries() {
        for (net.minecraft.client.KeyMapping keyMapping : Services.PLATFORM.keyMappings().all()) {
            String keybindName = keyMapping.getName();

            if ("key.ami.recipe_back".equals(keybindName) && !isNativeRecipeViewerActive()) {
                continue;
            }

            if ("key.ami.debug_tooltips".equals(keybindName) && !Services.PLATFORM.supportsDebugTooltipToggle()) {
                continue;
            }

            Component keyLabel = Component.translatable(keybindName);
            if (searchQuery.isEmpty() || keyLabel.getString().toLowerCase().contains(searchQuery)) {
                list.publicAddEntry(list.new KeybindEntry(keyLabel, keyMapping));
            }
        }
    }

    private boolean isNativeRecipeViewerActive() {
        if (AmiConfig.recipeViewerMode == AmiConfig.RecipeViewerMode.NATIVE) return true;
        if (AmiConfig.recipeViewerMode == AmiConfig.RecipeViewerMode.EMI_JEI) return false;
        return !Services.PLATFORM.isModLoaded("emi") && !Services.PLATFORM.isModLoaded("jei");
    }

    private void addCustomizeLayoutEntry() {
        list.publicAddEntry(list.new CustomizeLayoutEntry());
    }

    private void addSidePanelEditorEntries() {
        list.publicAddEntry(list.new PanelTitleEntry(Component.translatable("ami.config.panel.left"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.left"))));
        list.publicAddEntry(list.new SidePanelWidthEntry(Component.translatable("ami.config.panel.width"), field("leftPanelWidth")));
        list.publicAddEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.normal_view"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.normal_view"))));
        addSlotListEntries(field("leftPanelSlots"));
        list.publicAddEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.toggled_view"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.toggled_view"))));
        addSlotListEntries(field("leftPanelAlternateSlots"));

        list.publicAddEntry(list.new PanelTitleEntry(Component.translatable("ami.config.panel.right"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.right"))));
        list.publicAddEntry(list.new SidePanelWidthEntry(Component.translatable("ami.config.panel.width"), field("rightPanelWidth")));
        list.publicAddEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.normal_view"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.normal_view"))));
        addSlotListEntries(field("rightPanelSlots"));
        list.publicAddEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.toggled_view"), Tooltip.create(Component.translatable("ami.config.tooltip.panel.toggled_view"))));
        addSlotListEntries(field("rightPanelAlternateSlots"));
    }

    private void addLayoutPanelSectionEntries() {
        if (!panelEditorMatchesSearch(true)) {
            return;
        }
        list.publicAddEntry(list.new CompatSectionEntry(Component.translatable("ami.config.panel.section")));
        addSidePanelEditorEntries();
    }

    private void addSlotListEntries(Field slotsField) {
        List<AmiConfig.PanelContent> slots = readSlots(slotsField);
        Tooltip tooltip = tooltipForConfigField(slotsField);
        if (slots.isEmpty()) {
            list.publicAddEntry(list.new PanelEmptySlotsEntry(Component.translatable("ami.config.panel.no_slots"), tooltip));
        }
        for (int i = 0; i < slots.size(); i++) {
            list.publicAddEntry(list.new PanelSlotEntry(Component.translatable("ami.config.panel.slot", i + 1), slotsField, i, tooltip));
        }
        list.publicAddEntry(list.new PanelAddSlotEntry(Component.translatable("ami.config.panel.add_slot"), slotsField, Tooltip.create(Component.translatable("ami.config.tooltip.panel.add_slot"))));
    }

    private Field field(String name) {
        try {
            return AmiConfig.class.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing AmiConfig field " + name, e);
        }
    }

    private boolean isSidePanelField(Field field) {
        ConfigValue value = field.getAnnotation(ConfigValue.class);
        return value != null && value.value().startsWith("sidepanels.");
    }

    private String compatSection(String configKey) {
        if (!configKey.startsWith("compat.")) {
            return null;
        }
        int start = "compat.".length();
        int end = configKey.indexOf('.', start);
        return end > start ? configKey.substring(start, end) : null;
    }

    private boolean isVisibleConfigValue(Field field) {
        return field.getAnnotation(ConfigValue.class) != null && !field.isAnnotationPresent(AmiConfig.ConfigHidden.class);
    }

    private boolean groupHasVisibleEntries(String groupId) {
        if ("sidepanels".equals(groupId)) {
            return false;
        }
        if ("binds".equals(groupId)) {
            return true;
        }
        return groupHasMatchingConfigValue(groupId, false) || ("layout".equals(groupId) && panelEditorMatchesSearch(false));
    }

    private boolean groupHasRenderableEntries(String groupId) {
        if ("sidepanels".equals(groupId)) {
            return false;
        }
        if ("binds".equals(groupId)) {
            return true;
        }
        return groupHasMatchingConfigValue(groupId, true) || ("layout".equals(groupId) && panelEditorMatchesSearch(true));
    }

    private boolean groupHasMatchingConfigValue(String groupId, boolean applySearch) {
        String currentGroup = null;
        for (Field field : AmiConfig.class.getFields()) {
            ConfigGroup group = field.getAnnotation(ConfigGroup.class);
            if (group != null) {
                currentGroup = group.value();
            }
            if (!groupId.equals(currentGroup) || !isVisibleConfigValue(field) || isSidePanelField(field)) {
                continue;
            }
            ConfigValue value = field.getAnnotation(ConfigValue.class);
            Component valueText = Component.translatable("ami.config.value." + value.value());
            if (!applySearch || searchQuery.isEmpty() || valueText.getString().toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean panelEditorMatchesSearch(boolean applySearch) {
        if (!applySearch || searchQuery.isEmpty()) {
            return true;
        }
        String[] keys = {
                "ami.config.panel.section",
                "ami.config.panel.left",
                "ami.config.panel.right",
                "ami.config.panel.width",
                "ami.config.panel.normal_view",
                "ami.config.panel.toggled_view",
                "ami.config.panel.slot",
                "ami.config.panel.add_slot",
                "ami.config.panel.no_slots"
        };
        for (String key : keys) {
            if (Component.translatable(key, "1").getString().toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        for (AmiConfig.PanelContent content : selectablePanelContents()) {
            if (panelContentLabel(content).toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDependency(Field field) {
        ConfigDependsOn dep = field.getAnnotation(ConfigDependsOn.class);
        if (dep == null) return true;
        try {
            Field target = AmiConfig.class.getField(dep.value());
            Object val = target.get(null);
            return String.valueOf(val).equalsIgnoreCase(dep.expected());
        } catch (Exception e) {
            return true;
        }
    }

    private Tooltip tooltipForConfigField(Field field) {
        ConfigValue value = field.getAnnotation(ConfigValue.class);
        return value == null ? null : Tooltip.create(Component.translatable("ami.config.tooltip." + value.value()));
    }

    private boolean isHoverBlockedByOpenDropdown(double mouseX, double mouseY) {
        return AmiDropdownPopupController.blocksUnderlyingHover(openDropdown, mouseX, mouseY);
    }

    private void showTooltipIfHovered(Tooltip tooltip, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (tooltip != null
                && !isHoverBlockedByOpenDropdown(mouseX, mouseY)
                && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            this.setTooltipForNextRenderPass(tooltip, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
        }
    }

    private void renderTextWithHighlight(GuiGraphics g, Component text, int x, int y, int color) {
        String raw = text.getString();
        if (searchQuery.isEmpty() || !raw.toLowerCase().contains(searchQuery)) {
            g.drawString(this.font, text, x, y, color);
            return;
        }

        int idx = raw.toLowerCase().indexOf(searchQuery);
        String pre = raw.substring(0, idx);
        String match = raw.substring(idx, idx + searchQuery.length());
        String post = raw.substring(idx + searchQuery.length());

        int curX = x;
        g.drawString(this.font, pre, curX, y, color);
        curX += font.width(pre);
        g.drawString(this.font, match, curX, y, AMITheme.ACCENT_BLUE);
        curX += font.width(match);
        g.drawString(this.font, post, curX, y, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (AmiDropdownPopupController.mouseClicked(
                () -> openDropdown,
                dropdown -> openDropdown = dropdown,
                mouseX,
                mouseY,
                button
        )) {
            return true;
        }
        focusedConfigEntry = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (focusedConfigEntry != null && focusedConfigEntry.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (focusedConfigEntry != null && focusedConfigEntry.mouseReleased(mouseX, mouseY, button)) {
            focusedConfigEntry = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedConfigEntry != null && focusedConfigEntry.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedConfigEntry != null && focusedConfigEntry.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // Draw sidebar backdrop after the screen background but before widgets.
        g.fill(0, 0, sidebarWidth, height, AMITheme.SIDEBAR_BG);
        g.fill(sidebarWidth, 0, sidebarWidth + 1, height, AMITheme.CONFIG_SEP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        // Brand text rendered after widgets so it's not clipped by lists.
        g.drawString(this.font, Component.translatable("ami.config.brand_top"), 8, 8, AMITheme.CONFIG_BRAND_GOLD);
        g.drawString(this.font, Component.translatable("ami.config.brand_bottom"), 8, 18, AMITheme.CONFIG_TEXT_PRIMARY);
        String version = Services.PLATFORM.getModVersion(AmiCore.MODID).map(v -> "v" + v).orElse("");
        if (!version.isEmpty()) {
            g.drawString(this.font, version, 8, 28, AMITheme.CONFIG_TEXT_MUTED, false);
        }
        renderOpenDropdown(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        AmiConfigStore.save();
        AMITheme.sync();
        this.minecraft.setScreen(parent);
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    class CategoryList extends ObjectSelectionList<CategoryList.CategoryEntry> {
        public CategoryList(net.minecraft.client.Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 4;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
            // Sidebar background is drawn in renderBackground; suppress list texture.
        }

        public void publicClearEntries() {
            super.clearEntries();
        }

        public int publicAddEntry(CategoryEntry entry) {
            return super.addEntry(entry);
        }

        class CategoryEntry extends ObjectSelectionList.Entry<CategoryEntry> {
            private final String id;
            private final String icon;
            private final Component label;

            CategoryEntry(String id, String icon) {
                this.id = id;
                this.icon = icon;
                this.label = Component.translatable("ami.config.group." + id);
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                boolean active = activeCategory.equals(id);
                if (active) {
                    g.fill(x, y, x + width, y + height, AMITheme.SIDEBAR_SELECTION);
                    g.fill(x, y, x + 2, y + height, AMITheme.ACCENT_BLUE);
                } else if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    g.fill(x, y, x + width, y + height, AMITheme.SIDEBAR_TOGGLE_IDLE_HALO);
                }

                int iconColor = active ? AMITheme.SIDEBAR_TEXT_ACTIVE : AMITheme.SIDEBAR_TEXT;
                drawIcon(g, x + 10, y + height / 2, iconColor);

                String labelStr = label.getString();
                int maxLabelW = width - 25;
                if (font.width(labelStr) > maxLabelW) {
                    labelStr = font.plainSubstrByWidth(labelStr, maxLabelW - 8) + "..";
                }
                g.drawString(font, labelStr, x + 22, y + 7, iconColor, false);
            }

            private void drawIcon(GuiGraphics g, int cx, int cy, int color) {
                switch (icon) {
                    case "general" -> AmiGuiIcons.general(g, cx, cy, color);
                    case "display" -> AmiGuiIcons.display(g, cx, cy, color);
                    case "interaction" -> AmiGuiIcons.interaction(g, cx, cy, color);
                    case "layout" -> AmiGuiIcons.layout(g, cx, cy, color);
                    case "palette" -> AmiGuiIcons.palette(g, cx, cy, color);
                    case "sidepanels" -> AmiGuiIcons.sidepanels(g, cx, cy, color);
                    case "keybinds" -> AmiGuiIcons.keybinds(g, cx, cy, color);
                    case "cheat" -> AmiGuiIcons.cheat(g, cx, cy, color);
                    case "subtitles" -> AmiGuiIcons.subtitles(g, cx, cy, color);
                    default -> AmiGuiIcons.compact(g, cx, cy, color);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (!activeCategory.equals(id)) {
                    resetConfigScrollOnNextBuild = true;
                }
                activeCategory = id;
                buildConfigUI();
                return true;
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }
    }

    class ConfigList extends ObjectSelectionList<ConfigList.ConfigEntry> {
        public ConfigList(net.minecraft.client.Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        public void publicClearEntries() {
            super.clearEntries();
        }

        public int publicAddEntry(ConfigEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(320, this.width - 25);
        }

        @Override
        protected int getScrollbarPosition() {
            return AmiConfigScreen.this.listX + this.width - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
            // Screen background is sufficient; suppress list texture.
        }

        abstract class ConfigEntry extends ObjectSelectionList.Entry<ConfigEntry> {
            protected void renderCard(GuiGraphics g, int x, int y, int w, int h) {
                AMITheme.fillRounded(g, x, y, w, h, AMITheme.CONFIG_CARD_BG);
            }
        }

        class HeaderEntry extends ConfigEntry {
            private final Component text;

            HeaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(font, text, x + 5, y + 8, AMITheme.CONFIG_HEADER_GOLD, false);
                g.fill(x + 5, y + 18, x + width - 5, y + 19, AMITheme.CONFIG_SEP);
            }

            @Override
            public Component getNarration() {
                return text;
            }
        }

        class SettingEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final Tooltip tooltip;
            private final AbstractWidget widget;

            SettingEntry(Component label, Field field) {
                this.label = label;
                this.field = field;
                String tooltipKey = "ami.config.tooltip." + field.getAnnotation(ConfigValue.class).value();
                this.tooltip = Tooltip.create(Component.translatable(tooltipKey));
                this.widget = AmiWidgetFactory.createWidget(field, o -> onSettingChanged(field, o), AmiConfigScreen.this::onConfigDropdownOpened);
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                renderCard(g, x, y, width, height);
                boolean enabled = checkDependency(field);
                int textColor = enabled ? AMITheme.CONFIG_TEXT_PRIMARY : AMITheme.CONFIG_TEXT_MUTED;

                int widgetW = Math.min(widget instanceof AmiEnumDropdownWidget || field.isAnnotationPresent(ConfigSlider.class) ? 110 : 75, width / 3);
                int widgetX = x + width - (field.isAnnotationPresent(ConfigColor.class) ? widgetW + 28 : widgetW + 8);

                String labelStr = label.getString();
                int maxLabelW = widgetX - x - 15;
                if (font.width(labelStr) > maxLabelW) {
                    labelStr = font.plainSubstrByWidth(labelStr, maxLabelW - 8) + "..";
                }
                renderTextWithHighlight(g, Component.literal(labelStr), x + 10, y + 6, textColor);

                if (widget != null) {
                    widget.active = enabled;
                    widget.setX(widgetX);
                    widget.setY(y + 3);
                    widget.setWidth(widgetW);
                    widget.render(g, mouseX, mouseY, partialTick);

                    if (field.isAnnotationPresent(ConfigColor.class)) {
                        int color = 0;
                        try {
                            color = field.getInt(null);
                        } catch (Exception ignored) {
                        }
                        renderColorSwatch(g, x + width - 22, y + 4, color);
                        g.renderOutline(x + width - 23, y + 3, 19, 18, 0xFFFFFFFF);
                    }
                }

                if (enabled
                        && !isHoverBlockedByOpenDropdown(mouseX, mouseY)
                        && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    AmiConfigScreen.this.setTooltipForNextRenderPass(this.tooltip, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (widget != null && widget.active && field.isAnnotationPresent(ConfigColor.class) && isColorSwatchMouseOver(mouseX, mouseY)) {
                    openColorPicker();
                    return true;
                }
                if (widget != null && widget.active && widget.mouseClicked(mouseX, mouseY, button)) {
                    widget.setFocused(true);
                    AmiConfigScreen.this.focusedConfigEntry = this;
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return widget != null && widget.active && widget.isFocused()
                        && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (widget != null && widget.mouseReleased(mouseX, mouseY, button)) {
                    widget.setFocused(false);
                    return true;
                }
                return false;
            }

            private boolean isColorSwatchMouseOver(double mouseX, double mouseY) {
                int swatchX = widget.getX() + widget.getWidth() + 5;
                return mouseX >= swatchX && mouseX <= swatchX + 19
                        && mouseY >= widget.getY() && mouseY <= widget.getY() + 18;
            }

            private void openColorPicker() {
                try {
                    int initial = field.getInt(null);
                    minecraft.setScreen(new AmiColorPickerScreen(AmiConfigScreen.this, field, initial, c -> {
                        AMITheme.sync();
                        buildConfigUI();
                    }));
                } catch (Exception ignored) {
                }
            }

            private void renderColorSwatch(GuiGraphics g, int x, int y, int color) {
                int preview = isAlphaColorField() || (color >>> 24) != 0 ? color : 0xFF000000 | color;
                g.fill(x, y, x + 17, y + 16, 0xFFE6E6E6);
                g.fill(x, y, x + 8, y + 8, 0xFF9A9A9A);
                g.fill(x + 8, y + 8, x + 17, y + 16, 0xFF9A9A9A);
                g.fill(x, y, x + 17, y + 16, preview);
            }

            private boolean isAlphaColorField() {
                ConfigValue value = field.getAnnotation(ConfigValue.class);
                return value != null && value.value().startsWith("palette.");
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                return widget != null && widget.charTyped(codePoint, modifiers);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return widget != null && widget.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }

        class PanelTitleEntry extends ConfigEntry {
            private final Component text;
            private final Tooltip tooltip;

            PanelTitleEntry(Component text, Tooltip tooltip) {
                this.text = text;
                this.tooltip = tooltip;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(font, text, x + 5, y + 6, AMITheme.CONFIG_PANEL_TITLE, false);
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public Component getNarration() {
                return text;
            }
        }

        class PanelSubheaderEntry extends ConfigEntry {
            private final Component text;
            private final Tooltip tooltip;

            PanelSubheaderEntry(Component text, Tooltip tooltip) {
                this.text = text;
                this.tooltip = tooltip;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(font, text, x + 15, y + 6, AMITheme.CONFIG_TEXT_SECONDARY, false);
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public Component getNarration() {
                return text;
            }
        }

        class CompatSectionEntry extends ConfigEntry {
            private final Component text;

            CompatSectionEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                int lineY = y + 13;
                g.drawString(font, text, x + 10, y + 6, AMITheme.CONFIG_PANEL_TITLE, false);
                g.fill(x + 10 + font.width(text) + 8, lineY, x + width - 5, lineY + 1, AMITheme.CONFIG_SEP);
            }

            @Override
            public Component getNarration() {
                return text;
            }
        }

        class PanelEmptySlotsEntry extends ConfigEntry {
            private final Component label;
            private final Tooltip tooltip;

            PanelEmptySlotsEntry(Component label, Tooltip tooltip) {
                this.label = label;
                this.tooltip = tooltip;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(font, label, x + 30, y + 6, AMITheme.CONFIG_TEXT_MUTED, false);
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }

        class PanelSlotEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final int slotIndex;
            private final AmiPanelContentDropdownWidget dropdown;
            private final Button removeButton;
            private final Tooltip tooltip;

            PanelSlotEntry(Component label, Field field, int slotIndex, Tooltip tooltip) {
                this.label = label;
                this.field = field;
                this.slotIndex = slotIndex;
                this.tooltip = tooltip;
                this.dropdown = new AmiPanelContentDropdownWidget(
                        currentContent(),
                        selectablePanelContents(),
                        this::setSlot,
                        AmiConfigScreen.this::onConfigDropdownOpened
                );
                this.removeButton = Button.builder(Component.literal("-"), b -> remove()).bounds(0, 0, 20, 18).build();
            }

            private AmiConfig.PanelContent currentContent() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex < 0 || slotIndex >= slots.size()) return AmiConfig.PanelContent.EMPTY;
                return slots.get(slotIndex);
            }

            private void setSlot(AmiConfig.PanelContent content) {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex < 0 || slotIndex >= slots.size()) return;
                slots.set(slotIndex, content);
                writeSlots(field, slots);
                AmiConfigScreen.this.buildConfigUI();
                AmiConfigScreen.this.updateButtonStates();
            }

            private void remove() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex >= 0 && slotIndex < slots.size()) {
                    slots.remove(slotIndex);
                    writeSlots(field, slots);
                    AmiConfigScreen.this.buildConfigUI();
                    AmiConfigScreen.this.updateButtonStates();
                }
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                renderCard(g, x, y, width, height);
                g.drawString(font, label, x + 30, y + 6, AMITheme.CONFIG_TEXT_PRIMARY, false);

                int removeX = x + Math.max(0, width - 25);
                removeButton.setX(removeX);
                removeButton.setY(y + 3);
                removeButton.render(g, mouseX, mouseY, partialTick);

                int dropdownW = Math.max(72, Math.min(118, width - 110));
                int dropdownX = removeX - dropdownW - 4;
                dropdown.setX(dropdownX);
                dropdown.setY(y + 3);
                dropdown.setWidth(dropdownW);
                dropdown.render(g, mouseX, mouseY, partialTick);
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.removeButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (this.dropdown.mouseClicked(mouseX, mouseY, button)) return true;
                return false;
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }

        class CustomizeLayoutEntry extends ConfigEntry {
            private final Button button;

            CustomizeLayoutEntry() {
                this.button = Button.builder(
                        Component.translatable("ami.config.value.layout.customize-layout"),
                        b -> {
                            InventoryOverlayHandler.getManager().setLayoutMode(true);
                            AmiConfigScreen.this.onClose();
                        }
                ).bounds(0, 0, 120, 18).build();
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                renderCard(g, x, y, width, height);
                int btnW = Math.min(140, width - 20);
                button.setX(x + 10);
                button.setY(y + 3);
                button.setWidth(btnW);
                button.render(g, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return button.getMessage();
            }
        }

        class PanelAddSlotEntry extends ConfigEntry {
            private final Button button;
            private final Tooltip tooltip;

            PanelAddSlotEntry(Component label, Field field, Tooltip tooltip) {
                this.tooltip = tooltip;
                this.button = Button.builder(label, b -> {
                    List<AmiConfig.PanelContent> slots = readSlots(field);
                    slots.add(AmiConfig.PanelContent.EMPTY);
                    writeSlots(field, slots);
                    AmiConfigScreen.this.buildConfigUI();
                    AmiConfigScreen.this.updateButtonStates();
                }).bounds(0, 0, 80, 18).build();
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                int btnW = Math.max(24, Math.min(80, width - 36));
                button.setX(x + 30);
                button.setY(y + 3);
                button.setWidth(btnW);
                button.render(g, mouseX, mouseY, partialTick);
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal("Add Slot");
            }
        }

        class SidePanelWidthEntry extends ConfigEntry {
            private final Component label;
            private final AbstractWidget widget;
            private final Tooltip tooltip;

            SidePanelWidthEntry(Component label, Field field) {
                this.label = label;
                this.tooltip = tooltipForConfigField(field);
                this.widget = AmiWidgetFactory.createWidget(field, o -> onSettingChanged(field, o));
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                renderCard(g, x, y, width, height);
                g.drawString(font, label, x + 15, y + 6, AMITheme.CONFIG_TEXT_PRIMARY, false);
                if (widget != null) {
                    int btnW = Math.min(75, width / 3);
                    widget.setX(x + width - btnW - 8);
                    widget.setY(y + 3);
                    widget.setWidth(btnW);
                    widget.render(g, mouseX, mouseY, partialTick);
                }
                showTooltipIfHovered(tooltip, x, y, width, height, mouseX, mouseY);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (widget != null && widget.mouseClicked(mouseX, mouseY, button)) {
                    widget.setFocused(true);
                    AmiConfigScreen.this.focusedConfigEntry = this;
                    return true;
                }
                return false;
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                return widget != null && widget.charTyped(codePoint, modifiers);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return widget != null && widget.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }

        class KeybindEntry extends ConfigEntry {
            private final Component label;
            private final net.minecraft.client.KeyMapping keyMapping;
            private final Button button;

            KeybindEntry(Component label, net.minecraft.client.KeyMapping keyMapping) {
                this.label = label;
                this.keyMapping = keyMapping;
                this.button = Button.builder(keyMapping.getTranslatedKeyMessage(), b -> {
                    minecraft.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(AmiConfigScreen.this, minecraft.options));
                }).bounds(0, 0, 80, 18).build();
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                renderCard(g, x, y, width, height);

                int btnW = Math.min(80, width / 3);
                int btnX = x + width - btnW - 8;

                String labelStr = label.getString();
                int maxLabelW = btnX - x - 15;
                if (font.width(labelStr) > maxLabelW) {
                    labelStr = font.plainSubstrByWidth(labelStr, maxLabelW - 8) + "..";
                }
                renderTextWithHighlight(g, Component.literal(labelStr), x + 10, y + 6, AMITheme.CONFIG_TEXT_PRIMARY);

                button.setX(btnX);
                button.setY(y + 3);
                button.setWidth(btnW);
                button.setMessage(keyMapping.getTranslatedKeyMessage());
                button.render(g, mouseX, mouseY, 0);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return label;
            }
        }
    }
}
