package app.olaunchercf.ui

import android.graphics.drawable.Drawable
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.olaunchercf.R
import app.olaunchercf.data.AppModel
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.databinding.AdapterAppDrawerBinding
import app.olaunchercf.helper.dp2px
import app.olaunchercf.helper.uninstallApp
import java.text.Normalizer
import java.util.Locale

// Compiled once, not per app per keystroke.
private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
private val SEPARATORS = Regex("[-_+,. ]")

private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(SEPARATORS, "")
        .lowercase(Locale.getDefault())

private fun AppModel.isSameApp(other: AppModel): Boolean =
    appPackage == other.appPackage && appActivityName == other.appActivityName && user == other.user

private object AppDiffCallback : DiffUtil.ItemCallback<AppModel>() {
    override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean = oldItem.isSameApp(newItem)

    // AppModel is a data class, so this compares label, alias, package, activity and user.
    override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean = oldItem == newItem
}

class AppDrawerAdapter(
    private val flag: AppDrawerFlag,
    private val gravity: Int,
    private val textSize: Float,
    private val autoOpenApp: Boolean,
    private val clickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    private val appRenameListener: (String, String) -> Unit,
) : ListAdapter<AppModel, AppDrawerAdapter.ViewHolder>(AppDiffCallback), Filterable {

    /** Search keys for one app. */
    private class IndexedApp(val app: AppModel, val lowercase: String, val normalized: String)

    /** Search keys for a whole list, tied to the list they were built from. */
    private class SearchIndex(val source: List<AppModel>) {
        private val entries = source.map { app ->
            IndexedApp(app, app.name.lowercase(Locale.getDefault()), normalize(app.name))
        }

        fun search(query: String): List<AppModel> {
            val queryLowercase = query.lowercase(Locale.getDefault())
            val queryNormalized = normalize(query)

            return entries
                .filter { entry ->
                    entry.lowercase.contains(queryLowercase) ||
                            (queryNormalized.isNotEmpty() && entry.normalized.contains(queryNormalized))
                }
                .map { it.app }
        }
    }

    private val hideDrawableRes: Int =
        if (flag == AppDrawerFlag.HiddenApps) R.drawable.visibility else R.drawable.visibility_off

    private val myUser = Process.myUserHandle()

    // Written on the main thread, read on the filter thread.
    @Volatile
    private var sourceApps: List<AppModel> = emptyList()

    // Built lazily on the filter thread, the first time a non-empty query needs it.
    @Volatile
    private var searchIndex: SearchIndex? = null

    /** Last query asked for. Re-applied when the source list changes. */
    private var requestedQuery: String = ""
    private var lastAutoOpenedQuery: String = ""

    private val appFilter = createAppFilter()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getFilter(): Filter = appFilter

    fun submitApps(apps: List<AppModel>) {
        sourceApps = apps
        appFilter.filter(requestedQuery)
    }

    fun search(query: String) {
        requestedQuery = query.trim()
        appFilter.filter(requestedQuery)
    }

    fun launchFirstInList() {
        currentList.firstOrNull()?.let(clickListener)
    }

    /** Drops an app right away, without waiting for the new list from the view model. */
    private fun removeApp(appModel: AppModel) {
        sourceApps = sourceApps.filterNot { it.isSameApp(appModel) }
        appFilter.filter(requestedQuery)
    }

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim().orEmpty()
                val source = sourceApps

                val filtered = if (query.isEmpty()) {
                    source
                } else {
                    val index = searchIndex?.takeIf { it.source === source }
                        ?: SearchIndex(source).also { searchIndex = it }
                    index.search(query)
                }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val query = constraint?.toString()?.trim().orEmpty()
                val filtered = results?.values as? List<AppModel> ?: emptyList()

                if (filtered.size != 1) {
                    lastAutoOpenedQuery = ""
                }

                submitList(filtered) {
                    autoOpenIfSingleMatch(query, filtered)
                }
            }
        }
    }

    private fun autoOpenIfSingleMatch(query: String, filtered: List<AppModel>) {
        if (flag != AppDrawerFlag.LaunchApp) return
        if (!autoOpenApp) return
        if (query.isEmpty()) return
        if (query != requestedQuery) return // the user has typed on since
        if (filtered.size != 1) return
        if (lastAutoOpenedQuery == query) return

        lastAutoOpenedQuery = query
        clickListener(filtered[0])
    }

    inner class ViewHolder(private val binding: AdapterAppDrawerBinding) : RecyclerView.ViewHolder(binding.root) {

        private var appModel: AppModel? = null
        private var workProfileIcon: Drawable? = null

        init {
            // Everything that does not depend on the bound app is set up once per holder,
            // so binding during a fling only sets text.
            binding.appTitle.textSize = textSize
            binding.appTitle.compoundDrawablePadding = 20

            // Mutated before the first layout pass, so no extra requestLayout is needed.
            (binding.appTitle.layoutParams as FrameLayout.LayoutParams).gravity = gravity

            // A fresh drawable per holder: ImageView writes its bounds into the drawable.
            binding.appHide.setImageDrawable(AppCompatResources.getDrawable(itemView.context, hideDrawableRes))

            binding.appTitleFrame.setOnClickListener { appModel?.let(clickListener) }
            binding.appTitleFrame.setOnLongClickListener {
                showOptions()
                true
            }

            binding.appHideLayout.setOnClickListener { binding.appHideLayout.visibility = View.GONE }

            binding.appInfo.setOnClickListener { appModel?.let(appInfoListener) }
            binding.appInfo.setOnLongClickListener {
                appModel?.let { uninstallApp(itemView.context, it.appPackage) }
                true
            }

            binding.appHide.setOnClickListener {
                val app = appModel ?: return@setOnClickListener
                removeApp(app)
                appHideListener(flag, app)
            }

            binding.appRename.setOnClickListener { renameApp() }
            binding.appRenameEdit.setOnEditorActionListener { _, actionId, event ->
                val enterPressed = event != null &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        event.keyCode == KeyEvent.KEYCODE_ENTER

                if (actionId == EditorInfo.IME_ACTION_DONE || enterPressed) {
                    renameApp()
                    true
                } else {
                    false
                }
            }

            binding.appRenameEdit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val app = appModel ?: return
                    val text = s?.toString().orEmpty()

                    binding.appRename.setText(
                        when {
                            text.isEmpty() -> R.string.reset
                            text == app.appAlias || text == app.appLabel -> R.string.cancel
                            else -> R.string.rename
                        }
                    )
                }
            })
        }

        fun bind(app: AppModel) {
            appModel = app
            binding.appHideLayout.visibility = View.GONE
            binding.appTitle.text = app.name

            val icon = if (app.user != myUser) getWorkProfileIcon() else null

            if (gravity == Gravity.LEFT) {
                binding.appTitle.setCompoundDrawables(null, null, icon, null)
            } else {
                binding.appTitle.setCompoundDrawables(icon, null, null, null)
            }
        }

        private fun getWorkProfileIcon(): Drawable? {
            workProfileIcon?.let { return it }

            val context = itemView.context
            val size = dp2px(context.resources, textSize.toInt())
            return AppCompatResources.getDrawable(context, R.drawable.work_profile)?.also {
                it.setBounds(0, 0, size, size)
                workProfileIcon = it
            }
        }

        private fun showOptions() {
            val app = appModel ?: return
            // Only filled in when the options are actually opened, not on every bind.
            binding.appRenameEdit.setText(app.name)
            binding.appHideLayout.visibility = View.VISIBLE
        }

        private fun renameApp() {
            val app = appModel ?: return
            val name = binding.appRenameEdit.text.toString().trim()

            binding.appTitle.text = name.ifEmpty { app.appLabel }
            binding.appHideLayout.visibility = View.GONE
            appRenameListener(app.appPackage, name)
        }
    }
}
