Now I have comprehensive context of your Android project. Let me draft a detailed execution plan for the 3-module refactoring.

---

## Plan: Refactor Android App to 3-Module Architecture (`:feature:albums`)

Migrate album-related UI, screens, navigation, and ViewModel from `:app` to a new `:feature:albums` module, leaving `:app` as a thin navigation shell. This improves modularity, encapsulation, and maintainability while preserving type-safe navigation and Koin DI.

### Phase 1: Preparation & Analysis

#### Step 1.1: Audit Code Dependencies
- **Actions**:
  - Search for all imports of classes that will move (AlbumsViewModel, AlbumsScreen, AlbumDetailScreen, AlbumItem)
  - Identify cross-module dependencies (`:app` â†” `:data`, `:app` â†” `:feature:albums`)
  - Document all Koin module registrations for album-related classes in [AppDependenciesProvider.kt](../app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/di/AppDependenciesProvider.kt)
  - Check test files that depend on moving classes ([AlbumsViewModelTest.kt](app/src/test/java/fr/leboncoin/androidrecruitmenttestapp/AlbumsViewModelTest.kt))

- **Validation Criteria**:
  - Complete list of classes to move documented
  - All import chains traced (A imports B imports C)
  - Package structure mapped

- **Complexity**: Low
- **Risk**: None (read-only analysis)

#### Step 1.2: Verify Navigation Routes
- **Actions**:
  - Review [AppRoutes.kt](app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/navigation/AppRoutes.kt) to confirm `@Serializable` routes
  - Check how routes are used in [MainActivity.kt](../app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/MainActivity.kt) (NavHost, composable<T>)
  - Confirm type-safe navigation doesn't rely on string hardcoding

- **Validation Criteria**:
  - Routes are @Serializable objects/data classes
  - Navigation is type-safe (no string-based routes)

- **Complexity**: Low
- **Risk**: None

---

### Phase 2: Gradle & Module Setup

#### Step 2.1: Create `:feature:albums` Module Directory Structure
- **Dependencies**: Phase 1 complete
- **Actions**:
  - Create directory: `feature/albums/`
  - Create subdirectories:
    - `feature/albums/src/main/java/fr/leboncoin/feature/albums/`
    - `feature/albums/src/main/java/fr/leboncoin/feature/albums/ui/`
    - `feature/albums/src/main/java/fr/leboncoin/feature/albums/navigation/`
    - `feature/albums/src/main/java/fr/leboncoin/feature/albums/viewmodel/`
    - `feature/albums/src/main/java/fr/leboncoin/feature/albums/di/`
    - `feature/albums/src/test/java/fr/leboncoin/feature/albums/`
    - `feature/albums/src/main/res/` (for future resources)

- **Validation Criteria**:
  - All directories created and empty
  - No files yet

- **Complexity**: Low
- **Risk**: None

#### Step 2.2: Create `:feature:albums` Build Configuration
- **Dependencies**: Step 2.1 complete
- **Actions**:
  - Create [feature/albums/build.gradle.kts](../feature/albums/build.gradle.kts) with:
    - `plugins { alias(libs.plugins.android.library) }` (library, not app)
    - `namespace = "fr.leboncoin.feature.albums"`
    - `compileSdk = 37`, `minSdk = 24`, `targetSdk = 36`
    - `buildFeatures { compose = true }`
    - Dependencies:
      - `implementation(project(":data"))` (needs repository)
      - Compose libraries (ui, material3, lifecycle-viewmodel-compose, navigation-compose, serialization)
      - Koin libraries (koin-core, koin-androidx-compose)
      - Coil libraries (coil-compose, coil-network-okhttp)
      - Spark theme libraries
      - Test dependencies (junit, mockito if needed)

- **Validation Criteria**:
  - Build file is syntactically correct
  - References valid library aliases from [libs.versions.toml](../gradle/libs.versions.toml)
  - Correctly set as Android library

- **Complexity**: Medium
- **Risk**: Dependency misconfiguration (test by attempting gradle sync)

#### Step 2.3: Update Root Settings
- **Dependencies**: Step 2.2 complete
- **Actions**:
  - Update [settings.gradle.kts](../settings.gradle.kts):
    - Change `include(":app")` and `include(":data")` to add `include(":feature:albums")`
    - Should be: `include(":app")`, `include(":data")`, `include(":feature:albums")`

