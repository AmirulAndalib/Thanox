package github.tornaco.android.thanox.module.notification.recorder.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.savedstate.SavedStateRegistryOwner
import com.elvishew.xlog.XLog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.miguelcatalan.materialsearchview.MaterialSearchView.SearchViewListener
import github.tornaco.android.thanos.R
import github.tornaco.android.thanos.core.app.ThanosManager
import github.tornaco.android.thanos.databinding.ModuleNotificationRecorderNrdListLayoutBinding
import github.tornaco.android.thanos.theme.ThemeActivity
import github.tornaco.android.thanos.util.ActivityUtils
import github.tornaco.android.thanos.util.ToastUtils
import github.tornaco.android.thanos.widget.ModernProgressDialog
import github.tornaco.android.thanos.widget.SwitchBar
import github.tornaco.android.thanox.module.notification.recorder.NotificationRecordSettingsActivity
import github.tornaco.android.thanox.module.notification.recorder.source.NotificationRecordRepository
import github.tornaco.android.thanox.module.notification.recorder.ui.stats.StatsActivity
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationRecordActivity : ThemeActivity() {
    private lateinit var binding: ModuleNotificationRecorderNrdListLayoutBinding

    val owner: SavedStateRegistryOwner get() = this


    private val model: NotificationRecordViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(owner, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                val repo = NotificationRecordRepository(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return NotificationRecordViewModel(handle, repo) as T
            }
        }
    }

    private lateinit var adapter: NotificationAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ModuleNotificationRecorderNrdListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initAdapter()
        initSwipeToRefresh()
        initSearch()

        onSetupSwitchBar(binding.switchBarContainer.switchBar)

        val progressDialog = ModernProgressDialog(this)
        progressDialog.setTitle(github.tornaco.android.thanos.res.R.string.common_text_wait_a_moment)
        lifecycleScope.launch {
            model.effect.collectLatest {
                when (it) {
                    is Effect.ExportFail -> {
                        progressDialog.dismiss()
                        ToastUtils.nook(this@NotificationRecordActivity)
                        XLog.e(it.err, "ExportFail")
                    }

                    Effect.ExportSuccess -> {
                        progressDialog.dismiss()
                        ToastUtils.ok(this@NotificationRecordActivity)
                    }

                    Effect.Exporting -> {
                        progressDialog.show()
                    }
                }
            }
        }
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(github.tornaco.android.thanos.res.R.string.module_notification_recorder_feature_title_notification_recorder)
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun initAdapter() {
        adapter = NotificationAdapter()
        binding.apps.adapter = adapter

        lifecycleScope.launchWhenCreated {
            model.notifications.collectLatest {
                adapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collect { state ->
                val isLoading = state.mediator?.refresh is LoadState.Loading
                binding.swipe.isRefreshing = isLoading
            }
        }
    }

    private fun initSwipeToRefresh() {
        binding.swipe.setOnRefreshListener { adapter.refresh() }
    }

    private fun onSetupSwitchBar(switchBar: SwitchBar) {
        switchBar.setOnLabel(
            getString(
                github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_notification_recorder_feature_title_notification_recorder)
            )
        )
        switchBar.setOffLabel(
            getString(
                github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_notification_recorder_feature_title_notification_recorder)
            )
        )
        switchBar.isChecked = getSwitchBarCheckState()
        switchBar.addOnSwitchChangeListener { _: MaterialSwitch?, isChecked: Boolean ->
            onSwitchBarCheckChanged(
                isChecked
            )
        }
    }

    private fun getSwitchBarCheckState(): Boolean {
        return (ThanosManager.from(applicationContext).isServiceInstalled
                && ThanosManager.from(applicationContext)
            .notificationManager
            .isPersistOnNewNotificationEnabled)
    }

    private fun onSwitchBarCheckChanged(isChecked: Boolean) {
        ThanosManager.from(applicationContext)
            .notificationManager.isPersistOnNewNotificationEnabled = isChecked
    }

    private fun initSearch() {
        // Search.
        binding.searchView.setOnQueryTextListener(
            object : MaterialSearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    model.load(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    model.load(newText)
                    return true
                }
            })

        binding.searchView.setOnSearchViewListener(
            object : SearchViewListener {
                override fun onSearchViewShown() {
                    binding.toolbarLayout.isTitleEnabled = false
                    binding.appbar.setExpanded(false, true)
                }

                override fun onSearchViewClosed() {
                    model.load("")
                    binding.toolbarLayout.isTitleEnabled = true
                }
            })
    }

    override fun onBackPressed() {
        if (binding.searchView.isSearchOpen) {
            binding.searchView.closeSearch()
            return
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.module_notification_recorder_nr_list, menu)
        val item = menu.findItem(github.tornaco.android.thanos.module.common.R.id.action_search)
        binding.searchView.setMenuItem(item)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_all) {
            MaterialAlertDialogBuilder(this)
                .setTitle(github.tornaco.android.thanos.res.R.string.module_notification_recorder_clear_all)
                .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    model.clearAllRecords(applicationContext)
                    finish()
                }.show()
        }
        if (item.itemId == R.id.action_settings) {
            NotificationRecordSettingsActivity.start(this)
        }
        if (item.itemId == R.id.action_stats) {
            StatsActivity.Starter.start(this)
        }
        if (item.itemId == R.id.action_export) {
            showExportNRDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showExportNRDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(github.tornaco.android.thanos.res.R.string.module_notification_recorder_export)
            .setMessage(github.tornaco.android.thanos.res.R.string.module_notification_recorder_export_dialog_message)

            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int -> }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                model.exportNRs(applicationContext)
            }
            .show()
    }

    companion object Starter {
        fun start(context: Context) {
            ActivityUtils.startActivity(context, NotificationRecordActivity::class.java)
        }
    }
}