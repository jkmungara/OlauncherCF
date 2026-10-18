package app.olaunchercf.data

import android.os.UserHandle

data class AppModel(
    val appLabel: String,
    val appPackage: String,
    val appActivityName: String,
    val user: UserHandle,
    val appAlias: String,
) {
    /** Name shown to the user: the alias if one is set, otherwise the system label. */
    val name: String
        get() = appAlias.ifEmpty { appLabel }

    /** Key under which this app is stored in the hidden apps set. */
    val hiddenKey: String
        get() = "$appPackage|$user"
}