- **Validation Criteria**:
  - Gradle recognizes new module in sync
  - No "project not found" errors

- **Complexity**: Low
- **Risk**: Syntax error in settings

#### Step 2.4: Update `:app` Build Dependencies
- **Dependencies**: Step 2.3 complete
- **Actions**:
  - Update [app/build.gradle.kts](../app/build.gradle.kts):
    - Add: `implementation(project(":feature:albums"))`
    - Keep: `implementation(project(":data"))` (for types needed by AppScreen)
    - Remove unnecessary Compose libraries that will now be transitive (optional cleanup)

- **Validation Criteria**:
  - App build file compiles
  - Gradle sync succeeds

- **Complexity**: Low
- **Risk**: Circular dependency (validate in Step 2.5)

#### Step 2.5: Validate Module Dependencies
- **Dependencies**: Step 2.4 complete
- **Actions**:
  - Run: `./gradlew dependencies` and check dependency tree
  - Verify no circular dependencies: `:app` â†’ `:feature:albums` â†’ `:data` (no reverse)
  - Confirm `:feature:albums` does NOT depend on `:app`

- **Validation Criteria**:
  - Gradle sync succeeds without errors
  - Dependency tree shows correct hierarchy
  - No "A depends on B which depends on A" patterns

- **Complexity**: Low
- **Risk**: Circular dependency creates compile failure

---

### Phase 3: Code Migration

#### Step 3.1: Move Navigation Routes
- **Dependencies**: Phase 2 complete
- **Actions**:
  - Copy [AppRoutes.kt](app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/navigation/AppRoutes.kt) to `feature/albums/src/main/java/fr/leboncoin/feature/albums/navigation/AlbumsRoutes.kt`
  - Update package declaration: `fr.leboncoin.feature.albums.navigation`
  - Keep both `AlbumsRoute` and `AlbumDetailRoute` (rename file but not classes)
  - Delete original `AppRoutes.kt` from `:app` module
  - Update imports in `:app` to import from `:feature:albums`

- **Validation Criteria**:
  - File copied with correct package
  - Serialization works (routes are still @Serializable)
  - `:app` can import new location without errors

- **Complexity**: Low
- **Risk**: Import path mismatch

#### Step 3.2: Move UI Components (Screens & Items)
- **Dependencies**: Step 3.1 complete
- **Actions**:
  - Copy `AlbumsScreen.kt`, `AlbumDetailScreen.kt`, `AlbumItem.kt` from `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/` to `feature/albums/src/main/java/fr/leboncoin/feature/albums/ui/`
  - Update package declarations to `fr.leboncoin.feature.albums.ui`
  - Update imports within each file to reference new locations
  - Remove original files from `:app` module

- **Validation Criteria**:
  - All three files copied and package-updated
  - Internal imports resolve (e.g., AlbumItem imports AlbumDto from `:data`)
  - Compose functions remain @Composable

- **Complexity**: Low
- **Risk**: Import cycles, missing dependencies

#### Step 3.3: Move ViewModel
- **Dependencies**: Step 3.2 complete
- **Actions**:
  - Copy [AlbumsViewModel.kt](app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/AlbumsViewModel.kt) to `feature/albums/src/main/java/fr/leboncoin/feature/albums/viewmodel/AlbumsViewModel.kt`
  - Update package: `fr.leboncoin.feature.albums.viewmodel`
  - Keep the Factory inner class
  - Update imports (AlbumRepository from `:data`, etc.)
  - Delete original from `:app` module

- **Validation Criteria**:
  - ViewModel compiles with dependencies resolved
  - Factory class is accessible for DI

- **Complexity**: Low
- **Risk**: CoroutineScope or GlobalScope issues if configuration changed

#### Step 3.4: Move Tests
- **Dependencies**: Step 3.3 complete
- **Actions**:
  - Copy [AlbumsViewModelTest.kt](app/src/test/java/fr/leboncoin/androidrecruitmenttestapp/AlbumsViewModelTest.kt) to `feature/albums/src/test/java/fr/leboncoin/feature/albums/AlbumsViewModelTest.kt`
  - Update package: `fr.leboncoin.feature.albums`
  - Update imports to reference new ViewModel and classes location
  - Delete original test from `:app` module

