package app.olaunchercf

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Constants.locale
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.ActivityMainBinding
import app.olaunchercf.helper.isTablet
import app.olaunchercf.helper.showToastLong
import java.io.FileOutputStream

private const val TAG = "MainActivity"

// A settings backup is a few KB. Anything far bigger is the wrong file.
private const val MAX_BACKUP_CHARS = 1_000_000

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onBackPressed() {
        if (navController.currentDestination?.id != R.id.mainFragment) {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)

        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)

        // Before inflating anything, so the first frame is already in the right language.
        setLanguage()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        initClickListeners()
        initObservers(viewModel)

        // Warm the app list cache once the first frame is out, so loading the app labels
        // does not compete with drawing the home screen.
        binding.root.doOnPreDraw { root ->
            root.post { viewModel.getAppList() }
        }

        setupOrientation()
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    @Suppress("DEPRECATION")
    fun setLanguage() {
        val config = resources.configuration
        config.setLocale(prefs.language.locale())
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        backToHomeScreen()
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Only uiMode changes are delivered here, and they only matter when following the
        // system theme. With a fixed theme there is nothing to redraw.
        if (prefs.appTheme == Constants.Theme.System) {
            recreate()
        }
    }

    private fun initClickListeners() {
        binding.okay.setOnClickListener {
            binding.messageLayout.visibility = View.GONE
            viewModel.showMessageDialog("")
        }
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) {
            openLauncherChooser(it)
        }

        viewModel.showMessageDialog.observe(this) {
            showMessage(it)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return

        // Android 8.0 throws when a translucent activity requests a fixed orientation.
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun backToHomeScreen() {
        if (!::navController.isInitialized) return

        if (navController.currentDestination?.id != R.id.mainFragment) {
            navController.popBackStack(R.id.mainFragment, false)
        }
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (!resetFailed) return

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            showToastLong(this, "Search for launcher or home app")
            Intent(Settings.ACTION_SETTINGS)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open launcher settings", e)
        }
    }

    private fun showMessage(message: String) {
        if (message.isEmpty()) return

        binding.messageTextView.text = message
        binding.messageLayout.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                prefs.lockModeOn = true

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    showMessage(getString(R.string.double_tap_lock_is_enabled_message))
                } else {
                    showMessage(getString(R.string.double_tap_lock_uninstall_message))
                }
            }

            Constants.BACKUP_READ -> data?.data?.let { restoreBackup(it) }

            Constants.BACKUP_WRITE -> data?.data?.let { writeBackup(it) }
        }
    }

    private fun restoreBackup(uri: Uri) {
        val text = try {
            readText(uri)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read backup", e)
            null
        }

        // Settings are only replaced once the file is known to be a valid backup.
        if (text == null || !Prefs(applicationContext).loadFromString(text)) {
            showToastLong(this, "Unable to load backup")
            return
        }

        startActivity(Intent.makeRestartActivityTask(intent?.component))
    }

    private fun readText(uri: Uri): String? {
        val input = contentResolver.openInputStream(uri) ?: return null

        return input.bufferedReader().use { reader ->
            val text = StringBuilder()
            val buffer = CharArray(8 * 1024)

            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break

                text.append(buffer, 0, count)
                if (text.length > MAX_BACKUP_CHARS) return null
            }

            text.toString()
        }
    }

    private fun writeBackup(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                FileOutputStream(file.fileDescriptor).use { stream ->
                    val text = Prefs(applicationContext).saveToString()
                    stream.channel.truncate(0)
                    stream.write(text.toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to write backup", e)
            showToastLong(this, "Unable to save backup")
        }
    }
}
