package fr.leboncoin.feature.favorites.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects [flow] only while the lifecycle is at least [STARTED][Lifecycle.State.STARTED],
 * invoking [onEvent] for every emitted value. Intended for one-time side effects
 * (navigation, snackbars) emitted from a ViewModel's event `Channel`.
 *
 * Local to `:feature:favorites` until a shared `core:presentation` module exists.
 */
@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(onEvent)
        }
    }
}
