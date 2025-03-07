package github.tornaco.thanos.module.component.manager

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elvishew.xlog.XLog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.miguelcatalan.materialsearchview.MaterialSearchView.SearchViewListener

import github.tornaco.android.thanos.core.pm.AppInfo
import github.tornaco.android.thanos.core.util.ClipboardUtils
import github.tornaco.android.thanos.module.common.R
import github.tornaco.android.thanos.theme.ThemeActivity
import github.tornaco.android.thanos.widget.ModernProgressDialog
import github.tornaco.thanos.module.component.manager.databinding.ModuleComponentManagerComponentListActivityBinding
import github.tornaco.thanos.module.component.manager.model.ComponentModel
import kotlin.math.max
import kotlin.math.min

abstract class ComponentListActivity : ThemeActivity() {

    private var appInfo: AppInfo? = null
    private lateinit var viewModel: ComponentListViewModel
    private lateinit var binding: ModuleComponentManagerComponentListActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!internalResolveIntent()) {
            finish()
            return
        }

        binding =
            ModuleComponentManagerComponentListActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupView()
        setupViewModel()
    }

    private fun internalResolveIntent(): Boolean {
        if (intent == null) return false
        appInfo = intent.getParcelableExtra("app")
        if (appInfo == null) return false
        return true
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = appInfo?.appLabel
        title = appInfo?.appLabel
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        // List.
        binding.componentListView.layoutManager = LinearLayoutManager(this)
        binding.componentListView.adapter =
            ComponentListAdapter(/* appInfo = */ appInfo!!,
                /* onComponentItemSwitchChangeListener = */ { componentModel, checked ->
                    viewModel.toggleComponentState(
                        appInfo,
                        componentModel,
                        checked
                    )
                }, /* onComponentItemClickListener = */ { v, componentModel ->
                    showItemPopMenu(v, componentModel)
                }, { v, rule ->
                    showComponentRuleInfoBottomSheet()
                })
        binding.swipe.setOnRefreshListener { viewModel.start() }
        binding.swipe.setColorSchemeColors(
            *resources.getIntArray(
                R.array.common_swipe_refresh_colors
            )
        )

        // Search.
        // Search.
        binding.searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.setSearchText(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Let's wait for submit.
                return true
            }
        })

        binding.searchView.setOnSearchViewListener(object : SearchViewListener {
            override fun onSearchViewShown() { // Noop.
            }

            override fun onSearchViewClosed() {
                viewModel.clearSearchText()
            }
        })
    }

    private fun setupViewModel() {
        viewModel = obtainViewModel(this)
        viewModel.setAppInfo(appInfo)
        viewModel.start()
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
    }

    private fun showItemPopMenu(anchor: View, componentModel: ComponentModel) {
        val popupMenu = onCreateItemPopupMenu(anchor, componentModel)
        popupMenu.show()
    }

    open fun onCreateItemPopupMenu(anchor: View, componentModel: ComponentModel): PopupMenu {
        val popupMenu = PopupMenu(thisActivity(), anchor)
        popupMenu.inflate(github.tornaco.thanos.module.component.manager.R.menu.module_component_manager_component_item_menu)
        popupMenu.setOnMenuItemClickListener(onCreateItemOnMenuItemClickListener(componentModel))
        return popupMenu
    }

    open fun onCreateItemOnMenuItemClickListener(componentModel: ComponentModel): PopupMenu.OnMenuItemClickListener {
        return PopupMenu.OnMenuItemClickListener { item ->
            if (item!!.itemId == github.tornaco.thanos.module.component.manager.R.id.action_copy_name) {
                ClipboardUtils.copyToClipboard(
                    thisActivity(),
                    "component",
                    componentModel.componentName.flattenToString()
                )
                Toast.makeText(
                    thisActivity(),
                    github.tornaco.android.thanos.res.R.string.common_toast_copied_to_clipboard,
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }
    }

    abstract fun obtainViewModel(activity: FragmentActivity): ComponentListViewModel

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        onInflateOptionsMenu(menu)
        val item =
            menu.findItem(github.tornaco.thanos.module.component.manager.R.id.action_component_search)
        binding.searchView.setMenuItem(item)
        return true
    }

    protected open fun onInflateOptionsMenu(menu: Menu?) {
        menuInflater.inflate(
            github.tornaco.thanos.module.component.manager.R.menu.module_component_manager_component_list_menu,
            menu
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (github.tornaco.thanos.module.component.manager.R.id.action_show_feature_desc == item.itemId) {
            showFeatureDesc()
            return true
        }
        if (R.id.action_select_all == item.itemId) {
            MaterialAlertDialogBuilder(thisActivity())
                .setTitle(github.tornaco.android.thanos.res.R.string.common_menu_title_select_all)
                .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    onRequestSelectAll(viewModel.componentModels, true)
                }.show()
            return true
        }
        if (R.id.action_un_select_all == item.itemId) {
            MaterialAlertDialogBuilder(thisActivity())
                .setTitle(github.tornaco.android.thanos.res.R.string.common_menu_title_un_select_all)
                .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> onRequestSelectAll(viewModel.componentModels, false) }
                .show()
            return true
        }
        if (github.tornaco.thanos.module.component.manager.R.id.action_select_all_in_this_page == item.itemId) {
            MaterialAlertDialogBuilder(thisActivity())
                .setTitle(github.tornaco.android.thanos.res.R.string.common_menu_title_select_all_in_this_page)
                .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> onRequestSelectAllInThisPage(binding.componentListView, true) }
                .show()
            return true
        }
        if (github.tornaco.thanos.module.component.manager.R.id.action_un_select_all_in_this_page == item.itemId) {
            MaterialAlertDialogBuilder(thisActivity())
                .setTitle(github.tornaco.android.thanos.res.R.string.common_menu_title_un_select_all_in_this_page)
                .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> onRequestSelectAllInThisPage(binding.componentListView, false) }
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onRequestSelectAllInThisPage(recyclerView: RecyclerView, isSelectAll: Boolean) {
        val models = viewModel.componentModels
        val lm = recyclerView.layoutManager as LinearLayoutManager
        val f = max(0, lm.findFirstVisibleItemPosition())
        val l = min(models.size, lm.findLastVisibleItemPosition())
        XLog.d("onRequestSelectAllInThisPage $f - $l")
        val items = viewModel.componentModels.subList(f, l)
        XLog.d("onRequestSelectAllInThisPage ${models.map { it.componentName.className }}")
        XLog.d("onRequestSelectAllInThisPage ${items.map { it.componentName.className }}")
        onRequestSelectAll(items, isSelectAll)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onRequestSelectAll(modelList: List<ComponentModel>, isSelectAll: Boolean) {
        val progressDialog = ModernProgressDialog(thisActivity())
        progressDialog.setTitle(getString(github.tornaco.android.thanos.res.R.string.common_text_wait_a_moment))
        progressDialog.show()
        viewModel.selectAll(
            appInfo,
            modelList,
            isSelectAll,
            {
                runOnUiThread {
                    XLog.d("onRequestSelectAll, onUpdate: $it")
                    progressDialog.setMessage(it)
                }
            }, {
                runOnUiThread {
                    XLog.d("onRequestSelectAll, onComplete")
                    progressDialog.dismiss()
                    binding.componentListView.adapter?.notifyDataSetChanged()
                }
            })
    }

    private fun showFeatureDesc() {
        MaterialAlertDialogBuilder(thisActivity())
            .setTitle(github.tornaco.android.thanos.res.R.string.module_component_manager_disabled_by_thanox)
            .setMessage(github.tornaco.android.thanos.res.R.string.module_component_manager_feature_desc)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                // Noop.
            }
            .show()
    }

    override fun onBackPressed() {
        if (binding.searchView.isSearchOpen) {
            binding.searchView.closeSearch()
            return
        }
        super.onBackPressed()
    }

    private fun showComponentRuleInfoBottomSheet() {
    }

    @Composable
    private fun RuleInfoScreen() {
        Column {
            Text("Hello")
        }
    }
}