package com.dr.tech.puretube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.components.HeaderSection
import com.dr.tech.puretube.components.StatusCard
import com.dr.tech.puretube.components.TemporaryHarnessBanner
import com.dr.tech.puretube.components.ThemeSelectorSection
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset

/**
 * =========================================================================================
 * [تنبيه هام للوكلاء والمطورين في الجلسات القادمة / NOTICE FOR FUTURE AGENTS]
 * =========================================================================================
 * هذه الشاشة (FoundationShowcaseScreen) والارتباط المباشر داخل MainActivity هما شاشة فحص مؤقتة
 * (Temporary Smoke-Test Harness) خاصة باعتماد المرحلة الأولى (Phase 1: Infrastructure Setup).
 *
 * في المرحلة الثالثة (Phase 3: UI & Navigation):
 * 1. استبدل محتوى MainActivity بالهيكل الرئيسي الدائم MainNavigationShell:
 *    - شريط التبويبات السفلي: (الرئيسية Feed، الاشتراكات Subscriptions، المشاهدة لاحقاً WatchLater).
 * 2. انقل كود اختيار الثيمات (ThemeSelectorSection) إلى شاشة الإعدادات الدائمة (SettingsScreen).
 * 3. لا تبنِ ميزات الإنتاج داخل هذه الشاشة المؤقتة.
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentPreset by rememberSaveable { mutableStateOf(ThemePreset.EMERALD_NIGHT) }

            PureTheme(preset = currentPreset) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureTheme.colors.background
                ) {
                    TemporaryMilestone1HarnessScreen(
                        currentPreset = currentPreset,
                        onPresetSelected = { currentPreset = it }
                    )
                }
            }
        }
    }
}

/**
 * شاشة الفحص المؤقتة للمرحلة الأولى.
 */
@Deprecated(
    message = "Temporary smoke-test harness for Phase 1. Must be replaced by MainNavigationShell in Phase 3.",
    level = DeprecationLevel.WARNING
)
@Composable
private fun TemporaryMilestone1HarnessScreen(
    currentPreset: ThemePreset,
    onPresetSelected: (ThemePreset) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            // شريط التنبيه المرئي الصريح بأن الشاشة مؤقتة
            TemporaryHarnessBanner()
        }

        item {
            HeaderSection()
        }

        item {
            ThemeSelectorSection(
                currentPreset = currentPreset,
                onPresetSelected = onPresetSelected
            )
        }

        item {
            Text(
                text = "الأنظمة المعمارية المفحوصة والمعتمدة",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        }

        item {
            StatusCard(
                title = "نظام التصميم المركزي (Vibrant Purity SSOT)",
                subtitle = "6 هويات لونية جاهزة، حظر للألوان الثابتة، وامتثال لقاعدة Zero-Emoji",
                icon = Icons.Default.Palette
            )
        }

        item {
            StatusCard(
                title = "محرك استخراج البيانات والشبكة",
                subtitle = "NewPipeExtractor مدمج مع OkHttp ومحدد تدفق آمن Semaphore(4)",
                icon = Icons.Default.Speed
            )
        }

        item {
            StatusCard(
                title = "مشغل الوسائط والبث المتقدم",
                subtitle = "AndroidX Media3 ExoPlayer جاهز لدمج مسارات DASH الصوتية والمرئية",
                icon = Icons.Default.PlayCircle
            )
        }

        item {
            StatusCard(
                title = "منافذ الحماية والذكاء الاصطناعي",
                subtitle = "واجهات VideoFrameHook و AudioFilterHook معدة لربط halalify-ai-core",
                icon = Icons.Default.Security
            )
        }

        item {
            StatusCard(
                title = "نظام حقن الاعتماديات والمعمارية",
                subtitle = "Koin Dependency Injection بنمط Feature-First وامتثال تام للدستور",
                icon = Icons.Default.Hub
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
