package app.olaunchercf.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.olaunchercf.R
import java.util.Locale

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {

    const val REQUEST_CODE_ENABLE_ADMIN = 666

    const val LONG_PRESS_DELAY_MS = 500

    const val MAX_HOME_APPS = 15
    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 60

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        SetHomeApp,
        SetSwipeLeft,
        SetSwipeRight,
        SetSwipeUp,
        SetSwipeDown,
        SetClickClock,
        SetClickDate,
        SetDoubleTap,
    }

    enum class Language: EnumOption {
        System,
        Arabic,
        Chinese,
        Croatian,
        Dutch,
        English,
        Estonian,
        French,
        German,
        Greek,
        Indonesian,
        Italian,
        Japanese,
        Korean,
        Lithuanian,
        Persian,
        Portuguese,
        Russian,
        Spanish,
        Swedish,
        Thai,
        Turkish,
        Vietnamese;

        @Composable
        override fun string(): String {
            return when(this) {
                System -> stringResource(R.string.lang_system)
                Arabic -> "العربية"
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Dutch -> "Nederlands"
                English -> "English"
                Estonian -> "Eesti keel"
                French -> "Français"
                German -> "Deutsch"
                Greek -> "Ελληνική"
                Indonesian -> "Bahasa Indonesia"
                Italian -> "Italiano"
                Japanese -> "日本"
                Korean -> "한국어"
                Lithuanian -> "Lietuvių"
                Persian -> "فارسی"
                Portuguese -> "Português"
                Russian -> "Русский"
                Spanish -> "Español"
                Swedish -> "Svenska"
                Thai -> "ไทย"
                Turkish -> "Türkçe"
                Vietnamese -> "Tiếng Việt"
            }
        }
    }

    /** Locale used for the app's resources. Regions match the translated resource folders. */
    fun Language.locale(): Locale {
        return when (this) {
            Language.System -> Locale.getDefault()
            Language.Arabic -> Locale("ar", "SA")
            Language.Chinese -> Locale.SIMPLIFIED_CHINESE
            Language.Croatian -> Locale("hr", "HR")
            Language.Dutch -> Locale("nl", "NL")
            Language.English -> Locale.ENGLISH
            Language.Estonian -> Locale("et", "EE")
            Language.French -> Locale("fr", "FR")
            Language.German -> Locale("de", "DE")
            Language.Greek -> Locale("el", "GR")
            Language.Indonesian -> Locale("in", "ID")
            Language.Italian -> Locale("it", "IT")
            Language.Japanese -> Locale("ja", "JP")
            Language.Korean -> Locale("ko", "KR")
            Language.Lithuanian -> Locale("lt", "LT")
            Language.Persian -> Locale("fa", "IR")
            Language.Portuguese -> Locale("pt", "BR")
            Language.Russian -> Locale("ru", "RU")
            Language.Spanish -> Locale("es", "ES")
            Language.Swedish -> Locale("sv", "SE")
            Language.Thai -> Locale("th", "TH")
            Language.Turkish -> Locale("tr", "TR")
            Language.Vietnamese -> Locale("vi", "VN")
        }
    }

    fun Language.value(): String = locale().language

    enum class Gravity: EnumOption {
        Left,
        Center,
        Right;

        @Composable
        override fun string(): String {
            return when(this) {
                Left -> stringResource(R.string.left)
                Center -> stringResource(R.string.center)
                Right -> stringResource(R.string.right)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when(this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
            }
        }
    }

    enum class Action: EnumOption {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification;

        @Composable
        override fun string(): String {
            return when(this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Theme: EnumOption {
        System,
        Dark,
        Light;

        @Composable
        override fun string(): String {
            return when(this) {
                System -> stringResource(R.string.lang_system)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }
}
