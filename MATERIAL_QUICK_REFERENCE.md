# Material Design 3 Refactoring - Quick Reference

## Component Migration Guide

### Scaffold
```kotlin
// Before: Spark
import com.adevinta.spark.components.scaffold.Scaffold
Scaffold(topBar = { ... }) { ... }

// After: Material 3
import androidx.compose.material3.Scaffold
Scaffold(
    topBar = { TopAppBar(title = { Text("...") }) },
) { contentPadding ->
    Column(modifier = Modifier.padding(contentPadding)) { ... }
}
```

### Top App Bar
```kotlin
// Before: Custom Row or Spark component
Row { ChipTinted(...) }

// After: Material 3
import androidx.compose.material3.TopAppBar
TopAppBar(
    title = { Text("Albums") },
    navigationIcon = {
        IconButton(onClick = { ... }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
    }
)
```

### Chips (Category Filter)
```kotlin
// Before: Spark ChipTinted
import com.adevinta.spark.components.chips.ChipTinted
ChipTinted(
    text = "All",
    intent = if (selected) ChipIntent.Main else ChipIntent.Basic,
    onClick = { ... }
)

// After: Material 3 FilterChip
import androidx.compose.material3.FilterChip
FilterChip(
    label = { Text("All") },
    selected = selected,
    onClick = { ... }
)
```

### Buttons
```kotlin
// Before: Spark ButtonFilled
import com.adevinta.spark.components.buttons.ButtonFilled
ButtonFilled(onClick = { ... }) { Text("Back") }

// After: Material 3
import androidx.compose.material3.Button
Button(onClick = { ... }) { Text("Back") }
```

### Cards
```kotlin
// Before: Spark Card
import com.adevinta.spark.components.card.Card
Card(onClick = { ... }) { ... }

// After: Material 3 + clickable
import androidx.compose.material3.Card
import androidx.compose.foundation.clickable
Card(modifier = Modifier.clickable { ... }) { ... }
```

### Icons
```kotlin
// Before: Text emoji or Spark icons
IconButton(onClick = { ... }) { Text("★") }

// After: Material Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
IconButton(onClick = { ... }) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
        contentDescription = "Favorite",
        tint = if (isFavorite) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.outline
    )
}
```

### Typography
```kotlin
// Before: SparkTheme.typography
Text(text = "Title", style = SparkTheme.typography.caption)

// After: Material 3
import androidx.compose.material3.MaterialTheme
Text(text = "Title", style = MaterialTheme.typography.bodyMedium)
Text(text = "Title", style = MaterialTheme.typography.headlineSmall)
Text(text = "Label", style = MaterialTheme.typography.labelSmall)
```

### Surface/Containers
```kotlin
// Before: Plain Box or Column
Box { ... }

// After: Material 3 Surface
import androidx.compose.material3.Surface
Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = MaterialTheme.shapes.medium
) { ... }
```

## Dependency Addition

```kotlin
// In feature/albums/build.gradle.kts
dependencies {
    // ... existing dependencies
    implementation(libs.androidx.material3)  // Add this line
}
```

## Common Issues & Fixes

### Issue: "This material API is experimental"
```kotlin
// Solution: Add @OptIn annotation
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen() { ... }
```

### Issue: FilterChip doesn't have 'onClick' callback
```kotlin
// FilterChip uses 'selected' state instead:
FilterChip(
    onClick = { ... },      // Click handler for selection toggle
    label = { Text("...") },
    selected = isSelected   // Shows current selected state
)
```

### Issue: Material Card doesn't have 'onClick' parameter
```kotlin
// Use clickable modifier instead:
import androidx.compose.foundation.clickable

Card(
    modifier = Modifier.clickable { onItemSelected(item) }
) { ... }
```

### Issue: ArrowBack icon is deprecated
```kotlin
// Use AutoMirrored version:
import androidx.compose.material.icons.automirrored.filled.ArrowBack
Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
```

## Material 3 Color Scheme Quick Reference

| Component | Color Property |
|-----------|-----------------|
| Primary buttons/icons (active) | `MaterialTheme.colorScheme.primary` |
| Outline/inactive elements | `MaterialTheme.colorScheme.outline` |
| Surface backgrounds | `MaterialTheme.colorScheme.surface` |
| Subtle backgrounds | `MaterialTheme.colorScheme.surfaceContainerLow` |
| Text | `MaterialTheme.colorScheme.onSurface` |

## Material 3 Typography Quick Reference

| Usage | Style |
|-------|-------|
| Page titles | Not needed (use TopAppBar) |
| Section headers | `titleMedium` |
| Card titles | `bodyMedium` or `bodyLarge` |
| Labels | `labelSmall` |
| Body text | `bodyMedium` or `bodySmall` |
| Emphasis | Add `fontWeight = FontWeight.Bold` |

## File Locations

| File | Package |
|------|---------|
| AlbumsScreen.kt | `fr.leboncoin.feature.albums.presentation.liste` |
| AlbumDetailScreen.kt | `fr.leboncoin.feature.albums.presentation.details` |
| AlbumItem.kt | `fr.leboncoin.feature.albums.ui` |

## Testing Notes

- All unit tests pass without modification
- Preview functions remain functional (add @OptIn if needed)
- No changes to ViewModel or state management required
- No changes to navigation required
- Offline behavior unchanged

## Performance Impact

- ✅ Material 3 components are optimized
- ✅ No performance degradation expected
- ✅ Potentially faster rendering due to better optimization
- ✅ Bundle size slightly smaller (less external dependencies)

## Resources

- [Material Design 3 Documentation](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material Icons](https://fonts.google.com/icons)
- [Compose Material 3 API](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
