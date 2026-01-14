# Bottomless Inventory - Implementation Plan

This plan implements the Bottomless Inventory mod as specified in `.claude/SPEC.md`. Each phase represents a logical grouping, and each step is commit-sized and independently testable.

**Target:** Minecraft 1.21.10 with Fabric Loader

---

## Phase 1: Project Setup & Core Data Structure

### Step 1.1: Update Project Metadata
**Files:** `fabric.mod.json`, `gradle.properties`
- Update mod description to match SPEC
- Update authors and contact info
- Verify Minecraft version is 1.21.10
- Add Mod Menu dependency for configuration (optional dependency)

### Step 1.2: Create Infinite Inventory Data Structure
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/inventory/InfiniteInventory.java`
- `src/main/java/com/chayut/bottomlessinventory/inventory/InfiniteInventoryEntry.java`

**Implementation:**
- Create `InfiniteInventoryEntry` class to hold:
  - `ItemStack` (with NBT) as the key identifier
  - `long count` (up to Integer.MAX_VALUE per spec, but long for safety)
- Create `InfiniteInventory` class with:
  - `Map<ItemStackKey, InfiniteInventoryEntry>` for item storage
  - Methods: `addItem()`, `removeItem()`, `getCount()`, `getAllEntries()`
  - NBT matching logic (items with different NBT are separate entries)
- Create `ItemStackKey` helper class for proper HashMap key behavior (matches by item type + NBT)

### Step 1.3: Implement NBT Serialization
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/inventory/InfiniteInventory.java` (add methods)
- `src/main/java/com/chayut/bottomlessinventory/inventory/InfiniteInventorySerializer.java`

**Implementation:**
- `toNbt()` method to serialize entire inventory to CompoundTag
- `fromNbt()` method to deserialize from CompoundTag
- Handle edge cases: empty inventory, corrupted data
- Version tag for future migration support

### Step 1.4: Attach Data to Player
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/data/PlayerDataAttachment.java`
- `src/main/java/com/chayut/bottomlessinventory/mixin/PlayerMixin.java` (or use Fabric API attachments)

**Implementation:**
- Use Fabric API's `AttachmentType` system to attach `InfiniteInventory` to players
- Register attachment type in mod initializer
- Ensure data persists with world save
- Data tied to player UUID

---

## Phase 2: Networking Foundation

### Step 2.1: Define Network Packets
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/network/BottomlessNetworking.java`
- `src/main/java/com/chayut/bottomlessinventory/network/packets/SyncInventoryPacket.java`
- `src/main/java/com/chayut/bottomlessinventory/network/packets/InventoryActionPacket.java`

**Implementation:**
- `SyncInventoryPacket`: Server → Client, sends full or partial inventory state
- `InventoryActionPacket`: Client → Server, requests item transfer/action
- Register packet handlers using Fabric Networking API
- Include packet versioning for future compatibility

### Step 2.2: Implement Sync Logic
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/network/InventorySyncHandler.java`

**Implementation:**
- Full sync on player join/respawn
- Incremental sync on inventory changes
- Server-side validation of all client requests
- Rate limiting to prevent spam

### Step 2.3: Client-Side Inventory Cache
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/ClientInventoryCache.java`

**Implementation:**
- Local cache of infinite inventory for rendering
- Updated via sync packets
- Methods to query items for GUI display

---

## Phase 3: Basic GUI Infrastructure

### Step 3.1: Create Base Screen Handler
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/screen/BottomlessScreenHandler.java`
- `src/main/java/com/chayut/bottomlessinventory/screen/BottomlessScreenHandlerFactory.java`

**Implementation:**
- Extend `ScreenHandler` for the bottomless inventory screen
- Include slots for: armor (4), offhand (1), hotbar (9), 2x2 crafting (4+1 output)
- Virtual slots concept for infinite inventory grid (no actual Slot objects)
- Register screen handler type

### Step 3.2: Create Client Screen Base
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/BottomlessInventoryScreen.java`

**Implementation:**
- Extend `HandledScreen`
- Basic layout skeleton with left panel (vanilla elements) and right panel (infinite grid)
- Render background texture placeholders
- Register screen to handler mapping

### Step 3.3: Implement Scrollable Item Grid Widget
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/InfiniteGridWidget.java`

**Implementation:**
- 9-column scrollable grid widget
- Mouse scroll handling
- Click handling (left, right, shift-click)
- Item rendering with quantity display
- Abbreviated numbers (10K, 1M) with full count in tooltip

### Step 3.4: Override Default Inventory Key
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/BottomlessInventoryClient.java`
- `src/client/java/com/chayut/bottomlessinventory/mixin/client/KeybindingMixin.java` (or use Fabric keybind API)

**Implementation:**
- Intercept E key press
- Open `BottomlessInventoryScreen` instead of vanilla inventory
- Ensure vanilla inventory is never opened in normal gameplay

