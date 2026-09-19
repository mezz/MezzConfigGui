# Mod navigation for packs

## Choose mod order and visibility

In your test instance, open MezzConfig GUI's config screen and select
**Mod Navigation → Visible Mods**.

- Drag entries or use the arrow buttons to change their order.
- Remove an entry to hide that mod from the mod tabs and **Browse Configs**.
- Use the editor's add control to restore a hidden mod, then place it where you
  want it in the list.

Save your changes when closing the screen. New mods with config screens appear
at the end of your customized list. Mods you have hidden stay hidden, even if
you remove and later reinstall them.

Hiding a mod only removes it from navigation; the mod still runs. If you hide
MezzConfig GUI itself, open its settings from your loader's mod menu to restore
it.

## Include mod navigation in your pack

1. Set the mod order and visibility in your test instance as described above,
   then save your changes.
2. Copy `config/mezz_config_gui/client/mod-navigation.ini` from that instance
   into the same location in your pack's distribution files.
3. Install the pack in a fresh instance and check that the mod order and
   visibility match your choices.

When updating the pack, preserve each player's existing
`config/mezz_config_gui/client/mod-navigation.ini` so their choices are kept.

## Set default GUI preferences

To give players initial settings for section sizes, layout, and other GUI
preferences:

1. Configure the GUI in your test instance and save your changes.
2. Copy `config/mezz_config_gui/client/mezz_config_gui.ini` from that instance
   to `config/mezz_config_gui/client/default/mezz_config_gui.ini` in the pack's
   distribution files, replacing the default file if one is already present.
3. Leave `config/mezz_config_gui/client/mezz_config_gui.ini` out of the pack's
   distribution files.
4. Install the pack in a fresh instance and check that the GUI starts with your
   chosen settings.

Players can change these defaults through the GUI. When updating the pack,
replace the file in `client/default/` and preserve each player's
`client/mezz_config_gui.ini`. Their saved settings take precedence over the
pack defaults.
