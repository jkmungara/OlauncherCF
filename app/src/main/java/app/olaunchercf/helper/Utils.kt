package app.olaunchercf.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.core.app.ActivityCompat
import app.olaunchercf.BuildConfig
import app.olaunchercf.data.AppModel
import app.olaunchercf.data.Constants.BACKUP_READ
import app.olaunchercf.data.Constants.BACKUP_WRITE
import app.olaunchercf.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.Collator

private const val TAG = "Utils"

fun showToastLong(context: Context, message: String) {
    val toast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun showToastShort(context: Context, message: String) {
    val toast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

/**
 * Loads every launchable activity of every user profile, sorted by display name.
 *
 * Hidden apps are included; callers filter them with [AppModel.hiddenKey]. This keeps
 * hiding and unhiding apps free, since nothing has to be reloaded from the system.
 *
 * Returns null when the list could not be loaded, so callers can keep what they have and retry.
 */
suspend fun getAppsList(context: Context): List<AppModel>? = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val prefs = Prefs(appContext)

    try {
        if (!prefs.hiddenAppsUpdated) {
            upgradeHiddenApps(prefs)
        }

        val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val apps = ArrayList<AppModel>()

        for (profile in userManager.userProfiles) {
            ensureActive()

            // One broken or locked profile must not take the whole list down with it.
            val activities = try {
                launcherApps.getActivityList(null, profile)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to load apps for $profile", e)
                continue
            }

            for (activity in activities) {
                val packageName = activity.componentName.packageName

                if (packageName == BuildConfig.APPLICATION_ID) {
                    continue
                }

                val appLabel = activity.label?.toString()?.trim().orEmpty().ifEmpty { packageName }
                val appAlias = prefs.getAppAlias(packageName).ifEmpty { prefs.getAppAlias(appLabel) }

                apps.add(
                    AppModel(
                        appLabel = appLabel,
                        appPackage = packageName,
                        appActivityName = activity.componentName.className,
                        user = profile,
                        appAlias = appAlias,
                    )
                )
            }
        }

        sortApps(apps)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Unable to load app list", e)
        null
    }
}

/** Sorts apps by the name the user sees, using the rules of the current locale. */
fun sortApps(apps: List<AppModel>): List<AppModel> {
    val collator = Collator.getInstance()
    return apps.sortedWith { first, second -> collator.compare(first.name, second.name) }
}

private fun upgradeHiddenApps(prefs: Prefs) {
    val hiddenAppsSet = prefs.hiddenApps
    val newHiddenAppsSet = mutableSetOf<String>()
    val myUser = Process.myUserHandle()

    for (hiddenPackage in hiddenAppsSet) {
        if (hiddenPackage.contains("|")) {
            newHiddenAppsSet.add(hiddenPackage)
        } else {
            newHiddenAppsSet.add("$hiddenPackage|$myUser")
        }
    }

    prefs.hiddenApps = newHiddenAppsSet
    prefs.hiddenAppsUpdated = true
}

// User profiles practically never change while the launcher runs, but every stored app
// (home apps, gesture apps) resolves its profile through here. Without the cache each of
// those reads was a binder call into UserManager.
@Volatile
private var userHandleCache: Map<String, UserHandle> = emptyMap()

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    if (userHandleString.isEmpty()) {
        return Process.myUserHandle()
    }

    userHandleCache[userHandleString]?.let { return it }

    val handles = try {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        userManager.userProfiles.associateBy { it.toString() }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read user profiles", e)
        emptyMap()
    }

    if (handles.isNotEmpty()) {
        userHandleCache = handles
    }

    return handles[userHandleString] ?: Process.myUserHandle()
}

fun isOlauncherDefault(context: Context): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(context)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)

    val packageManager = context.packageManager
    val result = packageManager.resolveActivity(intent, 0)

    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else {
        "android"
    }
}

fun resetDefaultLauncher(context: Context) {
    try {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, FakeHomeActivity::class.java)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(selector)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        Log.w(TAG, "Unable to reset default launcher", e)
    }
}

fun openAppInfo(context: Context, appModel: AppModel) {
    openAppInfo(context, appModel.user, appModel.appPackage, appModel.appActivityName)
}

fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String, activityName: String = "") {
    try {
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        // Resolve through LauncherApps rather than PackageManager so work profile apps work too.
        val component = if (activityName.isNotEmpty()) {
            ComponentName(packageName, activityName)
        } else {
            launcher.getActivityList(packageName, userHandle).firstOrNull()?.componentName
        }

        if (component != null) {
            launcher.startAppDetailsActivity(component, userHandle, null, null)
            return
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to open app info through LauncherApps", e)
    }

    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        showToastShort(context, "Unable to open app info")
    }
}

fun openDialerApp(context: Context) {
    try {
        val sendIntent = Intent(Intent.ACTION_DIAL)
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        Log.d(TAG, "Unable to open dialer: $e")
    }
}

fun openCameraApp(context: Context) {
    try {
        val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        Log.d(TAG, "Unable to open camera: $e")
    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.d(TAG, "Unable to open alarm: $e")
    }
}

fun openCalendar(context: Context) {
    try {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(System.currentTimeMillis().toString())

        context.startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Unable to open calendar: $e")
        }
    }
}

/**
 * Standard Android definition of a tablet. The old physical-size calculation relied on
 * xdpi/ydpi, which many devices report incorrectly.
 */
fun isTablet(context: Context): Boolean {
    return context.resources.configuration.smallestScreenWidthDp >= 600
}

fun initActionService(context: Context): ActionService? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val actionService = ActionService.instance()

        if (actionService != null) {
            return actionService
        }

        openAccessibilitySettings(context)
    } else {
        showToastLong(context, "This action requires Android P (9) or higher")
    }

    return null
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val componentString = ComponentName(context.packageName, ActionService::class.java.name).flattenToString()
    val bundle = Bundle()

    bundle.putString(":settings:fragment_args_key", componentString)

    intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(":settings:fragment_args_key", componentString)
        putExtra(":settings:show_fragment_args", bundle)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        showToastShort(context, "Unable to open accessibility settings")
    }
}

fun showStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())
    } else {
        @Suppress("DEPRECATION", "InlinedApi")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }
}

fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun uninstallApp(context: Context, appPackage: String) {
    try {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", appPackage, null))
        context.startActivity(intent)
    } catch (e: Exception) {
        showToastShort(context, "Unable to uninstall app")
    }
}

fun dp2px(resources: Resources, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun storeFile(activity: Activity) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "backup.txt")
    }

    try {
        ActivityCompat.startActivityForResult(activity, intent, BACKUP_WRITE, null)
    } catch (e: Exception) {
        showToastShort(activity, "No app available to save the backup")
    }
}

fun loadFile(activity: Activity) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "text/plain"
    }

    try {
        ActivityCompat.startActivityForResult(activity, intent, BACKUP_READ, null)
    } catch (e: Exception) {
        showToastShort(activity, "No app available to open the backup")
    }
}

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandNotificationDrawer(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to expand notifications", e)
    }
}

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandQuickSettings(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandSettingsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to expand quick settings", e)
    }
}
