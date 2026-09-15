package com.dr.tech.puretube.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.navigation.PureNavTab

/**
 * Shared Bottom Navigation Bar for PureTube (نَقِيّ).
 * Renders the 4 core tabs with active glow and smooth state transitions.
 * Conforms to Single Source of Truth Theme tokens, Zero-Emoji discipline, and RTL layout.
 */
@Composable
fun PureBottomNavBar(
    currentTab: PureNavTab,
    onTabSelected: (PureNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1.dp Top border outline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PureTheme.colors.borderOutline)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PureNavTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val animatedTint by animateColorAsState(
                        targetValue = if (isSelected) PureTheme.colors.primary else PureTheme.colors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavTabColor_${tab.name}"
                    )

                    val icon = if (isSelected) tab.iconSelected else tab.iconUnselected

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PureTheme.colors.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            } else {
                                Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.titleArabic,
                                tint = animatedTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.titleArabic,
                            color = animatedTint,
                            style = if (isSelected) {
                                PureTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            } else {
                                PureTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
