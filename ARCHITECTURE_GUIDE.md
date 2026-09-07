# Quick Start: 3-Module Architecture

This document provides a quick reference for navigating and developing in the refactored 3-module Android architecture.

## Module Overview

### `:app` - Application Shell
**Location**: `app/`
**Purpose**: App lifecycle, main coordination, Koin setup

**Key Files**:
- `MainActivity.kt` - Single-line entry point (calls AppScreen)
- `PhotoApp.kt` - Koin module initialization
- `ui/AppScreen.kt` - NavHost with route coordination
- `di/AppDependenciesProvider.kt` - App-level DI (AnalyticsHelper, CoroutineScope)
- `viewmodel/AppScreenViewModel.kt` - App-level state (minimal)
- `utils/AnalyticsHelper.kt` - Analytics tracking

**Package**: `fr.leboncoin.androidrecruitmenttestapp`

### `:feature:albums` - Feature Module
**Location**: `feature/albums/`
**Purpose**: Complete album feature (UI, state, navigation, DI)

**Key Files**:
- `navigation/AlbumsRoutes.kt` - Type-safe routes (@Serializable)
- `ui/AlbumsScreen.kt` - Album list view
- `ui/AlbumDetailScreen.kt` - Album detail view
- `ui/AlbumItem.kt` - Reusable list item
- `viewmodel/AlbumsViewModel.kt` - Album state (UI state, loading)
- `di/AlbumsModule.kt` - Feature-level Koin configuration

**Package**: `fr.leboncoin.feature.albums`

### `:data` - Data Layer
**Location**: `data/`
**Purpose**: Network, repository, models, data DI

**Key Files**:
- `network/api/AlbumApiService.kt` - Retrofit API definitions
- `repository/AlbumRepository.kt` - Data access layer
- `model/AlbumDto.kt` - Domain model
- `di/DataModule.kt` - Data layer DI (Retrofit, OkHttp, serialization)

**Package**: `fr.leboncoin.data`

---

## Dependency Flow

```
:app
  ├─ imports routes & screens from :feature:albums
  ├─ depends on :feature:albums
  └─ depends on :data

:feature:albums
  ├─ imports domain model from :data
  ├─ injects repository from :data
  └─ depends on :data

:data
  ├─ no dependencies on other modules
  └─ pure data layer
```

**IMPORTANT**: `:feature:albums` does NOT depend on `:app`. This is key to modularity.

---

## Adding a New Feature

To add a new feature (e.g., `:feature:search`):

### 1. Create Module Structure
```bash
mkdir -p feature/search/src/{main,test}/java/fr/leboncoin/feature/search/{navigation,ui,viewmodel,di}
```

### 2. Create `feature/search/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.leboncoin.feature.search"
    compileSdk = 37
    // ... rest of config (copy from :feature:albums)
}

dependencies {
    implementation(project(":data"))
    // ... rest of dependencies (copy from :feature:albums)
}
```

### 3. Update `settings.gradle.kts`
```kotlin
include(":app")
include(":data")
include(":feature:albums")
include(":feature:search")  // ADD THIS
```

### 4. Update `app/build.gradle.kts`
```kotlin
dependencies {
    implementation(project(":data"))
    implementation(project(":feature:albums"))
    implementation(project(":feature:search"))  // ADD THIS
}
```

### 5. Create Feature Routes
`feature/search/src/main/java/fr/leboncoin/feature/search/navigation/SearchRoutes.kt`:
```kotlin
package fr.leboncoin.feature.search.navigation

import kotlinx.serialization.Serializable

@Serializable
object SearchRoute

@Serializable
data class SearchDetailRoute(val query: String)
```

### 6. Create Feature DI Module
`feature/search/src/main/java/fr/leboncoin/feature/search/di/SearchModule.kt`:
```kotlin
package fr.leboncoin.feature.search.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import fr.leboncoin.feature.search.viewmodel.SearchViewModel

val SearchModule = module {
    viewModel { SearchViewModel(get()) }  // get() resolves from :data
}
```

### 7. Update `PhotoApp.kt`
```kotlin
import fr.leboncoin.feature.search.di.SearchModule

class PhotoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PhotoApp)
            modules(DataModule, AppDependenciesProvider, AlbumsModule, SearchModule)  // ADD SearchModule
        }
    }
}
```

### 8. Update `AppScreen.kt`
```kotlin
import fr.leboncoin.feature.search.navigation.SearchRoute
import fr.leboncoin.feature.search.ui.SearchScreen

