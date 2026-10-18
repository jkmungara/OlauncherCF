package app.olaunchercf.ui

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.olaunchercf.MainViewModel
import app.olaunchercf.R
import app.olaunchercf.data.AppModel
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.FragmentAppDrawerBinding
import app.olaunchercf.helper.openAppInfo

private const val KEYBOARD_DELAY_MS = 100L

class AppDrawerFragment : Fragment() {

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var appAdapter: AppDrawerAdapter

    private var flag = AppDrawerFlag.LaunchApp

    // Set once an app is picked. Taps that land while the closing animation runs are ignored,
    // instead of launching twice or popping the back stack twice (which left a blank screen).
    private var closing = false

    private var showKeyboardRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())

        if (prefs.firstOpen()) {
            binding.appDrawerTip.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flagName = arguments?.getString("flag")
        flag = AppDrawerFlag.values().firstOrNull { it.name == flagName } ?: AppDrawerFlag.LaunchApp
        val n = arguments?.getInt("n", 0) ?: 0
        val showHiddenApps = arguments?.getBoolean("showHidden", false) ?: false

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupDrawerButton(n)

        val gravity = prefs.drawerAlignment.value()

        appAdapter = AppDrawerAdapter(
            flag = flag,
            gravity = gravity,
            textSize = prefs.textSize.toFloat(),
            autoOpenApp = prefs.autoOpenApp,
            clickListener = { app -> onAppSelected(app, n) },
            appInfoListener = ::onAppInfo,
            appHideListener = ::onAppHiddenChanged,
            appRenameListener = { appPackage, alias -> viewModel.setAppAlias(appPackage, alias) },
        )

        searchTextView()?.gravity = gravity

        setupRecyclerView()
        observeApps()
        setupSearch()

        if (flag == AppDrawerFlag.HiddenApps) {
            binding.search.queryHint = getString(R.string.hidden_apps)
            viewModel.getHiddenApps()
        } else {
            viewModel.getAppList(showHiddenApps)
        }
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? =
        createLayeredAnimator(nextAnim)

    override fun onStart() {
        super.onStart()
        showKeyboard()
    }

    override fun onStop() {
        hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        cancelPendingKeyboard()
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupDrawerButton(n: Int) {
        if (flag == AppDrawerFlag.SetHomeApp) {
            binding.drawerButton.text = getString(R.string.rename)
            binding.drawerButton.isVisible = true
            binding.drawerButton.setOnClickListener { renameHomeApp(n) }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
            setHasFixedSize(true)

            itemAnimator = DefaultItemAnimator().apply {
                supportsChangeAnimations = false
                moveDuration = 0 // no jumpy upward movement while typing
                addDuration = 100
                removeDuration = 80
            }

            addOnScrollListener(getRecyclerViewOnScrollListener())
        }
    }

    private fun observeApps() {
        val source = if (flag == AppDrawerFlag.HiddenApps) viewModel.hiddenApps else viewModel.appList

        source.observe(viewLifecycleOwner) { apps ->
            if (apps == null) return@observe

            binding.listEmptyHint.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            appAdapter.submitApps(apps)
        }
    }

    private fun setupSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                appAdapter.launchFirstInList()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter coalesces requests on its own worker thread, no extra debounce needed.
                appAdapter.search(newText.orEmpty())
                return true
            }
        })
    }

    private fun searchTextView(): TextView? = _binding?.search?.findViewById(R.id.search_src_text)

    private fun showKeyboard() {
        if (!prefs.autoShowKeyboard) return

        val searchTextView = searchTextView() ?: return
        searchTextView.requestFocus()

        cancelPendingKeyboard()
        val runnable = Runnable {
            if (searchTextView.isAttachedToWindow) {
                searchTextView.requestFocus()
                val imm = searchTextView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // An explicit request; SHOW_FORCED could leave the keyboard up after the drawer closed.
                imm.showSoftInput(searchTextView, 0)
            }
        }

        showKeyboardRunnable = runnable
        searchTextView.postDelayed(runnable, KEYBOARD_DELAY_MS)
    }

    private fun hideKeyboard() {
        cancelPendingKeyboard()

        val search = _binding?.search ?: return
        search.clearFocus()

        val imm = search.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(search.windowToken, 0)
    }

    private fun cancelPendingKeyboard() {
        val runnable = showKeyboardRunnable ?: return
        _binding?.search?.removeCallbacks(runnable)
        searchTextView()?.removeCallbacks(runnable)
        showKeyboardRunnable = null
    }

    private fun onAppSelected(appModel: AppModel, n: Int) {
        if (closing || !isAdded || _binding == null) return
        closing = true

        viewModel.selectedApp(appModel, flag, n)

        if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps) {
            popToHome()
        } else {
            closeDrawer()
        }
    }

    private fun onAppInfo(appModel: AppModel) {
        if (closing || !isAdded) return
        closing = true

        openAppInfo(requireContext(), appModel)
        popToHome()
    }

    private fun onAppHiddenChanged(drawerFlag: AppDrawerFlag, appModel: AppModel) {
        val hidden = prefs.hiddenApps

        if (drawerFlag == AppDrawerFlag.HiddenApps) {
            hidden.remove(appModel.appPackage) // entry written by old versions
            hidden.remove(appModel.hiddenKey)
        } else {
            hidden.add(appModel.hiddenKey)
        }

        prefs.hiddenApps = hidden
        viewModel.onHiddenAppsChanged()

        if (hidden.isEmpty() && drawerFlag == AppDrawerFlag.HiddenApps) {
            closeDrawer()
        }
    }

    private fun renameHomeApp(i: Int) {
        val name = binding.search.query.toString().trim()
        if (name.isEmpty()) return

        prefs.setHomeAppName(i, name)
        closeDrawer()
    }

    /** Pops this screen, but only while it is still the current one. */
    private fun closeDrawer() {
        if (!isAdded) return

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.appListFragment) {
            navController.popBackStack()
        }
    }

    private fun popToHome() {
        if (!isAdded) return
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)

                        if (onTop) {
                            hideKeyboard()
                        }

                        if (onTop && !recyclerView.canScrollVertically(1)) {
                            closeDrawer()
                        }
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) {
                                closeDrawer()
                            } else {
                                showKeyboard()
                            }
                        }
                    }
                }
            }
        }
    }
}
