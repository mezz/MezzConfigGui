# Quick menus

Keep `registration.getQuickConfigMenuFactory()` in your plugin, then build on
the client thread after registration:

```java
var menu = quickMenuFactory.createBuilder(title)
    .addDefault(config.showLabels())
    .addDefault(overlayToggle)
    .addAdditional("display.alignment")
    .build();
```

Call `menu.open(button)` to open it. Both add methods accept `IConfigValue<?>`,
`IConfigScreenValue<?>`, or a registered path and preserve order. Additional
settings can be enabled in Customize. Registered editors and save behavior are reused.
Values shown more than once use their first entry in screen order.

Reuse screen-value instances or share their `getIdentityKey()` object. Keep
registered category and value names stable for saved choices. Server settings
are unsupported. The path-list API remains compatible, and identical ordered
definitions retain saved choices.
