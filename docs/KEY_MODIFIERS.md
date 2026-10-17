# Key modifiers

The binding editor supports NeoForge modifiers and optional AMECS modifiers on
Fabric. Other Fabric mappings need an adapter to support modifiers.

## Custom mappings

Register an adapter during plugin registration (MezzConfig GUI 0.6.0+):

```java
IKeyMappingHelper helper = registration.getKeyMappingHelper();
registration.registerKeyMappingAdapter(MyKeyMapping.class,
    mapping -> new MyKeyMappingAdapter(mapping, helper));
```

Implement `IKeyMappingAdapter` from `net.mezzdev.config.gui.api.keybindings`.
Use the helper to create binding snapshots from your mapping's state. The
interface Javadocs describe input capture, supported modifiers, and conflict rules.

## Bindings and input

- `IKeyMappingBinding` describes a configured shortcut. Its modifiers can include
  `CONTROL_OR_COMMAND`, meaning Command on macOS and Control elsewhere.
- `IKeyMappingInput` describes physical input, with separate Control and Command
  modifiers. `binding.resolve()` converts a binding to this form;
  `adapter.createBinding(input)` chooses a binding the mapping supports.

Keep current and default values as bindings so portable aliases retain their
meaning. Minecraft and the loader or mod integration handle saving bindings.
