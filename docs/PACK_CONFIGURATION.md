# Mod navigation for packs

Open MezzConfig GUI's own config screen and select **Mod Navigation → Visible
Mods**. This is a sorting editor populated with all discovered mods that have
config screens, using their display names and icons.

- Drag entries or use the arrow buttons to change their order.
- Remove an entry to hide that mod from the mod tabs and **Browse Configs**.
- Use the editor's add control to restore a hidden mod, then place it where you
  want it in the list.

Save your changes when closing the screen. Include
`config/mezz_config_gui/client/mod-navigation.ini` in your pack's client
configuration to distribute the order and visibility preferences. The file
stores stable mod IDs, so display-name or language changes do not change which
mods are hidden.

By default, mods appear alphabetically by display name. Once you customize the
list, newly discovered mods with config screens are appended automatically in
alphabetical order. Previously hidden mods stay hidden, including after a
restart or when an optional mod is removed and later reinstalled. For example,
hiding mod B from A, B, C and then installing D leaves A, C, D visible.

Hiding a mod changes navigation only. It does not disable the mod or its
configuration, and a loader or another mod may still open its screen directly.
If you hide MezzConfig GUI itself, use its loader mod-menu entry to reopen its
settings and restore it.

Other GUI preferences are stored separately in
`config/mezz_config_gui/client/mezz_config_gui.ini`. Older `modOrder` and
`hiddenMods` lists in that file are migrated once to the sorting configuration;
use **Visible Mods** for subsequent changes.