@Composable
fun AppScreen(analyticsHelper: AnalyticsHelper, viewModel: AppScreenViewModel = koinViewModel()) {
    val navController = rememberNavController()
    
    SparkTheme {
        NavHost(navController, startDestination = AlbumsRoute) {
            composable<AlbumsRoute> { /* ... */ }
            composable<AlbumDetailRoute> { /* ... */ }
            
            // ADD NEW ROUTES
            composable<SearchRoute> {
                SearchScreen(
                    viewModel = koinViewModel(),
                    onItemSelected = { navController.navigate(SearchDetailRoute(it)) }
                )
            }
            composable<SearchDetailRoute> { backStackEntry ->
                val route: SearchDetailRoute = backStackEntry.toRoute()
                SearchDetailScreen(
                    query = route.query,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

### 9. Test New Module
```bash
./gradlew :feature:search:test
./gradlew clean build
```

Done! The new feature is now integrated and ready for development.

---

## Common Tasks

### Run All Tests
```bash
./gradlew test
```

### Run Feature Tests Only
```bash
./gradlew :feature:albums:test
```

### Build App APK
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

### Check Dependencies
```bash
./gradlew :app:dependencies
```

### Clean and Rebuild
```bash
./gradlew clean build
```

### Lint Checks
```bash
./gradlew lint
```

### Format Code
```bash
./gradlew spotlessApply
```

---

## DI (Dependency Injection) Reference

### How Koin Resolves Dependencies

1. **DataModule** (loaded first):
   - Provides: OkHttpClient, Retrofit, AlbumApiService, AlbumRepository, Json serializer

2. **AppDependenciesProvider** (loaded second):
   - Provides: AnalyticsHelper, CoroutineScope, AppScreenViewModel
   - Consumes: Nothing from other modules

3. **AlbumsModule** (loaded third):
   - Provides: AlbumsViewModel
   - Consumes: AlbumRepository (from DataModule via `get()`)

4. **SearchModule** (if added):
   - Provides: SearchViewModel
   - Consumes: AlbumRepository (from DataModule via `get()`)

### Getting a ViewModel in Composable
```kotlin
// In any composable in :app or :feature:albums
val viewModel: AlbumsViewModel = koinViewModel()
```

### Getting a Service in ViewModel
```kotlin
// AlbumsViewModel constructor
class AlbumsViewModel(
    private val repository: AlbumRepository  // Koin injects from DataModule
) : ViewModel()
```

### Adding New Service to DI
```kotlin
// In DataModule (if data-related)
single { YourService(get()) }  // get() resolves dependencies

// In AppDependenciesProvider (if app-level)
single { YourAppService() }

// In feature module's DI (if feature-specific)
val YourFeatureModule = module {
    single { YourFeatureService(get()) }
}
// Then add to PhotoApp.kt: modules(..., YourFeatureModule)
```

---

## Navigation Reference

### Type-Safe Routes
```kotlin
// Define in feature module
@Serializable
object MyRoute

@Serializable
data class MyDetailRoute(val id: Int)

// Use in composable
composable<MyRoute> { /* ... */ }
composable<MyDetailRoute> { backStackEntry ->
    val route: MyDetailRoute = backStackEntry.toRoute()
    val id = route.id
}

// Navigate from anywhere
navController.navigate(MyRoute)
navController.navigate(MyDetailRoute(id = 123))
```

### Navigation from Composable
```kotlin
@Composable
fun MyScreen(
    onNavigate: (route: Any) -> Unit = {}
) {
    Button(onClick = { onNavigate(MyDetailRoute(id = 1)) }) {
        Text("Go to Detail")
    }
}
```

---

## Testing

### Unit Test in Feature Module
Create: `feature/albums/src/test/java/fr/leboncoin/feature/albums/AlbumsViewModelTest.kt`

```kotlin
package fr.leboncoin.feature.albums

import fr.leboncoin.feature.albums.viewmodel.AlbumsViewModel
import fr.leboncoin.data.repository.AlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AlbumsViewModelTest {
    @Test
    fun loadsAlbums_emitsNonEmptyList() = runBlocking {
        val repository = AlbumRepository(CoroutineScope(SupervisorJob()), fakeService)
        val vm = AlbumsViewModel(repository)
        
        vm.loadAlbums()
        
        val albums = vm.albums.first()
        assert(albums.isNotEmpty())
    }
}
```

### Integration Test in :app Module
Create: `app/src/test/java/fr/leboncoin/androidrecruitmenttestapp/NavigationTest.kt`

```kotlin
package fr.leboncoin.androidrecruitmenttestapp

import org.junit.Test

class NavigationTest {
    @Test
    fun navigateFromAlbumsToDetail() {
        // Use Compose testing library
        // Test navigation between routes
    }
}
```

---

## Troubleshooting

### Module Not Found
```
Error: Project ':feature:albums' not found in rootProject
```
**Solution**: Check `settings.gradle.kts` has `include(":feature:albums")`

### Circular Dependency
```
Circular dependency between :app and :feature:albums
```
**Solution**: 
- ✅ :app can depend on :feature:albums
- ✅ :feature:albums can depend on :data
- ❌ :feature:albums should NOT depend on :app

### Koin Resolution Failed
```
Can't find definition for class AlbumsViewModel
```
**Solution**: 
1. Check AlbumsModule is registered in PhotoApp.kt
2. Check `viewModel { AlbumsViewModel(get()) }` is in AlbumsModule
3. Ensure AlbumRepository is available from DataModule

### Lint Error: Composable Button
```
Error: Composable Button has a Spark replacement that should be used
```
**Solution**: Use Spark Composables instead of Material:
```kotlin
// ❌ WRONG
import androidx.compose.material3.Button
Button(...) { }

// ✅ CORRECT
import com.adevinta.spark.components.buttons.ButtonFilled
ButtonFilled(...) { }
```

---

## Best Practices

1. **Keep Features Independent**: :feature:albums should not import from :app
2. **Use Type-Safe Routes**: Always use @Serializable routes, never strings
3. **Inject Repository in ViewModel**: Use Koin for dependency injection
4. **Test in Feature Module**: Unit tests should be in feature module, not :app
5. **Document Public API**: Clearly document what your feature exports
6. **Use Sealed Classes for State**: Consider sealed classes for ViewModel state
7. **Handle Errors Gracefully**: Add error states to ViewModel
8. **Test Navigation**: Add integration tests for navigation flows

---

## Resources

- [Android Architecture - Multi-module](https://developer.android.com/guide/navigation)
- [Koin Documentation](https://insert-koin.io/)
- [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [Type-Safe Navigation](https://developer.android.com/guide/navigation/navigate-with-object)

---

**Last Updated**: September 7, 2026
**Architecture Version**: 3-Module (Feature-Based)
**Status**: Production Ready
