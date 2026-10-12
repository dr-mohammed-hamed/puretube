package com.dr.tech.puretube.features.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.data.model.SearchFilter
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Filter chips row: الكل / فيديوهات / قنوات.
 * PureTheme tokens only, RTL by default.
 */
@Composable
fun SearchFilterRow(
    selected: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = remember {
        listOf(
            SearchFilter.ALL to "الكل",
            SearchFilter.VIDEOS to "فيديوهات",
            SearchFilter.CHANNELS to "قنوات"
        )
    }
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.first.name }) { (filter, label) ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = label,
                        style = PureTheme.typography.labelMedium,
                        color = if (isSelected) PureTheme.colors.background else PureTheme.colors.textPrimary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PureTheme.colors.primary,
                    containerColor = PureTheme.colors.surface,
                    selectedLabelColor = PureTheme.colors.background,
                    labelColor = PureTheme.colors.textPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) PureTheme.colors.primary else PureTheme.colors.borderOutline
                )
            )
        }
    }
}
