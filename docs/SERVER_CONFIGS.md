# Server configuration support

MezzConfig GUI's remote editor supports server schemas registered through
MezzConfig when its server-side integration is installed. The server checks the
player's operator permission before accepting changes and validates the values.

Auto-detected NeoForge `SERVER` configs are a separate integration. NeoForge
supplies the server's values to clients, but MezzConfig GUI does not currently
send edits to those native configs back to a multiplayer server. They are
read-only in this screen during multiplayer, including for operators. An
administrator must edit the server's config file and follow that mod's reload
or restart requirements. Native singleplayer server configs remain locally
editable.

NeoForge `COMMON` configs are local files on each installation. Editing a client
copy does not update a multiplayer server. This is distinct from `SERVER`
configuration, even when the Common file contains server-related options.
