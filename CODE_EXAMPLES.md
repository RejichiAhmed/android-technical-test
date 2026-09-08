# Material Design 3 Refactoring - Code Examples

## AlbumsScreen.kt - Before & After

### Before: Using Spark Scaffold & Chips
```kotlin
@OptIn(ExperimentalSparkApi::class)
@Composable
fun AlbumsListScreen(
    state: AlbumsState,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CategoryTopBar(...)  // Custom top bar
        },
    ) { contentPadding ->
        // Content...
    }
}

@Composable
private fun CategoryTopBar(
    availableCategories: List<Int>,
    selectedCategory: Int?,
    onCategorySelected: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipTinted(
            text = "All",
            intent = if (selectedCategory == null) ChipIntent.Main else ChipIntent.Basic,
            onClick = { onCategorySelected(null) },
        )
        availableCategories.forEach { albumId ->
            ChipTinted(
                text = "Album #$albumId",
                intent = if (selectedCategory == albumId) ChipIntent.Main else ChipIntent.Basic,
                onClick = { onCategorySelected(albumId) },
            )
        }
    }
}
```

### After: Using Material 3 Scaffold & FilterChips
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsListScreen(
    state: AlbumsState,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            CategoryTopBar(...)  // Now just a component inside content
            // Rest of content...
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTopBar(
    availableCategories: List<Int>,
    selectedCategory: Int?,
    onCategorySelected: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            selected = selectedCategory == null,
        )
        availableCategories.forEach { albumId ->
            FilterChip(
                onClick = { onCategorySelected(albumId) },
                label = { Text("Album #$albumId") },
                selected = selectedCategory == albumId,
            )
        }
    }
}
```

## AlbumDetailScreen.kt - Before & After

### Before: Basic Layout with Spark Button
```kotlin
@Composable
fun AlbumDetailScreen(
    state: AlbumsState,
    albumId: Int,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = state.findAlbum(albumId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (album == null && state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (album == null) {
            Text("Album not found")
            ButtonFilled(
                onClick = { onAction(AlbumsAction.OnBackClick) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back")
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = album.title)
            IconButton(
                onClick = { onAction(AlbumsAction.OnFavoriteToggle(album.id)) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = if (album.isFavorite) "★" else "☆")
            }
        }
        Text(text = album.albumLabel)
        Text(text = album.trackLabel)
        ButtonFilled(
            onClick = { onAction(AlbumsAction.OnBackClick) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}
```

### After: Rich Design with Material Components
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    state: AlbumsState,
    albumId: Int,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = state.findAlbum(albumId)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Album Details") },
                navigationIcon = {
                    IconButton(onClick = { onAction(AlbumsAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        when {
            album == null && state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            album == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Album not found")
                        Button(
                            onClick = { onAction(AlbumsAction.OnBackClick) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Back")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Album Image Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Column {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(album.thumbnailUrl)
                                    .httpHeaders(
                                        NetworkHeaders.Builder()
                                            .add("User-Agent", "LeboncoinApp/1.0")
                                            .build()
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = album.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop,
                            )

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = album.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { onAction(AlbumsAction.OnFavoriteToggle(album.id)) },
                                    ) {
                                        Icon(
                                            imageVector = if (album.isFavorite) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.Star
                                            },
                                            contentDescription = "Add to favorites",
                                            tint = if (album.isFavorite) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Details Surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                Text(
                                    text = "Album",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = album.albumLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column {
                                Text(
                                    text = "Track",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = album.trackLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
```

## AlbumItem.kt - Before & After

### Before: Spark Card with Chip Labels
```kotlin
@OptIn(ExperimentalSparkApi::class)
@Composable
fun AlbumItem(
    album: AlbumUi,
    onItemSelected: (AlbumUi) -> Unit,
    onFavoriteToggle: (AlbumUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp),
        onClick = { onItemSelected(album) },
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(...)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
            ) {
                Text(
                    text = album.title,
                    style = SparkTheme.typography.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipTinted(text = album.albumLabel)
                    ChipTinted(text = album.trackLabel)
                }
            }

            IconButton(onClick = { onFavoriteToggle(album) }) {
                Text(text = if (album.isFavorite) "★" else "☆")
            }
        }
    }
}
```

### After: Material Card with Material Icons
```kotlin
@Composable
fun AlbumItem(
    album: AlbumUi,
    onItemSelected: (AlbumUi) -> Unit,
    onFavoriteToggle: (AlbumUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
            .clickable { onItemSelected(album) },
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(...)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = album.albumLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = album.trackLabel,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = { onFavoriteToggle(album) }) {
                Icon(
                    imageVector = if (album.isFavorite) {
                        Icons.Filled.Star
                    } else {
                        Icons.Outlined.Star
                    },
                    contentDescription = "Add to favorites",
                    tint = if (album.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}
```

## Key Patterns Demonstrated

1. **Scaffold Pattern**: Use Material Scaffold instead of Spark
2. **TopAppBar**: Add Material TopAppBar in Scaffold's topBar parameter
3. **Icon Usage**: Use Material Icons with color theming
4. **Typography**: Use MaterialTheme.typography for consistent sizing
5. **Surfaces**: Use Material Surface for content grouping
6. **Cards**: Use Material Card with clickable modifier
7. **Chips**: Use Material FilterChip for selection state
8. **Buttons**: Use Material Button instead of ButtonFilled

All patterns maintain the same state management and architecture while improving the visual design and consistency.
