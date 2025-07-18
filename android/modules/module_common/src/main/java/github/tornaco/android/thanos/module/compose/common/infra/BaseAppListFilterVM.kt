package github.tornaco.android.thanos.module.compose.common.infra

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import github.tornaco.android.thanos.common.AppLabelSearchFilter
import github.tornaco.android.thanos.core.app.ThanosManager
import github.tornaco.android.thanos.core.pm.PREBUILT_PACKAGE_SET_ID_3RD
import github.tornaco.android.thanos.module.compose.common.infra.AppListUiState.Companion.filterOptionsAll
import github.tornaco.android.thanos.module.compose.common.infra.sort.AppSortTools
import github.tornaco.android.thanos.module.compose.common.loader.AppSetFilterItem
import github.tornaco.android.thanos.module.compose.common.loader.Loader
import github.tornaco.android.thanos.res.R
import github.tornaco.android.thanos.theme.CommonAppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppListUiState(
    val apps: List<AppUiModel> = emptyList(),
    val isLoading: Boolean = false,

    val appFilterItems: List<AppSetFilterItem> = emptyList(),
    val selectedAppSetFilterItem: AppSetFilterItem? = null,

    val optionsFilterItems: List<AppSetFilterItem> = emptyList(),
    val selectedOptionFilterItem: AppSetFilterItem? = filterOptionsAll,

    val appSort: AppSortTools = AppSortTools.Default,
    val sortReverse: Boolean = false,
    val sortToolItems: List<AppSortTools> = emptyList(),

    val searchKeyword: String = "",

    val isInSelectionMode: Boolean = false,
    val selectedAppItems: List<AppUiModel> = emptyList()
) {
    companion object {
        val filterOptionsAll = AppSetFilterItem("") { it.getString(R.string.all) }
    }
}

