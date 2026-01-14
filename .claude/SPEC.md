# Bottomless Inventory

> Replaces survival inventory with infinite creative-style storage

A Fabric mod for Minecraft 1.21.10 that replaces the standard 27-slot player inventory with unlimited storage, presented through a creative-mode-style GUI.

## Core Concept

- **Complete inventory replacement**: The 27 storage slots above the hotbar are replaced with infinite storage
- **Hotbar preserved**: The 9-slot hotbar remains unchanged with vanilla behavior
- **Armor & offhand preserved**: Equipment slots remain visible and manually managed
- **Available immediately**: No progression gate or unlock requirement

## GUI Design

### Standalone Inventory (E key)

```
+----------------------------------+---------------------------+
|  [Armor]    [2x2 Crafting]       |  [Tab] [Tab] [Tab] ...    |
|  [Slots]    [Grid]  [Output]     |  [>>>] (page navigation)  |
|                                  |                           |
|  [Offhand]                       |  [Search Bar] [Filters]   |
|                                  |                           |
|             [Recipe Book]        |  +---------------------+  |
|                                  |  | Infinite Inventory  |  |
|                                  |  | Grid (9 columns)    |  |
|                                  |  | Scrollable          |  |
|                                  |  +---------------------+  |
+----------------------------------+---------------------------+
|                    [Hotbar - 9 slots]                        |
+--------------------------------------------------------------+
```

- **Left panel**: Armor slots, offhand slot, 2x2 crafting grid with output, recipe book button
- **Right panel**: Creative-style tabs (top/bottom), search bar with filter buttons, 9-column scrollable item grid
- Replaces default `E` key inventory entirely

### Container Interactions

When opening containers (chests, furnaces, shulker boxes, etc.):

```
+---------------------------+---------------------------+
|  Container GUI            |  [Tab] [Tab] [Tab] ...    |
|  (chest slots, etc.)      |                           |
|                           |  [Search Bar] [Filters]   |
|                           |                           |
|                           |  +---------------------+  |
|                           |  | Infinite Inventory  |  |
|                           |  | Grid                |  |
|                           |  +---------------------+  |
+---------------------------+---------------------------+
|                    [Hotbar - 9 slots]                        |
+--------------------------------------------------------------+
```

- Container GUI on left, infinite inventory on right
- Full infinite inventory access while interacting with containers

### Crafting Table Interaction

- Recipe book button toggles **crafting mode**
- In crafting mode, clicking an item shows its recipe and places ingredients if available
- Behaves like vanilla recipe book: grayed recipe outline if ingredients unavailable
- Crafting mode only active when toggled via recipe book button in craftable GUI

## Tabs & Categories

### Tab System
- Mirrors creative mode tabs (Building Blocks, Colored Blocks, Natural, Functional, Redstone, Tools, Combat, Food, Ingredients, Spawn Eggs)
- Includes mod-added tabs from other Fabric mods
- Page navigation arrows for tabs when many mods are installed
- **Uncategorized tab**: Always exists, catches items without a creative tab assignment
- Tabs positioned at top and bottom like creative mode

### Item Display
- Shows **all items** in each category, not just owned items
- Items player doesn't have are grayed out
- 9 columns wide (matches creative/vanilla)
- Scrollable grid

## Search & Filtering

### Search Bar
- Full fuzzy text search
- Searches item display names
- Searches item IDs (e.g., `minecraft:oak_boat`)

### Filter Buttons
- Filter categories: enchanted, food, tools, weapons, armor, etc.
- Multiple filters combine with OR logic
- Filters work alongside search

## Item Management

### Item Quantities
- Items stack beyond 64 (no slot limit)
- Maximum per entry: `Integer.MAX_VALUE` (~2.1 billion)
- Display: Abbreviated numbers (10K, 1M) with exact count in tooltip

### NBT Handling
- Each unique NBT variation stored as separate entry
- Enchanted diamond sword (Sharpness III) ≠ Enchanted diamond sword (Sharpness V)
- Named items stored separately from unnamed

### Transfers
- **Shift-click**: Moves one stack (64 max) like vanilla
- **Instant transfer**: No animation when moving items
- **Pickup behavior**: All collected items go directly to infinite storage (hotbar items stay in hotbar)

### Auto-Refill
- When a **stackable item** in hotbar depletes, automatically refill from infinite storage
- Immediate refill, no delay
- Does **not** apply to non-stackable items (tools, weapons, armor)

## Death & Persistence

### On Death
- **Items preserved**: Infinite inventory contents never drop on death
- Hotbar items follow configured behavior (likely also preserved to match)

### Data Storage
- Stored in player NBT data
- Persists with world save
- Same player (UUID) on same world = same inventory, regardless of where world files are located
- Designed for portability when world files are moved

### Mod Removal
- Items persist in player data if mod is removed
- Items restored if mod is re-added
- No data loss

## Compatibility

### Migration
- Existing worlds: Items in hotbar remain in hotbar
- Items in 27 storage slots automatically migrate to infinite storage on first load

### API
- Provides read/write API for other mods
- Other mods can query items, add items, remove items
- Standard Fabric mod interoperability

### Other Mods
- Inventory-manipulating mods may need updates to use API
- Mod does not expose fake vanilla inventory

## Configuration

- **Extensive configuration** via Mod Menu integration
- Configurable behaviors include:
  - Keybinds
  - UI scale preferences
  - Auto-refill toggle
  - Most behaviors customizable

## Technical Requirements

- **Fabric Loader** for Minecraft 1.21.10
- **Required on both client and server**
- Server validates storage, client renders GUI

## Vanilla Behavior Preservation

| Behavior | Status |
|----------|--------|
| Hotbar scrolling/number keys | Unchanged |
| Armor/offhand slots | Unchanged |
| Pickup sound/animation | Unchanged |
| GUI closes when attacked | Unchanged |
| Shift-click transfer amount | Unchanged (1 stack) |
| Recipe book interaction | Preserved via crafting mode |

## Statistics

- Integrates with vanilla statistics system
- Track items collected, stored, etc.

## Out of Scope

- Item deletion feature (infinite storage makes deletion unnecessary)
- Quick deposit keybinds
- Visual hotbar indicators for storage quantities
- Server-side limits or admin controls
- Progression gates or unlock requirements
