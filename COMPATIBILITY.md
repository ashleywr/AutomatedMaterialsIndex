# AMI Compatibility & Integration

## Supported Recipe UIs

AMI is designed to work alongside existing recipe UI mods, enhancing them with intelligent indexing and categorization.

### JEI (Just Enough Items)
- **Status**: Planned integration (deferred)
- **Approach**: Plugin system via `IModPlugin`
- **Features**: Feeds AMI's color/mod/tier indices as searchable aliases
- **Coexistence**: Yes - AMI shell UI disabled when JEI is present

### EMI (Equipment/Equivalent Enough Items)
- **Status**: Planned integration (deferred)
- **Approach**: Plugin system via `EmiPlugin`
- **Features**: Registers AMI's indexed categories as recipe categories
- **Coexistence**: Yes - works alongside JEI or standalone

### AMI Shell UI (Fallback)
- **Status**: Working
- **Trigger**: When neither JEI nor EMI is installed
- **Features**: Basic item grid, recipe viewing, search
- **Launch**: Press **I** to open

## Deployment

### Development Environment (PrismLauncher)
JAR auto-copies to: `C:\Users\ashle\AppData\Roaming\PrismLauncher\instances\Mod Making\minecraft\mods`

Run: `gradlew build`

### Testing with Multiple Recipe UIs

To test all three modes:

1. **JEI Only**: Install AMI + JEI → uses JEI UI with AMI indexing
2. **EMI Only**: Install AMI + EMI → uses EMI UI with AMI indexing
3. **Both**: Install AMI + JEI + EMI → both receive AMI data (via plugins)
4. **Neither**: Install AMI alone → uses AMI shell UI

## Architecture

```
┌─────────────────────────────────────┐
│  Recipe UI Layer (JEI / EMI / None) │
├─────────────────────────────────────┤
│    Integration Bridges (plugins)    │
│  JeiPlugin.java | EmiPlugin.java    │
├─────────────────────────────────────┤
│         AMI Core (Indexer)          │
│  Categorizes by color/mod/tier      │
└─────────────────────────────────────┘
```

AMI core is always active. Integration plugins activate only when their respective mods are present.

## Future Enhancements

- [x] Core indexing system
- [x] Shell UI (fallback)
- [ ] JEI plugin integration (deferred)
- [ ] EMI plugin integration (deferred)
- [ ] Storage metric compat adapters:
  - Functional Storage drawer capacity
  - Applied Energistics 2 cell bytes/types conversion
  - Refined Storage disk capacity
  - Sophisticated Backpacks API/config-backed tier capacity
- [ ] Material Root UI (blockstate collapsing)
- [ ] Ghost Crafting (Architect's Gauntlet)
- [ ] Progression Graph (GameStages integration)