@HiltViewModel
class BaseAppListFilterVM @Inject constructor(@ApplicationContext private val context: Context) :
    StateViewModel<AppListUiState>(initState = { AppListUiState() }) {
    private var config: BaseAppListFilterContainerConfig = BaseAppListFilterContainerConfig(
        appBarConfig = AppBarConfig(title = { "" }),
        featureId = "",
        appItemConfig = AppItemConfig(
            itemType = AppItemConfig.ItemType.Plain(onAppClick = {}),
            loader = { _, _ -> emptyList() }
        )
    )

    private val thanox by lazy { ThanosManager.from(context) }

    private val appLabelSearchFilter by lazy { AppLabelSearchFilter() }
    private val searchJobs: MutableList<Job> = mutableListOf()

    fun installIn(config: BaseAppListFilterContainerConfig) {
        this.config = config
        viewModelScope.launch {
            loadDefaultAppFilterItems()
            loadOptionsFilterItems()
            loadSortToolItems()
            loadApps("installIn")
        }
    }

    private suspend fun loadApps(reason: String) {
        XLog.d("loadApps: $reason")
        updateState { copy(isLoading = true) }
        withContext(Dispatchers.IO) {
            with(config) {
                val searchKeyword = state.value.searchKeyword.takeIf { it.isNotBlank() }.orEmpty()
                val sort = state.value.appSort
                val sortReverse = state.value.sortReverse

                val selectedOptionFilter = state.value.selectedOptionFilterItem ?: filterOptionsAll

                val appModels = appItemConfig.loader(
                    context,
                    state.value.selectedAppSetFilterItem?.id ?: PREBUILT_PACKAGE_SET_ID_3RD
                ).filter {
                    searchKeyword.isEmpty() || (searchKeyword.length > 2 && it.appInfo.pkgName.contains(
                        searchKeyword
                    )) || appLabelSearchFilter.matches(searchKeyword, it.appInfo.appLabel)
                }.filter {
                    selectedOptionFilter == filterOptionsAll || it.selectedOptionId == selectedOptionFilter.id
                }.let {
                    val appSorterProvider = sort.provider
                    val sortApplied =
                        it.sortedWith(appSorterProvider.comparator(context))
                    if (sortReverse) {
                        sortApplied.reversed()
                    } else {
                        sortApplied
                    }
                }.map { model ->
                    val appSorterProvider = sort.provider
                    val appSortDescription = appSorterProvider.getAppSortDescription(
                        context,
                        model
                    )
                    if (appSortDescription.isNullOrEmpty()) {
                        model
                    } else {
                        model.copy(
                            description = model.description?.let {
                                it + System.lineSeparator() + appSortDescription
                            } ?: appSortDescription
                        )
                    }
                }.let {
                    if (sort.relyOnUsageStats()) {
                        inflateAppUsageStats(it)
                    } else {
                        it
                    }
                }
                updateState {
                    copy(apps = appModels, isLoading = false)
                }
            }
        }
    }

    fun onFilterItemSelected(appSetFilterItem: AppSetFilterItem) {
        CommonAppPrefs.getInstance()
            .setFeatureAppFilterId(context, config.featureId, appSetFilterItem.id)
        updateState {
            copy(selectedAppSetFilterItem = appSetFilterItem)
        }
        refresh("onFilterItemSelected")
    }

    fun onOptionFilterItemSelected(appSetFilterItem: AppSetFilterItem) {
        updateState {
            copy(selectedOptionFilterItem = appSetFilterItem)
        }
        refresh("onOptionFilterItemSelected")
    }

    private suspend fun loadDefaultAppFilterItems() {
        val appFilterListItems = Loader.loadAllFromAppSet(context)
        val filterPref = CommonAppPrefs.getInstance()
            .getFeatureAppFilterId(context, config.featureId, PREBUILT_PACKAGE_SET_ID_3RD)
        updateState {
            copy(
                // Default select 3-rd
                selectedAppSetFilterItem = appFilterListItems.find {
                    it.id == filterPref
                },
                appFilterItems = appFilterListItems
            )
        }
    }

    private fun loadOptionsFilterItems() {
        val itemType = config.appItemConfig.itemType
        val extraItems: List<AppSetFilterItem> = when (itemType) {
            is AppItemConfig.ItemType.OptionSelectable -> {
                itemType.options.map { op ->
                    AppSetFilterItem(op.id) { op.title(it) }
                }.toMutableList().apply {
                    add(filterOptionsAll)
                }
            }

            else -> {
                emptyList()
            }
        }
        updateState {
            copy(
                selectedOptionFilterItem = filterOptionsAll,
                optionsFilterItems = extraItems
            )
        }
    }

    private fun loadSortToolItems() {
        val items = when (config.appItemConfig.itemType) {
            is AppItemConfig.ItemType.Plain -> {
                AppSortTools.entries.filter {
                    it != AppSortTools.CheckState && it != AppSortTools.OptionState
                }
            }

            is AppItemConfig.ItemType.Checkable -> {
                AppSortTools.entries.filter {
                    it != AppSortTools.OptionState
                }
            }

            is AppItemConfig.ItemType.OptionSelectable -> {
                AppSortTools.entries.filter {
                    it != AppSortTools.CheckState
                }
            }
        }

        val prefSort = CommonAppPrefs.getInstance()
            .getFeatureSortSelection(context, config.featureId, AppSortTools.Default)

        updateState {
            copy(sortToolItems = items, appSort = prefSort)
        }
    }

    private fun inflateAppUsageStats(res: List<AppUiModel>): List<AppUiModel> {
        val statsMap =
            thanox.usageStatsManager.queryAndAggregateUsageStats(0, System.currentTimeMillis())
        return res.map { app ->
            if (statsMap.containsKey(app.appInfo.pkgName)) {
                val stats = statsMap[app.appInfo.pkgName]
                if (stats != null) {
                    app.copy(
                        lastUsedTimeMills = stats.lastTimeUsed,
                        totalUsedTimeMills = stats.totalTimeInForeground
                    )
                } else app
            } else {
                app
            }
        }
    }

    fun refresh(reason: String) {
        viewModelScope.launch { loadApps(reason) }
    }

    fun keywordChanged(it: String) {
        updateState { copy(searchKeyword = it) }
        searchJobs.forEach { it.cancel() }
        searchJobs.clear()
        searchJobs.add(
            viewModelScope.launch {
                loadApps("keywordChanged-$it")
            }
        )
    }

    fun updateSort(sort: AppSortTools) {
        CommonAppPrefs.getInstance().setFeatureSortSelection(context, config.featureId, sort)
        updateState { copy(appSort = sort) }
        refresh("updateSort")
    }

    fun updateSortReverse(reverse: Boolean) {
        updateState { copy(sortReverse = reverse) }
        refresh("updateSortReverse")
    }

    fun updateAppCheckState(app: AppUiModel, checked: Boolean) {
        updateState {
            copy(apps = apps.toMutableList().apply {
                val index =
                    indexOfFirst { app.appInfo.pkgName == it.appInfo.pkgName && app.appInfo.userId == it.appInfo.userId }
                if (index >= 0) {
                    set(index, get(index).copy(isChecked = checked))
                }
            })
        }
    }

    fun updateAppOptionState(app: AppUiModel, id: String) {
        updateState {
            copy(apps = apps.toMutableList().apply {
                val index =
                    indexOfFirst { app.appInfo.pkgName == it.appInfo.pkgName && app.appInfo.userId == it.appInfo.userId }
                if (index >= 0) {
                    set(index, get(index).copy(selectedOptionId = id))
                }
            })
        }
    }

    fun enterSelectionMode(isEnter: Boolean) {
        updateState {
            copy(
                isInSelectionMode = isEnter
            )
        }
        if (!isEnter) {
            updateState {
                copy(selectedAppItems = emptyList())
            }
        }
    }

    fun selectApp(select: Boolean, app: AppUiModel) {
        updateState {
            copy(
                selectedAppItems = selectedAppItems.toMutableList().apply {
                    if (select) {
                        add(app)
                    } else {
                        remove(app)
                    }
                }
            )
        }

        enterSelectionMode(state.value.selectedAppItems.isNotEmpty())
    }

    fun selectAll() {
        updateState {
            copy(selectedAppItems = apps.toList())
        }
    }

    fun unSelectAll() {
        updateState { copy(selectedAppItems = emptyList()) }
    }
}