- **Validation Criteria**:
  - Test file compiles and runs (`./gradlew :feature:albums:test`)

- **Complexity**: Low
- **Risk**: Test runner misconfiguration

---

### Phase 4: AppScreen & Navigation Refactoring

#### Step 4.1: Create AppScreenViewModel
- **Dependencies**: Phase 3 complete
- **Actions**:
  - Create `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/viewmodel/AppScreenViewModel.kt`
  - Class should:
    - Extend ViewModel
    - Store navigation state (current route, back stack if needed for app-level logic)
    - Provide methods for app-level navigation (e.g., `navigateToAlbums()`, `navigateToDetails()`)
    - Initially minimal; focus on delegating to feature modules
  - Example structure:
    ```
    class AppScreenViewModel : ViewModel() {
        // Minimal for now; can grow if needed for app-level UI state
    }
    ```

- **Validation Criteria**:
  - File compiles
  - Class is testable

- **Complexity**: Medium
- **Risk**: Over-engineering; keep it minimal

#### Step 4.2: Create AppScreen Composable
- **Dependencies**: Step 4.1 complete
- **Actions**:
  - Create `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/AppScreen.kt`
  - Composable function structure:
    - Move NavHost logic from MainActivity to AppScreen
    - Import routes from `:feature:albums` (AlbumsRoute, AlbumDetailRoute)
    - Define composable routes pointing to `:feature:albums` screens
    - Pass navigation callbacks (onItemSelected, onBack) from screens to NavController
  - Example:
    ```kotlin
    @Composable
    fun AppScreen(viewModel: AppScreenViewModel = koinViewModel()) {
        val navController = rememberNavController()
        
        SparkTheme {
            NavHost(navController, startDestination = AlbumsRoute) {
                composable<AlbumsRoute> {
                    AlbumsScreen(
                        viewModel = koinViewModel(),
                        onItemSelected = { navController.navigate(AlbumDetailRoute(it.id)) }
                    )
                }
                composable<AlbumDetailRoute> { backStackEntry ->
                    val route: AlbumDetailRoute = backStackEntry.toRoute()
                    AlbumDetailScreen(
                        albumId = route.albumId,
                        onBack = { navController.popBackStack() },
                        albums = {} // passed via ViewModel if needed
                    )
                }
            }
        }
    }
    ```

- **Validation Criteria**:
  - AppScreen compiles
  - NavHost references correct routes from `:feature:albums`
  - Routes are @Serializable from correct module

- **Complexity**: Medium
- **Risk**: Navigation breaks if routes not imported correctly; album data sharing pattern needs verification

#### Step 4.3: Refactor MainActivity
- **Dependencies**: Step 4.2 complete
- **Actions**:
  - Update [MainActivity.kt](../app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/MainActivity.kt):
    - Remove NavHost, composable definitions, route imports
    - Remove AlbumsViewModel creation and albums state collection
    - Remove AlbumDetailRoute, AlbumsRoute imports
    - Keep AnalyticsHelper injection (still app-level)
    - Replace setContent body with single call to AppScreen()
  - Final MainActivity:
    ```kotlin
    class MainActivity : ComponentActivity() {
        private val analyticsHelper: AnalyticsHelper by inject()
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            analyticsHelper.initialize(this)
            setContent { AppScreen() }
        }
    }
    ```

- **Validation Criteria**:
  - MainActivity compiles
  - No unused imports
  - AnalyticsHelper still injected and used

- **Complexity**: Low
- **Risk**: Breaking navigation if AppScreen not imported correctly

#### Step 4.4: Handle Cross-Module Data Sharing
- **Dependencies**: Step 4.3 complete
- **Actions**:
  - Review AlbumDetailScreen signature in [AlbumDetailScreen.kt](app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/AlbumDetailScreen.kt)
  - Current: Takes `albums: List<AlbumDto>` from parent state
  - Options:
    - **A) Create a DetailViewModel**: Add DetailScreenViewModel in `:feature:albums` to fetch single album
    - **B) Pass via NavArgs**: Serialize album data in route (payload increases URL length)
    - **C) Keep parent state**: Pass albums from AlbumsScreen via composition or shared ViewModel
  - **Recommended**: **Option A** (details in next step)

- **Validation Criteria**:
  - Approach chosen and documented
  - No compile errors from data flow changes

