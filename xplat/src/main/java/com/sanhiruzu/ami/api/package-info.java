/**
 * Public API for integrating with AMI (Automated Materials Index).
 *
 * <h2>Getting started</h2>
 *
 * <h3>1. Which registration path?</h3>
 * <ul>
 *   <li><b>Viewer-neutral (recommended for most things):</b> Register a
 *       {@code com.sanhiruzu.searchableitems.api.SearchableItemProvider} or
 *       {@code com.sanhiruzu.searchableitems.api.SearchableItemActionProvider} via
 *       {@link com.sanhiruzu.ami.api.AmiApi}. These integrations are also picked up
 *       by other item viewer mods that adopt the shared provider API.</li>
 *   <li><b>AMI-specific:</b> Implement {@link com.sanhiruzu.ami.api.IAmiPlugin} for
 *       features unique to AMI: overlay exclusion zones, hero items for modular-item
 *       mods, and custom result context-menu actions.</li>
 * </ul>
 *
 * <h3>2. How to register an IAmiPlugin</h3>
 * <p>The cleanest approach is the Java ServiceLoader. Create a file at
 * {@code META-INF/services/com.sanhiruzu.ami.api.IAmiPlugin} inside your jar
 * containing the fully-qualified name of your implementation class. AMI discovers
 * it automatically at startup with no explicit registration call required.</p>
 * <p>Alternatively, call {@link com.sanhiruzu.ami.api.AmiPluginRegistry#register}
 * during your mod's client initialisation.</p>
 *
 * <h3>3. What can I contribute?</h3>
 * <ul>
 *   <li><b>Item metadata enrichment</b> —
 *       {@link com.sanhiruzu.ami.api.IAmiPlugin#enrichItemMeta}</li>
 *   <li><b>Result context-menu actions</b> —
 *       {@link com.sanhiruzu.ami.api.IAmiPlugin#addItemContextMenuActions}</li>
 *   <li><b>Hero / representative stacks</b> for modular-item mods —
 *       {@link com.sanhiruzu.ami.api.IAmiPlugin#getHeroItems}</li>
 *   <li><b>Overlay exclusion zones</b> —
 *       {@link com.sanhiruzu.ami.api.IAmiPlugin#getExclusionZones} or
 *       {@link com.sanhiruzu.ami.api.AmiApi#registerScreenSuppressor}</li>
 *   <li><b>Guide documents</b> —
 *       {@link com.sanhiruzu.ami.api.IAmiPlugin#addGuideDocuments} or
 *       the viewer-neutral
 *       {@link com.sanhiruzu.ami.api.AmiApi#registerSearchableGuideProvider}</li>
 *   <li><b>Quest / task data</b> —
 *       {@link com.sanhiruzu.ami.api.AmiApi#registerQuestDocument}</li>
 * </ul>
 */
package com.sanhiruzu.ami.api;