---

## Phase 4: GUI - Vanilla Elements (Left Panel)

### Step 4.1: Armor and Offhand Slots
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/BottomlessInventoryScreen.java` (update)
- `src/main/java/com/chayut/bottomlessinventory/screen/BottomlessScreenHandler.java` (update)

**Implementation:**
- Add armor slot rendering (4 slots, left side)
- Add offhand slot rendering
- Use vanilla slot textures and behavior
- Shift-click from grid to equipment slots

### Step 4.2: 2x2 Crafting Grid
**Files:**
- Update screen handler and screen

**Implementation:**
- Add 2x2 crafting input slots
- Add crafting output slot
- Wire up vanilla crafting logic
- Shift-click output moves result to infinite storage

### Step 4.3: Recipe Book Button
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/RecipeBookButtonWidget.java`
- Update main screen

**Implementation:**
- Add recipe book toggle button
- Visual indicator when recipe book/crafting mode is active
- Clicking toggles crafting mode state

### Step 4.4: Hotbar Display
**Files:**
- Update screen

**Implementation:**
- Render hotbar slots at bottom (9 slots)
- Full vanilla behavior for hotbar slots
- Shift-click between hotbar and infinite storage

---

## Phase 5: GUI - Tab System

### Step 5.1: Create Tab Data Structure
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/tabs/InventoryTab.java`
- `src/main/java/com/chayut/bottomlessinventory/tabs/TabRegistry.java`

**Implementation:**
- `InventoryTab` class with: id, display name, icon, item filter predicate
- `TabRegistry` to collect and manage all tabs
- Include all vanilla creative tabs (Building Blocks, Colored Blocks, Natural, Functional, Redstone, Tools, Combat, Food, Ingredients, Spawn Eggs)
- "Uncategorized" tab for items without creative tab assignment

### Step 5.2: Tab Widget Implementation
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/TabWidget.java`
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/TabRowWidget.java`

**Implementation:**
- Individual tab button widget
- Tab row container (top and bottom)
- Page navigation arrows when tabs exceed visible space
- Tab selection state management
- Visual styling matching creative mode

### Step 5.3: Integrate Tabs with Grid
**Files:**
- Update `InfiniteGridWidget.java`
- Update `BottomlessInventoryScreen.java`

**Implementation:**
- Filter grid items based on selected tab
- Show ALL items in category (not just owned)
- Gray out items player doesn't own
- Refresh grid on tab change

### Step 5.4: Mod Tab Integration
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/tabs/ModTabDiscovery.java`

**Implementation:**
- Scan for creative tabs added by other mods
- Dynamically add mod tabs to registry
- Handle mods adding/removing tabs at runtime

---

## Phase 6: Search and Filtering

### Step 6.1: Search Bar Widget
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/SearchBarWidget.java`

**Implementation:**
- Text input field
- Real-time search as user types
- Clear button
- Focus handling (click to activate, ESC to deactivate)

### Step 6.2: Search Logic
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/search/ItemSearcher.java`

**Implementation:**
- Fuzzy text matching on display names
- Exact/partial match on item IDs (e.g., `minecraft:oak_boat`)
- Case-insensitive search
- Search result caching for performance

### Step 6.3: Filter Buttons
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/widget/FilterButtonWidget.java`
- `src/main/java/com/chayut/bottomlessinventory/search/ItemFilter.java`

**Implementation:**
- Filter categories: enchanted, food, tools, weapons, armor, blocks, etc.
- Toggle buttons with visual state
- Multiple filters combine with OR logic
- Filters work alongside search (AND with search, OR between filters)

### Step 6.4: Integrate Search/Filter with Grid
**Files:**
- Update grid and screen

**Implementation:**
- Apply search + filter to item display
- Debounce search input for performance
- Visual feedback when filtering reduces results

---

## Phase 7: Item Interaction Mechanics

### Step 7.1: Click Handling in Infinite Grid
**Files:**
- Update `InfiniteGridWidget.java`

**Implementation:**
- Left-click: Pick up stack (up to 64)
- Right-click: Pick up half stack
- Shift-click: Move one stack (64 max) to hotbar or equipment
- Middle-click: Pick up full stack regardless of cursor state (creative-like)
- Number keys: Quick-move to hotbar slot

### Step 7.2: Item Pickup Redirection
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/mixin/ItemEntityMixin.java`
- `src/main/java/com/chayut/bottomlessinventory/mixin/PlayerInventoryMixin.java`

**Implementation:**
- Intercept item pickup events
- Route items to infinite storage instead of vanilla inventory slots
- Items already in hotbar stay in hotbar
- Preserve pickup sound/animation (vanilla behavior)

