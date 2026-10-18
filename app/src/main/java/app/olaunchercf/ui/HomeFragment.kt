package app.olaunchercf.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olaunchercf.MainViewModel
import app.olaunchercf.R
import app.olaunchercf.data.AppModel
import app.olaunchercf.data.Constants.Action
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.FragmentHomeBinding
import app.olaunchercf.helper.ActionService
import app.olaunchercf.helper.expandNotificationDrawer
import app.olaunchercf.helper.expandQuickSettings
import app.olaunchercf.helper.hideStatusBar
import app.olaunchercf.helper.initActionService
import app.olaunchercf.helper.isOlauncherDefault
import app.olaunchercf.helper.openAccessibilitySettings
import app.olaunchercf.helper.openAlarmApp
import app.olaunchercf.helper.openCalendar
import app.olaunchercf.helper.openCameraApp
import app.olaunchercf.helper.openDialerApp
import app.olaunchercf.helper.showStatusBar
import app.olaunchercf.helper.showToastLong
import app.olaunchercf.helper.showToastShort
import app.olaunchercf.listener.OnSwipeTouchListener
import app.olaunchercf.listener.ViewSwipeTouchListener

private const val TAG = "HomeFragment"

class HomeFragment : Fragment(), View.OnClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())

        if (prefs.firstSettingsOpen()) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()

        binding.touchArea.setOnTouchListener(getHomeScreenGestureListener(requireContext()))
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? =
        createLayeredAnimator(nextAnim)

    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        binding.clock.textSize = prefs.textSize * 2.5f
        binding.date.textSize = prefs.textSize.toFloat()
    }

    override fun onResume() {
        super.onResume()

        // only show "set as default"-button if tips are GONE
        if (binding.firstRunTips.visibility == View.GONE) {
            binding.setDefaultLauncher.visibility =
                if (isOlauncherDefault(requireContext())) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The fragment outlives its view while another screen is shown; don't keep the old views alive.
        _binding = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clock -> performAction(prefs.clickClockAction, ::openClickClockApp)
            R.id.date -> performAction(prefs.clickDateAction, ::openClickDateApp)
            R.id.setDefaultLauncher -> viewModel.resetDefaultLauncherApp(requireContext())
        }
    }

    private fun initObservers() {
        with(viewModel) {
            clockAlignment.observe(viewLifecycleOwner) { gravity ->
                binding.dateTimeLayout.gravity = gravity.value()
            }
            homeAppsAlignment.observe(viewLifecycleOwner) { (gravity, onBottom) ->
                val verticalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = gravity.value() or verticalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    (view as TextView).gravity = gravity.value()
                }
            }
            homeAppsCount.observe(viewLifecycleOwner) {
                updateAppCount(it)
            }
            showTime.observe(viewLifecycleOwner) {
                binding.clock.visibility = if (it) View.VISIBLE else View.GONE
            }
            showDate.observe(viewLifecycleOwner) {
                binding.date.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun homeAppClicked(location: Int) {
        val appModel = prefs.getHomeAppModel(location)

        if (appModel.appLabel.isEmpty()) showLongPressToast()
        else launchApp(appModel)
    }

    private fun homeAppLongClicked(view: View) {
        if (prefs.homeLocked) return

        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        val location = view.id
        // A slot that already has an app may also be given a hidden app.
        val hasApp = prefs.getHomeAppModel(location).appLabel.isNotEmpty()
        showAppList(AppDrawerFlag.SetHomeApp, showHiddenApps = hasApp, n = location)
    }

    private fun launchApp(appModel: AppModel) {
        viewModel.selectedApp(appModel, AppDrawerFlag.LaunchApp)
    }

    private fun showAppList(flag: AppDrawerFlag, showHiddenApps: Boolean = false, n: Int = 0) {
        navigateFromHome(
            R.id.action_mainFragment_to_appListFragment,
            bundleOf("flag" to flag.toString(), "n" to n, "showHidden" to showHiddenApps)
        )
    }

    /**
     * Navigates only while home is the current screen. Gestures can fire twice in a row
     * (two quick swipes, a long press racing a swipe) and must not stack screens.
     */
    private fun navigateFromHome(actionId: Int, args: Bundle? = null) {
        if (!isAdded) return

        try {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.mainFragment) {
                navController.navigate(actionId, args)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to navigate", e)
        }
    }

    private fun openGestureApp(appModel: AppModel, fallback: () -> Unit) {
        if (appModel.appPackage.isNotEmpty()) launchApp(appModel) else fallback()
    }

    private fun openSwipeRightApp() = openGestureApp(prefs.appSwipeRight) { openDialerApp(requireContext()) }

    private fun openSwipeDownApp() = openGestureApp(prefs.appSwipeDown) { openDialerApp(requireContext()) }

    private fun openSwipeUpApp() = openGestureApp(prefs.appSwipeUp) { showAppList(AppDrawerFlag.LaunchApp) }

    private fun openClickClockApp() = openGestureApp(prefs.appClickClock) { openAlarmApp(requireContext()) }

    private fun openClickDateApp() = openGestureApp(prefs.appClickDate) { openCalendar(requireContext()) }

    private fun openSwipeLeftApp() = openGestureApp(prefs.appSwipeLeft) { openCameraApp(requireContext()) }

    private fun openDoubleTapApp() = openGestureApp(prefs.appDoubleTap) { openCameraApp(requireContext()) }

    private fun performAction(action: Action, openApp: () -> Unit) {
        if (action == Action.OpenApp) openApp() else handleOtherAction(action)
    }

    // This function handles all actions that are independent of the gesture that triggered them
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp)
            Action.OpenApp -> {} // handled by the gesture specific open*App functions
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.Disabled -> {}
        }
    }

    private fun lockPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actionService = ActionService.instance()
            if (actionService != null) {
                actionService.lockScreen()
            } else {
                openAccessibilitySettings(requireContext())
            }
        } else {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                showToastLong(requireContext(), "App does not have the permission to lock the device")
            } catch (e: Exception) {
                showToastLong(requireContext(), "Olauncher failed to lock device.\nPlease check your app settings.")
                prefs.lockModeOn = false
            }
        }
    }

    private fun showLongPressToast() = showToastShort(requireContext(), "Long press to select app")

    private fun getHomeScreenGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() = performAction(prefs.swipeLeftAction, ::openSwipeLeftApp)

            override fun onSwipeRight() = performAction(prefs.swipeRightAction, ::openSwipeRightApp)

            override fun onSwipeUp() = performAction(prefs.swipeUpAction, ::openSwipeUpApp)

            override fun onSwipeDown() = performAction(prefs.swipeDownAction, ::openSwipeDownApp)

            override fun onDoubleClick() = performAction(prefs.doubleTapAction, ::openDoubleTapApp)

            override fun onLongClick() {
                // Runs after a delay, so the view may already be gone.
                val touchArea = _binding?.touchArea ?: return
                touchArea.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                navigateFromHome(R.id.action_mainFragment_to_settingsFragment)
            }
        }
    }

    private fun getHomeAppsGestureListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onClick(view: View) = homeAppClicked(view.id)

            override fun onLongClick(view: View) = homeAppLongClicked(view)

            override fun onSwipeLeft() = performAction(prefs.swipeLeftAction, ::openSwipeLeftApp)

            override fun onSwipeRight() = performAction(prefs.swipeRightAction, ::openSwipeRightApp)

            override fun onSwipeUp() = performAction(prefs.swipeUpAction, ::openSwipeUpApp)

            override fun onSwipeDown() = performAction(prefs.swipeDownAction, ::openSwipeDownApp)
        }
    }

    // Updates the number of apps on the home screen. Does nothing if the number has not changed.
    private fun updateAppCount(newAppsNum: Int) {
        val layout = binding.homeAppsLayout
        val oldAppsNum = layout.childCount

        if (newAppsNum < oldAppsNum) {
            layout.removeViews(newAppsNum, oldAppsNum - newAppsNum)
        } else if (newAppsNum > oldAppsNum) {
            val alignment = prefs.homeAlignment.value()
            val textSize = prefs.textSize.toFloat()
            val width = if (prefs.extendHomeAppsArea) LinearLayout.LayoutParams.MATCH_PARENT else LinearLayout.LayoutParams.WRAP_CONTENT

            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, layout, false) as TextView
                view.id = i
                view.text = prefs.getHomeAppModel(i).name
                view.textSize = textSize
                view.gravity = alignment
                view.layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
                view.setOnTouchListener(getHomeAppsGestureListener(view.context, view))
                layout.addView(view)
            }
        }
    }
}
