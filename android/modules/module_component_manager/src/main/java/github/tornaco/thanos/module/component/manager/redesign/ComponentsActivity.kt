@file:OptIn(ExperimentalMaterial3Api::class)

package github.tornaco.thanos.module.component.manager.redesign

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import github.tornaco.android.thanos.common.UiState
import github.tornaco.android.thanos.core.app.ThanosManager
import github.tornaco.android.thanos.core.pm.AppInfo
import github.tornaco.android.thanos.core.util.ClipboardUtils
import github.tornaco.android.thanos.module.compose.common.ComposeThemeActivity
import github.tornaco.android.thanos.module.compose.common.theme.TypographyDefaults
import github.tornaco.android.thanos.module.compose.common.widget.AppIcon
import github.tornaco.android.thanos.module.compose.common.widget.ConfirmDialog
import github.tornaco.android.thanos.module.compose.common.widget.DropdownItem
import github.tornaco.android.thanos.module.compose.common.widget.DropdownSelector
import github.tornaco.android.thanos.module.compose.common.widget.MD3Badge
import github.tornaco.android.thanos.module.compose.common.widget.SmallSpacer
import github.tornaco.android.thanos.module.compose.common.widget.ThanoxSmallAppBarScaffold
import github.tornaco.android.thanos.module.compose.common.widget.rememberConfirmDialogState
import github.tornaco.android.thanos.module.compose.common.widget.rememberDropdownSelectorState
import github.tornaco.android.thanos.module.compose.common.widget.rememberSearchBarState
import github.tornaco.android.thanos.util.BrowserUtils
import github.tornaco.android.thanos.util.ToastUtils
import github.tornaco.thanos.module.component.manager.model.ComponentModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@AndroidEntryPoint
class ComponentsActivity : ComposeThemeActivity() {
    companion object {
        private const val EXTRA_APP = "app"
        private const val EXTRA_TYPE = "type"

        @JvmStatic
        fun startActivity(context: Context, appInfo: AppInfo) {
            start(context, appInfo, Type.ACTIVITY)
        }

        @JvmStatic
        fun startService(context: Context, appInfo: AppInfo) {
            start(context, appInfo, Type.SERVICE)
        }

        @JvmStatic
        fun startBroadcastReceiver(context: Context, appInfo: AppInfo) {
            start(context, appInfo, Type.RECEIVER)
        }

        @JvmStatic
        fun startProvider(context: Context, appInfo: AppInfo) {
            start(context, appInfo, Type.PROVIDER)
        }

        private fun start(context: Context, appInfo: AppInfo, type: Type) {
            val starter = Intent(context, ComponentsActivity::class.java).apply {
                putExtra(EXTRA_APP, appInfo)
                putExtra(EXTRA_TYPE, type.name)
            }
            context.startActivity(starter)
        }

        enum class Type {
            ACTIVITY,
            RECEIVER,
            SERVICE,
            PROVIDER
        }
    }

    override fun isADVF(): Boolean {
        return true
    }


