---
name: compose-performance-guard
description: Guard for Jetpack Compose performance, recomposition optimization, strict semantic theme compliance (PureTheme.colors.*), and Right-to-Left (RTL) Arabic typography.
version: 1.0.0
tags: [android, compose, performance, recomposition, designsystem, rtl]
---

# Jetpack Compose Performance & Design System Guard

## 1. Overview & Quality Mandate

This skill governs the UI layer, enforcing high-performance Jetpack Compose best practices, single-source-of-truth theming, and authentic RTL Arabic layout parity.

### Core Mandates
1. **Single Source of Truth (Zero Hardcoded Hex Colors)**:
   - All colors referenced in UI composables must strictly originate from `PureTheme.colors.*` semantic tokens.
   - Hardcoded hex color literals (e.g. `Color(0xFF...)`) or raw default Material colors are **STRICTLY FORBIDDEN** in UI files.
2. **Recomposition Hygiene**:
   - Prevent unnecessary recompositions by hoisting state and keeping composables stateless.
   - Wrap expensive allocations, date formatters, and collection transforms inside `remember`.
   - Use `derivedStateOf` when observing rapidly mutating state (such as scroll positions) to only trigger recomposition on boundary crossings.
3. **RTL-First Arabic Layout Integrity**:
   - Use directional alignment (`Alignment.Start`, `Alignment.End`, `PaddingValues(start = ..., end = ...)`) instead of hardcoded `left`/`right`.
   - Ensure typography scales comfortably accommodate Arabic script without line clipping or awkward breaks.
4. **Fluid Micro-Animations (60 FPS)**:
   - Keep transition durations between 200ms and 300ms using smooth easings (`FastOutSlowInEasing`).
   - Use `graphicsLayer` (alpha, translation, scale) to animate without triggering layout recalculation.

---

## 2. Best Practice Patterns

### 2.1 Centralized Token Consumption
```kotlin
// CORRECT: Semantic token consumption
@Composable
fun VideoCardTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = PureTheme.colors.textPrimary,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}

// INCORRECT (FORBIDDEN): Hardcoded color literal
@Composable
fun BadVideoCardTitle(title: String) {
    Text(text = title, color = Color(0xFFF3F4F6)) // Hardcoded hex!
}
```

### 2.2 Recomposition Optimization with `derivedStateOf`
```kotlin
val listState = rememberLazyListState()

// Trigger recomposition only when the boolean state changes, not on every pixel scrolled
val showScrollToTopButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 2 }
}
```

### 2.3 Double-Tap Debouncing on Buttons
Prevent rapid duplicate clicks on async actions:

```kotlin
@Composable
fun DebouncedActionButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = PureTheme.colors.primary,
            contentColor = PureTheme.colors.background
        ),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = PureTheme.colors.background,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(text = text)
        }
    }
}
```

---

## 3. Checklist for Code Reviews

- [ ] Does any UI file contain hardcoded `Color(0x...)`? -> **Flag as [CRITICAL]**.
- [ ] Are list items using unique `key` parameters in `LazyColumn` / `LazyRow`? -> **Required**.
- [ ] Are modifier chains structured efficiently without redundant allocations? -> **Required**.
- [ ] Is layout direction properly handled with `start`/`end` paddings? -> **Required**.