### Step 7.3: Auto-Refill System
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/mechanics/AutoRefillHandler.java`
- `src/main/java/com/chayut/bottomlessinventory/mixin/HotbarMixin.java`

**Implementation:**
- Detect when stackable hotbar item depletes (count reaches 0)
- Immediately refill from infinite storage
- Match exact item type + NBT
- Does NOT apply to non-stackable items (tools, weapons, armor)
- No animation delay - instant refill

### Step 7.4: Drag-and-Drop Support
**Files:**
- Update screen and grid

**Implementation:**
- Drag items between grid and slots
- Drag to distribute across multiple slots
- Visual cursor with item being dragged

---

## Phase 8: Container Integration

### Step 8.1: Container Screen Mixin Strategy
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/mixin/client/ContainerScreenMixin.java`

**Implementation:**
- Identify screens that show player inventory alongside container
- Strategy to inject infinite inventory panel into these screens
- List of vanilla containers: chest, furnace, crafting table, enchanting table, anvil, brewing stand, shulker box, etc.

### Step 8.2: Dual-Panel Container Screen
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/ContainerWithInfiniteScreen.java`

**Implementation:**
- Generic wrapper/mixin that adds infinite inventory to right side
- Container GUI on left, infinite inventory on right
- Hotbar at bottom
- Transfer items between container and infinite storage

### Step 8.3: Crafting Table Integration
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/screen/CraftingTableScreen.java` (or mixin)

**Implementation:**
- 3x3 crafting grid visible on left
- Infinite inventory on right
- Recipe book integration
- Shift-click crafting output to infinite storage

### Step 8.4: Specialized Container Handling
**Files:**
- Individual mixins or screens for edge cases

**Implementation:**
- Furnace family (furnace, blast furnace, smoker)
- Brewing stand
- Enchanting table
- Anvil
- Villager trading
- Ensure each works correctly with infinite inventory

---

## Phase 9: Recipe Book & Crafting Mode

### Step 9.1: Crafting Mode State
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/crafting/CraftingModeManager.java`

**Implementation:**
- Boolean state: crafting mode on/off
- Toggled by recipe book button
- Only active when in craftable GUI (not in chest screens)
- Visual indicator when active

### Step 9.2: Recipe Display Integration
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/crafting/RecipeOverlay.java`

**Implementation:**
- When crafting mode active, clicking item shows its recipe
- Recipe ingredients shown as ghost items in crafting grid
- Gray out recipe if ingredients unavailable in infinite storage
- Match vanilla recipe book behavior

### Step 9.3: Auto-Fill Crafting Grid
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/crafting/CraftingAutoFill.java`

**Implementation:**
- When recipe selected, fill crafting grid from infinite storage
- Only fill if all ingredients available
- Show grayed recipe outline if ingredients unavailable
- Server validation for ingredient removal

---

## Phase 10: Death & Persistence

### Step 10.1: Death Handler
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/mixin/PlayerDeathMixin.java`
- `src/main/java/com/chayut/bottomlessinventory/mechanics/DeathHandler.java`

**Implementation:**
- Prevent infinite inventory items from dropping on death
- Items preserved in player data
- Hotbar items also preserved (configurable, default: preserve)
- Works with keepInventory gamerule (respect it if enabled)

### Step 10.2: Respawn Sync
**Files:**
- Update network sync handler

**Implementation:**
- Full inventory sync on respawn
- Ensure client cache is refreshed
- Hotbar state restored

### Step 10.3: World Migration
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/migration/VanillaInventoryMigration.java`

**Implementation:**
- On first load with mod, migrate existing inventory
- Items in 27 storage slots → infinite storage
- Hotbar items remain in hotbar
- One-time migration flag stored in player data

---

## Phase 11: Configuration System

### Step 11.1: Config Data Structure
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/config/BottomlessConfig.java`

**Implementation:**
- Config options:
  - Auto-refill enabled (boolean, default: true)
  - Preserve hotbar on death (boolean, default: true)
  - UI scale preference
  - Keybinds
- Use a config library (Cloth Config or simple JSON)

### Step 11.2: Config File Handler
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/config/ConfigHandler.java`

**Implementation:**
- Load config from file on mod init
- Save config on changes
- Default values for missing options
- Config versioning for migration

### Step 11.3: Mod Menu Integration
**Files:**
- `src/client/java/com/chayut/bottomlessinventory/client/config/ModMenuIntegration.java`

**Implementation:**
- Integrate with Mod Menu for in-game config screen
- All options editable in GUI
- Apply changes immediately where possible
- Register as Mod Menu entry point

---

## Phase 12: Public API

### Step 12.1: API Interface Definition
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/api/BottomlessInventoryAPI.java`
- `src/main/java/com/chayut/bottomlessinventory/api/InfiniteInventoryAccess.java`

