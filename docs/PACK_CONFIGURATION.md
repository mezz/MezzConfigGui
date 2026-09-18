# Mod navigation for packs

MezzConfig GUI stores its client preferences in `config/mezz_config_gui.ini`.
Open its config screen and select **Mod Navigation** to edit the lists, then
include that file in your pack's client configuration.

| Setting | Contents | Effect |
| --- | --- | --- |
| `modOrder` | An ordered list of mod IDs, for example `jei`, `create`, `minecraft` | These mods appear first in that order. Other mods follow alphabetically by display name. |
| `hiddenMods` | A list of mod IDs | These mods are omitted from the mod tabs and Browse Configs list. Hiding takes priority over ordering. |

Use mod IDs rather than display names. Missing mods and duplicate entries are
ignored, so one configuration can cover optional mods. Both lists are empty by
default. Save and reopen a config screen after changing them; returning to
Browse Configs also refreshes that list.

Hiding a mod changes navigation only. Its configuration still loads, and a
loader or another mod may provide a direct way to open its screen. To recover a
hidden entry, remove its ID from `hiddenMods`.
