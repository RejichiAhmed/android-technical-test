# Press Groups Architecture

## Overview
The Presse POC app is organized into three business groups, each with its own navigation flow, UI screen, and dedicated ViewModel.

## Group Structure

```
presspoc/
├── PressPocModels.kt (PressModuleGroup enum + PressModule with group reference)
├── HomeScreen.kt / HomeViewModel.kt (launches the three groups)
├── PressApp.kt (navigation router)
└── groups/
    ├── OubliesPresseScreen.kt / OubliesPresseViewModel.kt
    ├── ReceptionBordereauReouvertureScreen.kt / ReceptionBordereauReouvertureViewModel.kt
    ├── InvendusBordereauFermetureScreen.kt / InvendusBordereauFermetureViewModel.kt
    └── PressModuleGroupScreen.kt (shared reusable group UI)
```

## Key Files

### Data Models (PressPocModels.kt)
- **PressModuleGroup** enum: 3 business areas
  - `OUBLIES` — Oublies Presse (lost/forgotten items control)
  - `RECEPTION_REOUVERTURE` — Reception & bordereau reouverture (incoming + reopening)
  - `INVENDUS_FERMETURE` — Invendus & bordereau fermeture (unsolds + closure)
  
- **PressModule** enum: individual workflows, now mapped to a group
  - Each module knows which group it belongs to

- Extension functions on PressModuleGroup:
  - `modules()` — list of modules in this group
  - `representativeModule()` — first module for styling/icons
  - `draftCount()` — total unsaved rows across all modules in group
  - `isSynced()` — whether all modules in group have been transmitted

### Navigation (PressApp.kt + PressScreen)
- **PressScreen.Login** → login form
- **PressScreen.Home** → home screen showing 3 group cards
- **PressScreen.Group** → group detail (shows modules in that group)
- **PressScreen.ModuleScan** → individual module scanner

### Home Layer (HomeScreen.kt / HomeViewModel.kt)
- Displays 3 grouped cards instead of 5 raw modules
- Aggregates draft counts and sync status per group
- Navigates to `PressScreen.Group` when a group card is tapped

### Group Screens (groups/ folder)
Each group has:

1. **Screen composable** (e.g., `OubliesPresseScreen`)
   - Instantiates the group's ViewModel
   - Passes modules from ViewModel state to shared UI
   - Handles navigation callbacks to module scanner

2. **ViewModel** (e.g., `OubliesPresseViewModel : ViewModel()`)
   - Owns `ModuleGroupUiState` with `modules: List<PressModule>`
   - Computes modules for its specific group at creation time
   - Ready for extension: add filtering, sorting, or async loading

3. **Shared UI** (`PressModuleGroupScreen`)
   - Reusable card/list layout for any group
   - Takes group, modules, and navigation callbacks
   - Displays group title + subtitle, count of modules, module list

## Design Patterns

### Split by Business Concern
- Each group has a clear business purpose (e.g., Reception & Reouverture workflow)
- Modules are grouped, not scattered across a flat menu
- UX reflects business logic

### ViewModel per Group
- `OubliesPresseViewModel`, `ReceptionBordereauReouvertureViewModel`, `InvendusBordereauFermetureViewModel`
- Each ViewModel:
  - Manages its own group state
  - Can be extended for group-specific logic (e.g., filtering, sorting, compliance rules)
  - Scoped to the group screen

### Reusable UI
- `PressModuleGroupScreen` is a pure composable accepting modules
- No logic in the shared UI, just rendering
- Each group screen wrapper composes ViewModel → Shared UI

### Home Aggregation
- `HomeViewModel` combines draft counts and sync status across all groups
- Displays as 3 summary cards
- Respects offline-first: aggregates from repository flows in real-time

## Future Extensions

### Per-Group Logic
If a group needs special behavior (e.g., "Reception requires barcode scanning before proceed"):
1. Add state/methods to the specific ViewModel (e.g., `ReceptionBordereauReouvertureViewModel`)
2. Keep shared UI generic; pass callbacks if needed
3. Navigation remains type-safe through `PressScreen.Group`

### Persistence
If group-level preferences or state are needed:
1. Store in the ViewModel backed by a repository
2. Each ViewModel can independently manage its own preferences

### Testing
Each group ViewModel can be tested in isolation:
```kotlin
@Test
fun oubliesGroupLoadsModulesOnInit() {
    val vm = OubliesPresseViewModel()
    assertEquals(1, vm.uiState.modules.size)
    assertEquals(PressModule.OUBLIES, vm.uiState.modules.first())
}
```

