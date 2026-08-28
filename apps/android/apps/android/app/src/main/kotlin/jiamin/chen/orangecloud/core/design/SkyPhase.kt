package jiamin.chen.orangecloud.core.design

import androidx.compose.ui.graphics.Color
import jiamin.chen.orangecloud.core.design.theme.OcOrange

/**
 * 「晨昏」天色定义：由时刻 + 外观模式推导（亮色 = 昼，暗色 = 夜）。
 * 与 iOS 的 Shared/SkyPhase.swift 同一相位表，直译保持一致。
 *
 * 这是品牌签名层，永不被 Material You 动态取色覆盖。
 */
enum class SkyPhase {
    Dawn,   // 清晨（亮）
    Day,    // 白天（亮）
    Dusk,   // 黄昏（亮）
    Ember,  // 入夜，余晖未尽（暗）
    Night;  // 深夜（暗）

    /** 天空主体（自上而下） */
    val body: List<Color>
        get() = when (this) {
            Dawn -> listOf(Color(0xFFFFE8D1), Color(0xFFF5F2ED))
            Day -> listOf(Color(0xFFFCF2E3), Color(0xFFF2F2F2))
            Dusk -> listOf(Color(0xFFFFDEBF), Color(0xFFF0EDF2))
            Ember -> listOf(Color(0xFF1F1208), Color(0xFF0F0E13), Color(0xFF0A0A0F))
            Night -> listOf(Color(0xFF120D0A), Color(0xFF0A0A0E))
        }

    /** 顶部光源（白昼是日光，夜里是城市上空的橙色辉光） */
    val glow: Color
        get() = when (this) {
            Dawn -> Color(0xFFFFB066).copy(alpha = 0.50f)
            Day -> OcOrange.copy(alpha = 0.20f)
            Dusk -> Color(0xFFF58542).copy(alpha = 0.42f)
            Ember -> OcOrange.copy(alpha = 0.30f)
            Night -> OcOrange.copy(alpha = 0.15f)
        }

    val isDark: Boolean get() = this == Ember || this == Night

    companion object {
        fun current(isDark: Boolean, hour: Int): SkyPhase {
            if (isDark) return if (hour in 17..22) Ember else Night
            return when (hour) {
                in 5..8 -> Dawn
                in 9..15 -> Day
                in 16..23 -> Dusk
                else -> Dawn
            }
        }
    }
}
