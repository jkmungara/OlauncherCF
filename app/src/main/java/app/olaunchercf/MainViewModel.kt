package app.olaunchercf

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.olaunchercf.data.AppModel
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.data.Prefs
import app.olaunchercf.helper.getAppsList
import app.olaunchercf.helper.getDefaultLauncherPackage
import app.olaunchercf.helper.isOlauncherDefault
import app.olaunchercf.helper.resetDefaultLauncher
import app.olaunchercf.helper.showToastShort
import app.olaunchercf.helper.sortApps
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

// App updates fire several package events in a row. Wait for them to settle and reload once.
private const val PACKAGE_CHANGE_DEBOUNCE_MS = 1_000L

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    private val prefs = Prefs(appContext)
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    val showMessageDialog = MutableLiveData<String>()

    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isOlauncherDefault = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()

    val showTime = MutableLiveData(prefs.showTime)
    val showDate = MutableLiveData(prefs.showDate)
    val clockAlignment = MutableLiveData(prefs.clockAlignment)
    val homeAppsAlignment = MutableLiveData(Pair(prefs.homeAlignment, prefs.homeAlignmentBottom))
    val homeAppsCount = MutableLiveData(prefs.homeAppsNum)

    // App list cache. Everything below is only touched on the main thread.

    /** Every launchable app, hidden ones included, sorted. Null until the first load. */
    private var allApps: List<AppModel>? = null

    /** Bumped on every package event; compared with [loadedPackageChanges] to detect a stale cache. */
    private var packageChanges = 0
    private var loadedPackageChanges = -1

    /** Whether [appList] should contain hidden apps (used when picking a home screen app). */
    private var includeHiddenApps = false

    private var loadJob: Job? = null
    private var reloadJob: Job? = null

    private val isCacheStale: Boolean
        get() = allApps == null || loadedPackageChanges != packageChanges

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onPackagesChanged()

        override fun onPackageAdded(packageName: String?, user: UserHandle?) = onPackagesChanged()

        override fun onPackageChanged(packageName: String?, user: UserHandle?) = onPackagesChanged()

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            onPackagesChanged()

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            onPackagesChanged()
    }

    init {
        try {
            launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to listen for package changes", e)
        }
    }

    override fun onCleared() {
        try {
            launcherApps.unregisterCallback(packageCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to unregister package callback", e)
        }
        super.onCleared()
    }

    fun selectedApp(appModel: AppModel, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps -> {
                launchApp(appModel)
            }

            AppDrawerFlag.SetHomeApp -> {
                prefs.setHomeAppModel(n, appModel)
            }

            AppDrawerFlag.SetSwipeLeft -> prefs.appSwipeLeft = appModel
            AppDrawerFlag.SetSwipeRight -> prefs.appSwipeRight = appModel
            AppDrawerFlag.SetSwipeUp -> prefs.appSwipeUp = appModel
            AppDrawerFlag.SetSwipeDown -> prefs.appSwipeDown = appModel
            AppDrawerFlag.SetClickClock -> prefs.appClickClock = appModel
            AppDrawerFlag.SetClickDate -> prefs.appClickDate = appModel
            AppDrawerFlag.SetDoubleTap -> prefs.appDoubleTap = appModel
        }
    }

    fun setShowDate(visibility: Boolean) {
        showDate.value = visibility
    }

    fun setShowTime(visibility: Boolean) {
        showTime.value = visibility
    }

    private fun launchApp(appModel: AppModel) {
        val activities: List<LauncherActivityInfo> = try {
            launcherApps.getActivityList(appModel.appPackage, appModel.user)
        } catch (e: Exception) {
            emptyList()
        }

        // Prefer the stored activity, but survive apps renaming their launcher activity.
        val component: ComponentName? =
            (activities.firstOrNull { it.componentName.className == appModel.appActivityName }
                ?: activities.lastOrNull())?.componentName

        if (component == null) {
            showToastShort(appContext, "App not found")
            return
        }

        try {
            launcherApps.startMainActivity(component, appModel.user, null, null)
        } catch (e: SecurityException) {
            try {
                launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                showToastShort(appContext, "Unable to launch app")
            }
        } catch (e: Exception) {
            showToastShort(appContext, "Unable to launch app")
        }
    }

    /**
     * Publishes the app list to [appList]. Cached data is published right away; the system is
     * only queried again when the cache is missing or packages changed since the last load.
     */
    fun getAppList(showHiddenApps: Boolean = false) {
        includeHiddenApps = showHiddenApps
        allApps?.let(::publishAppList)
        if (isCacheStale) loadApps()
    }

    fun getHiddenApps() {
        allApps?.let(::publishHiddenApps)
        if (isCacheStale) loadApps()
    }

    /** Hidden apps are filtered in memory, so a change only needs a republish. */
    fun onHiddenAppsChanged() {
        if (allApps != null) publishAll()
    }

    fun setAppAlias(appPackage: String, appAlias: String) {
        prefs.setAppAlias(appPackage, appAlias)

        val apps = allApps ?: return
        val updated = apps.map { app ->
            if (app.appPackage != appPackage) {
                app
            } else {
                // Same fallback as the loader: an alias stored under the label (older versions).
                app.copy(appAlias = appAlias.ifEmpty { prefs.getAppAlias(app.appLabel) })
            }
        }

        allApps = sortApps(updated)
        publishAll()
    }

    private fun onPackagesChanged() {
        packageChanges++
        scheduleReload()
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(PACKAGE_CHANGE_DEBOUNCE_MS)
            loadApps()
        }
    }

    private fun loadApps() {
        // A load in flight re-checks for missed package events when it finishes.
        if (loadJob?.isActive == true) return

        reloadJob?.cancel()
        val changesAtStart = packageChanges

        loadJob = viewModelScope.launch {
            val apps = getAppsList(appContext)

            if (apps != null) {
                allApps = apps
                loadedPackageChanges = changesAtStart
            }

            // On failure an older list (or an empty one) is shown and the next request retries.
            publishAll()

            if (apps != null && packageChanges != changesAtStart) {
                scheduleReload()
            }
        }
    }

    private fun publishAll() {
        val apps = allApps ?: emptyList()
        publishAppList(apps)
        publishHiddenApps(apps)
    }

    private fun publishAppList(apps: List<AppModel>) {
        if (includeHiddenApps) {
            appList.value = apps
            return
        }

        val hidden = prefs.hiddenApps
        appList.value = if (hidden.isEmpty()) apps else apps.filterNot { it.hiddenKey in hidden }
    }

    private fun publishHiddenApps(apps: List<AppModel>) {
        val hidden = prefs.hiddenApps
        hiddenApps.value = if (hidden.isEmpty()) emptyList() else apps.filter { it.hiddenKey in hidden }
    }

    fun isOlauncherDefault() {
        isOlauncherDefault.value = isOlauncherDefault(appContext)
    }

    fun resetDefaultLauncherApp(context: Context) {
        resetDefaultLauncher(context)
        launcherResetFailed.value = getDefaultLauncherPackage(appContext).contains(".")
    }

    fun updateDrawerAlignment(gravity: Constants.Gravity) {
        prefs.drawerAlignment = gravity
    }

    fun updateClockAlignment(gravity: Constants.Gravity) {
        clockAlignment.value = gravity
    }

    fun updateHomeAppsAlignment(gravity: Constants.Gravity, onBottom: Boolean) {
        homeAppsAlignment.value = Pair(gravity, onBottom)
    }

    fun showMessageDialog(message: String) {
        showMessageDialog.postValue(message)
    }
}