- **Complexity**: Medium
- **Risk**: Incorrect pattern causes navigation data loss

#### Step 4.5: (Optional) Refactor AlbumDetailScreen Data Flow
- **Dependencies**: Step 4.4 decision made
- **Actions** (if Option A chosen):
  - Create `feature/albums/src/main/java/fr/leboncoin/feature/albums/viewmodel/AlbumDetailViewModel.kt`
  - Inject AlbumRepository, accept albumId in constructor
  - Fetch album details on init
  - Update AlbumDetailScreen to accept ViewModel instead of full albums list
  - Register ViewModel in feature DI

- **Validation Criteria**:
  - Detail screen displays without albums list dependency
  - Navigation still works

- **Complexity**: Medium
- **Risk**: Breaking detail screen display

---

### Phase 5: DI Wiring

#### Step 5.1: Create `:feature:albums` DI Module
- **Dependencies**: Phase 4 complete, `:feature:albums` module stable
- **Actions**:
  - Create `feature/albums/src/main/java/fr/leboncoin/feature/albums/di/AlbumsModule.kt`
  - Register:
    - `viewModel { AlbumsViewModel(get()) }` (get() resolves AlbumRepository from `:data`)
    - `viewModel { AlbumDetailViewModel(get()) }` (if created in Step 4.5)
  - Ensure module is a Koin `val` that can be imported in `:app`
  - Example:
    ```kotlin
    val AlbumsModule = module {
        viewModel { AlbumsViewModel(get()) }
    }
    ```

- **Validation Criteria**:
  - Module compiles
  - All dependencies resolvable (AlbumRepository from `:data`)
  - ViewModel instances can be created

- **Complexity**: Low
- **Risk**: Dependency resolution failure

#### Step 5.2: Update `:app` DI Module
- **Dependencies**: Step 5.1 complete
- **Actions**:
  - Update [AppDependenciesProvider.kt](../app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/di/AppDependenciesProvider.kt):
    - Remove `viewModel { AlbumsViewModel(get()) }` (now in `:feature:albums`)
    - Keep AnalyticsHelper, CoroutineScope
    - Add: `viewModel { AppScreenViewModel() }` (if needed)
  - Example:
    ```kotlin
    val AppDependenciesProvider = module {
        single { AnalyticsHelper() }
        single<CoroutineScope> { (androidApplication() as PhotoApp).applicationScope }
        viewModel { AppScreenViewModel() }
    }
    ```

- **Validation Criteria**:
  - Module compiles
  - App-level ViewModels still resolvable
  - Feature ViewModels removed from this module

- **Complexity**: Low
- **Risk**: Koin resolution failure if module structure incorrect

#### Step 5.3: Update PhotoApp Initialization
- **Dependencies**: Steps 5.1 & 5.2 complete
- **Actions**:
  - Update [PhotoApp.kt](../app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/PhotoApp.kt):
    - Add import: `import fr.leboncoin.feature.albums.di.AlbumsModule`
    - Modify `startKoin { modules(DataModule, AppDependenciesProvider, AlbumsModule) }`
  - Final:
    ```kotlin
    startKoin {
        androidLogger()
        androidContext(this@PhotoApp)
        modules(DataModule, AppDependenciesProvider, AlbumsModule)
    }
    ```

- **Validation Criteria**:
  - PhotoApp compiles
  - Koin can instantiate all modules (test with `./gradlew assemble`)

- **Complexity**: Low
- **Risk**: Module loading order (DataModule must come before feature modules)

#### Step 5.4: Verify DI Resolution
- **Dependencies**: Step 5.3 complete
- **Actions**:
  - Compile app: `./gradlew :app:assemble`
  - Check for Koin resolution errors in build output
  - Verify no "Cannot resolve..." for ViewModels or Repository
  - (Optional) Add test to check Koin context starts correctly

- **Validation Criteria**:
  - Build succeeds
  - No unresolved dependency warnings
  - Koin context initializes on app startup

- **Complexity**: Low
- **Risk**: Silent DI misconfiguration (may only fail at runtime)

---

### Phase 6: Validation & Testing

#### Step 6.1: Compile & Build Verification
- **Dependencies**: Phase 5 complete
- **Actions**:
  - Run: `./gradlew clean build`
  - Resolve any compilation errors (missing imports, package mismatches)
  - Check build succeeds for all modules (`:app`, `:data`, `:feature:albums`)