**Implementation:**
- Public interface for other mods:
  - `getInventory(PlayerEntity player)`: Get player's infinite inventory
  - `addItem(PlayerEntity player, ItemStack stack, int count)`
  - `removeItem(PlayerEntity player, ItemStack stack, int count)`
  - `getItemCount(PlayerEntity player, ItemStack stack)`
  - `getAllItems(PlayerEntity player)`
- Clear documentation/javadoc

### Step 12.2: API Implementation
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/api/impl/BottomlessInventoryAPIImpl.java`

**Implementation:**
- Implement API interface
- Delegate to internal inventory management
- Server-side validation
- Fire events for other mods to hook into

### Step 12.3: API Events
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/api/event/InventoryEvents.java`

**Implementation:**
- Events: ItemAdded, ItemRemoved, InventoryCleared
- Allow other mods to listen and react
- Cancellable events where appropriate

---

## Phase 13: Statistics Integration

### Step 13.1: Custom Statistics
**Files:**
- `src/main/java/com/chayut/bottomlessinventory/stats/BottomlessStats.java`

**Implementation:**
- Register custom statistics:
  - Items stored in infinite inventory
  - Total items collected
  - Unique item types stored
- Integrate with vanilla statistics screen

### Step 13.2: Statistics Tracking
**Files:**
- Update inventory handlers

**Implementation:**
- Increment stats when items added
- Track unique items stored
- Persist with player data

---

## Phase 14: Polish & Edge Cases

### Step 14.1: GUI Textures and Assets
**Files:**
- `src/main/resources/assets/bottomless-inventory/textures/gui/`

**Implementation:**
- Create or adapt GUI textures
- Tab icons
- Search bar texture
- Filter button textures
- Scrollbar texture
- Ensure visual consistency with vanilla

### Step 14.2: Tooltips and UX
**Files:**
- Update screen and widgets

**Implementation:**
- Exact item count in tooltip (in addition to abbreviated display)
- Helpful tooltips on tabs and filters
- Visual feedback on hover
- Accessibility considerations

### Step 14.3: Sound Effects
**Files:**
- Update interaction handlers

**Implementation:**
- Preserve vanilla pickup sounds
- Item transfer sounds
- GUI open/close sounds

### Step 14.4: Edge Case Handling
**Files:**
- Various

**Implementation:**
- GUI closes when player is attacked
- Handle inventory open during combat
- Handle concurrent container access
- Handle server disconnection during transfer
- Error recovery for corrupted data

---

## Phase 15: Testing & Documentation

### Step 15.1: Integration Testing
**Implementation:**
- Test all container interactions
- Test multiplayer sync
- Test death and respawn
- Test migration from vanilla
- Test mod removal and re-addition

### Step 15.2: Performance Testing
**Implementation:**
- Test with large inventory (millions of items)
- Test with many unique NBT variants
- Optimize rendering for large grids
- Profile memory usage

### Step 15.3: Documentation
**Files:**
- `README.md`
- Wiki or documentation site

**Implementation:**
- Installation instructions
- Usage guide
- API documentation for mod developers
- Known issues and limitations

---

## Dependency Graph

```
Phase 1 (Data Structure)
    |
    v
Phase 2 (Networking) <----------------------+
    |                                        |
    v                                        |
Phase 3 (GUI Infrastructure)                 |
    |                                        |
    v                                        |
Phase 4 (Vanilla Elements) ------------------+
    |                                        |
    v                                        |
Phase 5 (Tab System)                         |
    |                                        |
    v                                        |
Phase 6 (Search/Filter)                      |
    |                                        |
    v                                        |
Phase 7 (Item Mechanics) --------------------+
    |                                        |
    v                                        |
Phase 8 (Container Integration)              |
    |                                        |
    v                                        |
Phase 9 (Recipe Book) -----------------------+
    |
    v
Phase 10 (Death/Persistence)
    |
    v
Phase 11 (Configuration)
    |
    v
Phase 12 (API)
    |
    v
Phase 13 (Statistics)
    |
    v
Phase 14 (Polish)
    |
    v
Phase 15 (Testing)
```

---

## Notes for Orchestrator

1. **Parallelization opportunities:**
   - Phase 5 (Tabs) and Phase 6 (Search) can be done in parallel after Phase 4
   - Phase 11 (Config), Phase 12 (API), and Phase 13 (Stats) can be parallelized
   - GUI texture work (14.1) can happen anytime after Phase 3

2. **Critical path:**
   - Phases 1-3 are sequential and foundational
   - Phase 7 (Item Mechanics) is critical for core functionality
   - Phase 10 (Death) must work before any release

3. **Testing checkpoints:**
   - After Phase 3: Basic GUI should render and open
   - After Phase 7: Core item management should work
   - After Phase 10: Death should preserve items
   - After Phase 14: Full functionality

4. **Each step should:**
   - Compile successfully
   - Not break existing functionality
   - Be independently testable where possible
   - Include relevant mixins/registrations in the same commit