    @Composable
    override fun Content() {
        val type = remember {
            requireNotNull(
                Type.valueOf(
                    intent.getStringExtra(EXTRA_TYPE).orEmpty()
                )
            ) { "Type is null." }
        }

        val viewModel = when (type) {
            Type.ACTIVITY -> {
                hiltViewModel<ActivitiesVM>()
            }

            Type.RECEIVER -> {
                hiltViewModel<ReceiversVM>()
            }

            Type.SERVICE -> {
                hiltViewModel<ServicesVM>()
            }

            Type.PROVIDER -> {
                hiltViewModel<ProvidersVM>()
            }
        }

        val app = remember {
            requireNotNull(intent.getParcelableExtra(EXTRA_APP)) { "App info is null." } as AppInfo
        }
        LaunchedEffect(viewModel) {
            viewModel.initApp(app)
        }
        val searchBarState = rememberSearchBarState()

        LaunchedEffect(searchBarState) {
            snapshotFlow { searchBarState.keyword }
                .distinctUntilChanged()
                .collect {
                    viewModel.search(it)
                }
        }

        BackHandler(searchBarState.showSearchBar) {
            viewModel.search("")
            searchBarState.closeSearchBar()
        }

        val selectState by viewModel.selectState.collectAsStateWithLifecycle()
        val batchOpState by viewModel.batchOpState.collectAsStateWithLifecycle()

        ThanoxSmallAppBarScaffold(
            title = {
                AnimatedContent(selectState.isSelectMode) {
                    if (it) {
                        Text(
                            text = "${stringResource(github.tornaco.android.thanos.res.R.string.common_menu_title_batch_select)} ${selectState.selectedItems.size}",
                            style = TypographyDefaults.appBarTitleTextStyle()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(modifier = Modifier.size(32.dp), appInfo = app)
                            SmallSpacer()
                            Text(
                                text = app.appLabel,
                                style = TypographyDefaults.appBarTitleTextStyle()
                            )
                        }
                    }
                }
            },
            searchBarState = searchBarState,
            onBackPressed = {
                if (selectState.isSelectMode) {
                    viewModel.exitSelectionState()
                } else {
                    thisActivity().finish()
                }
            },
            actions = {
                AnimatedContent(selectState.isSelectMode) {
                    if (it) {
                        Row {
                            TextButton(onClick = {
                                viewModel.appBatchOp(true)
                            }) {
                                Text(text = stringResource(github.tornaco.android.thanos.res.R.string.on))
                            }
                            TextButton(onClick = {
                                viewModel.appBatchOp(false)
                            }) {
                                Text(text = stringResource(github.tornaco.android.thanos.res.R.string.off))
                            }
                        }
                    } else {
                        Row {
                            val filterDropdownState = rememberDropdownSelectorState()
                            Box {
                                IconButton(onClick = {
                                    filterDropdownState.open()
                                }) {
                                    Icon(
                                        painterResource(github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_filter_fill),
                                        contentDescription = "Filter"
                                    )
                                }

                                DropdownSelector(
                                    state = filterDropdownState,
                                    items = listOf(
                                        DropdownItem(
                                            data = FilterState.All,
                                            labelLines = listOf(stringResource(github.tornaco.android.thanos.res.R.string.all))
                                        ),
                                        DropdownItem(
                                            data = FilterState.Enabled,
                                            labelLines = listOf(stringResource(github.tornaco.android.thanos.res.R.string.enabled))
                                        ),
                                        DropdownItem(
                                            data = FilterState.Disabled,
                                            labelLines = listOf(stringResource(github.tornaco.android.thanos.res.R.string.disabled))
                                        )
                                    )
                                ) {
                                    viewModel.setFilter(it.data)
                                }
                            }

                            IconButton(onClick = {
                                searchBarState.showSearchBar()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                    }
                }
            }
        ) { paddings ->
            val components by viewModel.components.collectAsStateWithLifecycle()
            val refreshing = components is UiState.Loading
            val refreshState = rememberPullRefreshState(refreshing, onRefresh = {
                viewModel.refresh()
            })

            if (batchOpState.isWorking) {
                BasicAlertDialog(
                    onDismissRequest = {
                    },
                    content = {
                        Surface(
                            modifier = Modifier
                                .wrapContentWidth()
                                .wrapContentHeight(),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = AlertDialogDefaults.TonalElevation
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(text = batchOpState.progressText)
                                LinearProgressIndicator()
                            }
                        }
                    },
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddings)
                    .pullRefresh(refreshState)
            ) {
                val collapsedGroups by viewModel.collapsedGroups.collectAsStateWithLifecycle()
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (components) {
                        is UiState.Error -> {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text((components as UiState.Error).err.stackTraceToString())
                                }
                            }
                        }

                        is UiState.Loaded -> {
                            (components as UiState.Loaded<List<ComponentGroup>>).data.forEach { group ->
                                val isLargeGroup = group.components.size > 100

                                stickyHeader {
                                    RuleHeader(
                                        group = group,
                                        canExpand = !isLargeGroup,
                                        isExpand = !collapsedGroups.contains(group.id),
                                        expand = {
                                            viewModel.expand(group, it)
                                        },
                                        isInSelectMode = selectState.isSelectMode,
                                        isGroupSelected = selectState.selectedItems.containsAll(
                                            group.components
                                        ),
                                        updateGroupSelection = { componentGroup, b ->
                                            viewModel.select(componentGroup, b)
                                        }
                                    )
                                }

                                if (!isLargeGroup) {
                                    item {
                                        AnimatedVisibility(!collapsedGroups.contains(group.id)) {
                                            LazyColumn(Modifier.heightIn(max = 2000.dp)) {
                                                itemsIndexed(group.components, key = { index, com ->
                                                    "${com.name}-${index}"
                                                }) { _, component ->
                                                    ComponentItem(
                                                        component = component,
                                                        type = type,
                                                        toggle = {
                                                            viewModel.setComponentState(
                                                                componentModel = it,
                                                                setToEnabled = it.isDisabled
                                                            )
                                                        },
                                                        isInSelectMode = selectState.isSelectMode,
                                                        isSelected = selectState.selectedItems.contains(
                                                            component
                                                        ),
                                                        updateSelection = { componentModel: ComponentModel, b: Boolean ->
                                                            viewModel.select(componentModel, b)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Large group, always show.
                                    itemsIndexed(group.components, key = { index, com ->
                                        "${com.name}-${index}"
                                    }) { _, component ->
                                        ComponentItem(
                                            component = component,
                                            type = type,
                                            toggle = {
                                                viewModel.setComponentState(
                                                    componentModel = it,
                                                    setToEnabled = it.isDisabled
                                                )
                                            },
                                            isInSelectMode = selectState.isSelectMode,
                                            isSelected = selectState.selectedItems.contains(
                                                component
                                            ),
                                            updateSelection = { componentModel: ComponentModel, b: Boolean ->
                                                viewModel.select(componentModel, b)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        UiState.Loading -> {
                            // No op.
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = refreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            }

        }
    }

    @Composable
    private fun ComponentItem(
        component: ComponentModel,
        type: Type,
        isInSelectMode: Boolean,
        isSelected: Boolean,
        updateSelection: (ComponentModel, Boolean) -> Unit,
        toggle: (ComponentModel) -> Unit,
    ) {
        val fullName = remember { component.componentName.flattenToString() }
        val context = LocalContext.current
        val dropdownState = rememberDropdownSelectorState()

        val addToSmartStandByKeepsVarDialog = rememberConfirmDialogState()
        ConfirmDialog(
            title = stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_keep_service_smart_standby),
            state = addToSmartStandByKeepsVarDialog,
            data = "KEEP $fullName",
            messageHint = { it },
            onConfirm = {
                ThanosManager.from(context).activityManager.addStandbyRule(it)
                ToastUtils.ok(context)
            }
        )

        val addToAppLockAllowDialog = rememberConfirmDialogState()
        ConfirmDialog(
            title = stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_add_lock_white_list),
            state = addToAppLockAllowDialog,
            data = component.componentName,
            messageHint = { it.flattenToString() },
            onConfirm = {
                ThanosManager.from(context).activityStackSupervisor.addAppLockWhiteListComponents(
                    listOf(it)
                )
                ToastUtils.ok(context)
            }
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .combinedClickable(onClick = {
                        if (isInSelectMode) {
                            updateSelection(component, !isSelected)
                        } else {
                            dropdownState.open()
                        }
                    }, onLongClick = {
                        updateSelection(component, true)
                    })
                    .then(
                        if (isInSelectMode && isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var isChecked by remember(component) { mutableStateOf(!component.isDisabled) }
                Row(
                    modifier = Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row {
                            Text(
                                component.label,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (!isChecked && component.isDisabledByThanox) {
                                MD3Badge(stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_disabled_by_thanox))
                            }
                            if (component.isRunning) {
                                MD3Badge(stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_component_running))
                            }
                        }
                        Text(
                            component.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Switch(checked = isChecked, onCheckedChange = {
                    isChecked = !isChecked
                    toggle(component)
                })
            }

            DropdownSelector(state = dropdownState, items = listOfNotNull(
                DropdownItem(
                    labelLines = listOf(stringResource(android.R.string.copy)),
                    data = ComponentItemAction.Copy,
                ),
                if (type == Type.SERVICE) {
                    DropdownItem(
                        labelLines = listOf(stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_keep_service_smart_standby)),
                        data = ComponentItemAction.AddToSmartStandByKeepRules,
                    )
                } else null,
                if (type == Type.ACTIVITY) {
                    DropdownItem(
                        labelLines = listOf(stringResource(github.tornaco.android.thanos.res.R.string.module_component_manager_add_lock_white_list)),
                        data = ComponentItemAction.AddToAppLockAllowList,
                    )
                } else null
            ), onSelect = {
                when (it.data) {
                    ComponentItemAction.Copy -> {
                        ClipboardUtils.copyToClipboard(
                            context,
                            "Name",
                            fullName
                        )
                    }

                    ComponentItemAction.AddToSmartStandByKeepRules -> {
                        addToSmartStandByKeepsVarDialog.show()
                    }

                    ComponentItemAction.AddToAppLockAllowList -> {
                        addToAppLockAllowDialog.show()
                    }
                }
            })
        }
    }

    @Composable
    private fun RuleHeader(
        group: ComponentGroup,
        canExpand: Boolean,
        isExpand: Boolean,
        expand: (Boolean) -> Unit,
        isInSelectMode: Boolean,
        isGroupSelected: Boolean,
        updateGroupSelection: (ComponentGroup, Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    if (isInSelectMode) {
                        updateGroupSelection(group, !isGroupSelected)
                    } else {
                        if (canExpand) expand(!isExpand)
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(group.rule.iconRes.takeIf { it > 0 }
                        ?: github.tornaco.android.thanos.res.R.drawable.ic_logo_android_line),
                    contentDescription = null,
                    tint = if (group.rule.isSimpleColorIcon) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Unspecified
                    }
                )
                SmallSpacer()
                Text(
                    text = "${group.rule.label} (${group.components.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                group.rule.descriptionUrl?.let {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        BrowserUtils.launch(context, it)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info"
                        )
                    }
                }
            }

            if (canExpand) IconButton(onClick = {
                expand(!isExpand)
            }) {
                val rotate by animateFloatAsState(if (isExpand) 180f else 0f)
                Icon(
                    modifier = Modifier.rotate(rotate),
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Info"
                )
            }
        }
    }
}

internal enum class ComponentItemAction {
    Copy, AddToSmartStandByKeepRules, AddToAppLockAllowList
}