- **Validation Criteria**:
  - Build completes without errors
  - All three modules compile
  - No lint warnings related to dependencies

- **Complexity**: Low
- **Risk**: Lingering import or configuration issues

#### Step 6.2: Unit Test Execution
- **Dependencies**: Step 6.1 complete
- **Actions**:
  - Run `:feature:albums` tests: `./gradlew :feature:albums:test`
  - Run `:app` tests: `./gradlew :app:test`
  - Run `:data` tests: `./gradlew :data:test`
  - Fix any failing tests (mock imports, ViewModel creation, etc.)

- **Validation Criteria**:
  - All test suites pass
  - AlbumsViewModelTest passes in new location
  - No test runner errors

- **Complexity**: Medium
- **Risk**: Test setup issues (mocking, Coroutines, Koin injection in tests)

#### Step 6.3: App Startup & Navigation Test
- **Dependencies**: Step 6.2 complete
- **Actions**:
  - Build APK: `./gradlew :app:assembleDebug`
  - Run on emulator/device or use Android Studio's app preview
  - Verify:
    - App starts without crashes
    - Albums list displays (data flows correctly)
    - Clicking an album navigates to detail screen
    - Back button returns to albums list
    - Analytics tracking works

- **Validation Criteria**:
  - App launches without ANR/crash
  - Navigation routes work
  - Data binding preserved
  - Analytics helper initialized

- **Complexity**: Medium
- **Risk**: Runtime DI resolution failure, navigation graph issues, data binding broken

#### Step 6.4: Code Structure & Encapsulation Review
- **Dependencies**: Step 6.3 complete
- **Actions**:
  - Verify `:feature:albums` exports only public API (routes, screens if needed)
  - Check `:app` doesn't directly reference internal classes in `:feature:albums`
  - Confirm package naming follows convention: `fr.leboncoin.feature.albums.*`
  - Document public API (routes, composables for integration)

- **Validation Criteria**:
  - No internal cross-module imports (e.g., `:app` imports `.viewmodel` package)
  - Feature module is loosely coupled to `:app`
  - Public API clearly defined

- **Complexity**: Low
- **Risk**: Poor encapsulation defeats refactoring purpose

#### Step 6.5: Clean Up Unused Files
- **Dependencies**: All prior steps complete
- **Actions**:
  - Verify original `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/AppRoutes.kt` deleted
  - Verify original ViewModel, screens, tests deleted from `:app`
  - Check no duplicate classes remain
  - Ensure `:app` module no longer has `ui/` directory (if empty)
  - Delete empty directories

- **Validation Criteria**:
  - No duplicate classes in codebase
  - No orphaned imports
  - Project structure clean

- **Complexity**: Low
- **Risk**: Accidentally deleting necessary files (use version control to recover)

#### Step 6.6: Documentation & Handoff
- **Dependencies**: Step 6.5 complete
- **Actions**:
  - Update README.md or project docs with new module structure
  - Document package layout:
    - `:app` â†’ app-level shell, MainActivity, DI coordination
    - `:data` â†’ repository, API, models
    - `:feature:albums` â†’ all album UI, ViewModel, navigation routes
  - Add notes on adding new features (how to create new `:feature:*` modules)
  - Document DI contract (which modules are initialized in PhotoApp)

- **Validation Criteria**:
  - Documentation updated
  - Future developers can navigate architecture

- **Complexity**: Low
- **Risk**: None

---

## Further Considerations

1. **Data Sharing Between Features**: When `:feature:albums` detail screen needs album list data, **Option A (DetailViewModel)** is recommended to avoid tight coupling. This also scales better if you add more feature modules later.

2. **Cross-Feature Navigation**: If future features need to navigate to albums, consider exporting navigation routes from `:feature:albums` public API so other features can import them.

3. **AnalyticsHelper Placement**: Currently stays in `:app` as app-level utility. If multiple features need analytics, consider moving to `:data` or creating a `:core:analytics` module.

4. **Testing Strategy**: Ensure `:feature:albums` tests don't require `:app` context. Tests should be isolated; only integration tests in `:app` should test cross-module behavior.

---

This plan is **ready for execution tracking**. Each phase builds on the prior, with clear dependencies and validation criteria for sign-off. Would you like me to refine any phase or provide more detail on specific steps?
