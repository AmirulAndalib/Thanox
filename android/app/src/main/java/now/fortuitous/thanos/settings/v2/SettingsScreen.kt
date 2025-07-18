@file:OptIn(ExperimentalMaterial3Api::class)

package now.fortuitous.thanos.settings.v2

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elvishew.xlog.XLog
import github.tornaco.android.thanos.BuildProp
import github.tornaco.android.thanos.common.CommonPreferences
import github.tornaco.android.thanos.core.app.ThanosManager
import github.tornaco.android.thanos.core.pm.AppInfo
import github.tornaco.android.thanos.core.profile.ConfigTemplate
import github.tornaco.android.thanos.core.profile.ProfileManager
import github.tornaco.android.thanos.core.util.ClipboardUtils
import github.tornaco.android.thanos.core.util.DateUtils
import github.tornaco.android.thanos.module.compose.common.LocalSimpleStorageHelper
import github.tornaco.android.thanos.module.compose.common.REQUEST_CODE_CREATE_BACKUP
import github.tornaco.android.thanos.module.compose.common.REQUEST_CODE_CREATE_LOG
import github.tornaco.android.thanos.module.compose.common.infra.Pref
import github.tornaco.android.thanos.module.compose.common.settings.Preference
import github.tornaco.android.thanos.module.compose.common.settings.PreferenceUi
import github.tornaco.android.thanos.module.compose.common.theme.ThemeSettings
import github.tornaco.android.thanos.module.compose.common.widget.ConfirmDialog
import github.tornaco.android.thanos.module.compose.common.widget.LargeSpacer
import github.tornaco.android.thanos.module.compose.common.widget.LinkText
import github.tornaco.android.thanos.module.compose.common.widget.LottieLoadingView
import github.tornaco.android.thanos.module.compose.common.widget.MenuDialog
import github.tornaco.android.thanos.module.compose.common.widget.MenuDialogItem
import github.tornaco.android.thanos.module.compose.common.widget.SmallSpacer
import github.tornaco.android.thanos.module.compose.common.widget.StandardSpacer
import github.tornaco.android.thanos.module.compose.common.widget.TextInputDialog
import github.tornaco.android.thanos.module.compose.common.widget.rememberConfirmDialogState
import github.tornaco.android.thanos.module.compose.common.widget.rememberMenuDialogState
import github.tornaco.android.thanos.module.compose.common.widget.rememberTextInputState
import github.tornaco.android.thanos.module.compose.common.widget.rememberThanoxBottomSheetState
import github.tornaco.android.thanos.res.R
import github.tornaco.android.thanos.support.AppFeatureManager
import github.tornaco.android.thanos.support.FeatureAccessDialog
import github.tornaco.android.thanos.support.subscribe.LVLStateHolder
import github.tornaco.android.thanos.util.BrowserUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import now.fortuitous.thanos.apps.AppDetailsActivity
import now.fortuitous.thanos.main.ChooserActivity
import now.fortuitous.thanos.pref.AppPreference
import now.fortuitous.thanos.recovery.RecoveryUtilsActivity
import now.fortuitous.thanos.settings.FeatureToggleActivity
import now.fortuitous.thanos.settings.LicenseHelper
import org.orbitmvi.orbit.compose.collectAsState
import java.util.UUID

@Composable
fun SettingsScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        val vm = hiltViewModel<SettingsViewModel>()
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.loadState()
        }

        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val backupSuccessMsg = stringResource(R.string.pre_message_backup_success)
        val backupErrorTitle = stringResource(R.string.pre_message_backup_error)
        val actionLabel = stringResource(android.R.string.ok)

        val restoreSuccessMsg = stringResource(R.string.pre_message_restore_success)
        val logSuccess = stringResource(R.string.feedback_export_log_success)
        val logFail = stringResource(R.string.feedback_export_log_fail)

        val storageHelper = LocalSimpleStorageHelper.current

        SideEffect {
            storageHelper.onFileCreated = { code, file ->
                if (code == REQUEST_CODE_CREATE_BACKUP) {
                    vm.backup(file)
                } else if (code == REQUEST_CODE_CREATE_LOG) {
                    vm.log(file)
                }
            }

            storageHelper.onFileSelected = { code, files ->
                XLog.d("storageHelper onFileSelected- $files")
                val file = files.firstOrNull()
                file?.let {
                    vm.restore(file)
                } ?: Toast.makeText(context, "Canceled.", Toast.LENGTH_SHORT).show()
            }

        }

        val exportLog = {
            storageHelper.createFile(
                mimeType = "application/zip",
                fileName = "Thanox-Log-${DateUtils.formatForFileName(System.currentTimeMillis())}.zip",
                requestCode = REQUEST_CODE_CREATE_LOG
            )
        }

        LaunchedEffect(vm) {
            vm.backupPerformed.collectLatest {
                when (it) {
                    is BackupResult.Success -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "$backupSuccessMsg ${it.path}",
                                duration = SnackbarDuration.Indefinite,
                                actionLabel = actionLabel
                            )
                        }
                    }

                    is BackupResult.Failed -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "$backupErrorTitle ${it.tmpFile}",
                                duration = SnackbarDuration.Indefinite,
                                actionLabel = actionLabel
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(vm) {
            vm.restorePerformed.collectLatest {
                when (it) {
                    is RestoreResult.Success, RestoreResult.ResetComplete -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = restoreSuccessMsg,
                            )
                        }
                    }

                    is RestoreResult.Failed -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "ERROR: ${it.error}",
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(vm) {
            vm.logExportPerformed.collectLatest {
                when (it) {
                    is ExportLogResult.Success -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = logSuccess,
                            )
                        }
                    }

                    is ExportLogResult.Failed -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "ERROR: ${it.error}",
                            )
                        }
                    }
                }
            }
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    modifier = Modifier,
                    title = {
                        Row(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.nav_title_settings))
                            LinkText(stringResource(R.string.onboarding_guide_tips_title)) {
                                BrowserUtils.launch(context, BuildProp.THANOX_URL_DOCS_HOME)
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .consumeWindowInsets(paddingValues)
                    .padding(bottom = 32.dp)
                    .animateContentSize()

            ) {
                val subscribeState by LVLStateHolder.collectAsState()

                Spacer(Modifier.size(paddingValues.calculateTopPadding()))
                PreferenceUi(mutableListOf<Preference>().apply {
                    addAll(
                        generalSettings(
                            state = state,
                            vm = vm,
                            exportLog = exportLog
                        )
                    )
                    addAll(
                        strategySettings(
                            subState = subscribeState,
                            state = state,
                            vm = vm,
                        )
                    )
                    addAll(
                        dataSettings(
                            subState = subscribeState,
                            state = state,
                            vm = vm,
                        )
                    )
                    addAll(
                        uiSettings(
                            state = state,
                            vm = vm,
                        )
                    )
                    addAll(
                        devSettings(
                            state = state,
                            vm = vm,
                            exportLog = exportLog,
                        )
                    )
                    addAll(
                        aboutSettings(
                            state = state,
                            vm = vm,
                        )
                    )
                })
                StandardSpacer()

                SubscriptionStatus(subscribeState)

                LargeSpacer()
                FooterText()

                Spacer(Modifier.size(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun generalSettings(
    state: SettingsState,
    vm: SettingsViewModel,
    exportLog: () -> Unit,
): List<Preference> {
    val context = LocalContext.current
    val feedbackDialog = rememberConfirmDialogState()
    ConfirmDialog(
        title = stringResource(R.string.nav_title_feedback),
        messageHint = { it },
        data = stringResource(R.string.dialog_message_feedback),
        state = feedbackDialog,
        onConfirm = {
            exportLog()
        },
        confirmButton = stringResource(R.string.feedback_export_log),
    )
    return with(ThanosManager.from(context)) {
        listOf(
            Preference.Category(stringResource(R.string.pre_category_general)),
            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_toggle_fill,
                title = stringResource(R.string.pref_title_feature_toggle),
                hasLongSummary = false,
                onClick = {
                    FeatureToggleActivity.start(context)
                }
            ),
            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_battery_saver_fill,
                title = stringResource(R.string.pref_title_enable_power_save),
                summary = stringResource(R.string.pref_summary_enable_power_save),
                hasLongSummary = false,
                value = state.isPowerSaveModeEnabled,
                onCheckChanged = { enable ->
                    powerManager.isPowerSaveModeEnabled = enable
                    vm.loadState()
                }
            ),
            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_shield_cross_fill,
                title = stringResource(R.string.pref_title_app_stability_upkeep),
                summary = stringResource(R.string.pref_summary_app_stability_upkeep),
                hasLongSummary = false,
                value = state.isAppStabilityUpKeepEnabled,
                onCheckChanged = { enable ->
                    activityManager.isAppStabilityUpKeepEnabled = enable
                    vm.loadState()
                }
            ),
            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_file_list_3_fill,
                title = stringResource(R.string.pref_title_enable_global_white_list),
                summary = stringResource(R.string.pref_summary_enable_global_white_list),
                hasLongSummary = true,
                value = state.isProtectedWhitelistEnabled,
                onCheckChanged = { enable ->
                    pkgManager.isProtectedWhitelistEnabled = enable
                    vm.loadState()
                }
            ),

            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_device_recover_fill,
                title = stringResource(R.string.feature_title_recovery_tools),
                hasLongSummary = false,
                onClick = {
                    RecoveryUtilsActivity.start(context)
                }
            ),

            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_feedback_fill,
                title = stringResource(R.string.nav_title_feedback),
                hasLongSummary = false,
                onClick = {
                    feedbackDialog.show()
                }
            ),
        )
    }
}


@Composable
private fun uiSettings(
    state: SettingsState,
    vm: SettingsViewModel,
): List<Preference> {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pref = Pref(context)
    val dynamicColor by pref.uiThemeDynamicColor.collectAsStateWithLifecycle(true)
    val darkConfig by pref.uiThemeDarkModeConfig.collectAsStateWithLifecycle(ThemeSettings.DarkThemeConfig.FOLLOW_SYSTEM)
    val darkModeDialog = rememberMenuDialogState<Unit>(
        title = { "Dark mode" }, menuItems = ThemeSettings.DarkThemeConfig.entries.map {
            MenuDialogItem(id = it.name, title = it.name)
        }
    ) { _, id ->
        val config = ThemeSettings.DarkThemeConfig.valueOf(id)
        scope.launch { pref.serUiThemeDarkModeConfig(config) }
    }
    MenuDialog(darkModeDialog)
    return listOf(
        Preference.Category(stringResource(R.string.pre_category_ui)),

        Preference.SwitchPreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_number_9,
            title = stringResource(R.string.pre_title_app_list_item_show_version),
            value = state.uiShowAppVersion,
            onCheckChanged = { enable ->
                CommonPreferences.getInstance().setAppListShowVersionEnabled(context, enable)
                vm.loadState()
            }
        ),

        Preference.SwitchPreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_more_fill,
            title = stringResource(R.string.pre_title_app_list_item_show_pkg),
            value = state.uiShowAppPkgName,
            onCheckChanged = { enable ->
                CommonPreferences.getInstance().setAppListShowPkgNameEnabled(context, enable)
                vm.loadState()
            }
        ),

        Preference.SwitchPreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_palette_fill,
            title = "Dynamic color schema",
            value = dynamicColor,
            onCheckChanged = { enable ->
                scope.launch { pref.setUiThemeDynamicColor(enable) }
            }
        ),
        Preference.TextPreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_moon_fill,
            title = "Dark mode",
            summary = darkConfig.name,
            onClick = {
                darkModeDialog.show()
            }
        ),
    )
}


@Composable
private fun devSettings(
    state: SettingsState,
    vm: SettingsViewModel,
    exportLog: () -> Unit,
): List<Preference> {
    val context = LocalContext.current
    return with(ThanosManager.from(context)) {
        listOf(
            Preference.Category(stringResource(R.string.pre_category_dev_tools)),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_code_box_line,
                title = stringResource(R.string.pref_title_show_current_activity),
                value = state.showCurrentComponent,
                onCheckChanged = { enable ->
                    activityStackSupervisor.isShowCurrentComponentViewEnabled = enable
                    vm.loadState()
                }
            ),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_earth_fill,
                title = stringResource(R.string.title_net_stats),
                value = state.showTrafficStats,
                onCheckChanged = { enable ->
                    activityManager.isNetStatTrackerEnabled = enable
                    vm.loadState()
                }
            ),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_task_fill,
                title = stringResource(R.string.new_ops_feature),
                summary = stringResource(R.string.new_ops_feature_summary),
                value = state.newOPS,
                onCheckChanged = { enable ->
                    AppPreference.setFeatureNoticeAccepted(context, "NEW_OPS", enable)
                    if (enable) {
                        appOpsManager.isOpsEnabled = false
                    }
                    vm.loadState()
                }
            ),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_home_fill,
                title = "Test New home screen",
                value = state.newHome,
                onCheckChanged = { enable ->
                    AppPreference.setFeatureNoticeAccepted(context, "NEW_HOME", enable)
                    vm.loadState()
                }
            ),
            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_save_fill,
                title = stringResource(R.string.feedback_export_log),
                onClick = exportLog
            ),
        )
    }
}


@Composable
private fun aboutSettings(
    state: SettingsState,
    vm: SettingsViewModel,
): List<Preference> {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var clickTimes by remember { mutableIntStateOf(0) }
    return with(ThanosManager.from(context)) {
        listOf(
            Preference.Category(stringResource(R.string.pre_category_about)),
            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_information_fill,
                title = stringResource(R.string.pre_category_app_feature_info),
                summary = listOf(
                    BuildProp.THANOS_VERSION_NAME,
                    BuildProp.THANOS_VERSION_CODE,
                    BuildProp.THANOS_BUILD_FINGERPRINT,
                    BuildProp.THANOS_BUILD_DATE,
                    "Built on: ${BuildProp.THANOS_BUILD_HOST}"
                ).joinToString(),
                onClick = {
                    clickTimes += 1
                    if (clickTimes >= 12) {
                        clickTimes = 0
                        ChooserActivity.Starter.start(context)
                    }
                },
            ),

            Preference.ExpandablePreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_group_fill,
                title = stringResource(R.string.pref_title_rss_e),
                preferences = listOf(
                    Preference.TextPreference(
                        icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_qq_fill,
                        title = "QQ",
                        onClick = {
                            ClipboardUtils.copyToClipboard(
                                context,
                                "thanox QQ",
                                BuildProp.THANOX_QQ_PRIMARY
                            )
                            Toast.makeText(
                                context,
                                R.string.common_toast_copied_to_clipboard,
                                Toast.LENGTH_LONG
                            ).show()
                        },
                    ),
                    Preference.TextPreference(
                        icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_telegram_fill,
                        title = "Telegram",
                        onClick = {
                            BrowserUtils.launch(context, BuildProp.THANOX_TG_CHANNEL)
                        },
                    ),

                    Preference.TextPreference(
                        icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_mail_fill,
                        title = "Email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(
                                    Intent.EXTRA_EMAIL,
                                    arrayOf(BuildProp.THANOX_CONTACT_EMAIL)
                                )
                                putExtra(Intent.EXTRA_SUBJECT, "Thanox")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${BuildProp.THANOS_VERSION_NAME} ${Build.VERSION.SDK_INT} ${Build.DEVICE} ${Build.MODEL}"
                                )
                            }
                            context.startActivity(intent)
                        },
                    ),

                    Preference.TextPreference(
                        icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_github_fill,
                        title = "Github",
                        summary = "Tornaco/Thanox",
                        onClick = {

                        },
                    ),
                ),
            ),

            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_open_source_fill,
                title = stringResource(R.string.pref_title_open_source_license),
                onClick = {
                    LicenseHelper.showLicenseDialog(activity)
                },
            ),
        )
    }
}

@Composable
private fun strategySettings(
    subState: LVLStateHolder.State,
    state: SettingsState,
    vm: SettingsViewModel,
): List<Preference> {
    val context = LocalContext.current
    val badge = if (subState.isSubscribed) {
        null
    } else stringResource(R.string.module_donate_donated_available)

    val subscribeDialogState = rememberThanoxBottomSheetState()
    FeatureAccessDialog(subscribeDialogState)
    return with(ThanosManager.from(context)) {
        val templateSelectDialog = rememberMenuDialogState<Unit>(
            key1 = state.allConfigTemplateSelection,
            title = { context.getString(R.string.pref_title_new_installed_apps_config) },
            message = null,
            menuItems = state.allConfigTemplateSelection.map {
                MenuDialogItem(id = it.id, title = it.title)
            }
        ) { _, id ->
            profileManager.setAutoConfigTemplateSelection(id)
            vm.loadState()
        }
        MenuDialog(templateSelectDialog)

        val templateEditDialog = rememberMenuDialogState<ConfigTemplate>(
            title = { it?.title.orEmpty() },
            message = null,
            menuItems = listOf(
                MenuDialogItem(
                    id = "Edit",
                    title = stringResource(R.string.pref_action_edit_or_view_config_template)
                ),
                MenuDialogItem(
                    id = "Delete",
                    title = stringResource(R.string.pref_action_delete_config_template)
                )
            )
        ) { template, id ->
            if (template != null) {
                if (id == "Delete") {
                    profileManager.deleteConfigTemplate(template)
                } else {
                    val appInfo = AppInfo()
                    appInfo.isSelected = false
                    appInfo.pkgName = template.dummyPackageName
                    appInfo.appLabel = template.title
                    appInfo.isDummy = true
                    appInfo.versionCode = -1
                    appInfo.versionCode = -1
                    appInfo.uid = -1
                    appInfo.userId = 0
                    AppDetailsActivity.start(context, appInfo)
                }
                vm.loadState()
            }

        }
        MenuDialog(templateEditDialog)

        val addTemplateDialog = rememberTextInputState(
            title = stringResource(R.string.common_fab_title_add),
            message = null
        ) {
            val uuid = UUID.randomUUID().toString()
            val template =
                ConfigTemplate.builder()
                    .title(it)
                    .id(uuid)
                    .dummyPackageName(
                        ProfileManager
                            .PROFILE_AUTO_APPLY_NEW_INSTALLED_APPS_CONFIG_TEMPLATE_PACKAGE_PREFIX
                                + uuid
                    )
                    .createAt(System.currentTimeMillis())
                    .build()
            profileManager.addConfigTemplate(template)
            vm.loadState()
        }
        TextInputDialog(addTemplateDialog)

        listOf(
            Preference.Category(stringResource(R.string.pre_category_strategy)),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_tools_fill,
                title = stringResource(R.string.pref_title_new_installed_apps_config_enabled),
                summary = stringResource(R.string.pref_summary_new_installed_apps_config_enabled),
                hasLongSummary = false,
                value = state.isAutoApplyForNewInstalledAppsEnabled,
                onCheckChanged = { enable ->
                    if (subState.isSubscribed) {
                        profileManager.isAutoApplyForNewInstalledAppsEnabled = enable
                        vm.loadState()
                    } else {
                        subscribeDialogState.show()
                    }
                }
            ),

            Preference.SwitchPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_notification_4_fill,
                title = stringResource(R.string.pref_title_new_installed_apps_config_notification_enabled),
                summary = stringResource(R.string.pref_summary_new_installed_apps_config_notification_enabled),
                hasLongSummary = false,
                value = state.isAutoConfigTemplateNotificationEnabled,
                onCheckChanged = { enable ->
                    profileManager.isAutoConfigTemplateNotificationEnabled = enable
                    vm.loadState()
                }
            ),

            Preference.TextPreference(
                icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_file_code_fill,
                title = stringResource(R.string.pref_title_new_installed_apps_config),
                summary = state.autoConfigTemplateSelection?.title
                    ?: stringResource(R.string.common_text_value_not_set),
                onClick = {
                    if (subState.isSubscribed) {
                        templateSelectDialog.show()
                    } else {
                        subscribeDialogState.show()
                    }
                }
            ),

            Preference.ExpandablePreference(
                title = stringResource(R.string.pref_title_config_template_category),
                preferences = state.allConfigTemplateSelection.map {
                    Preference.TextPreference(
                        title = it.title,
                        onClick = {
                            if (subState.isSubscribed) {
                                templateEditDialog.show(it)
                            } else {
                                subscribeDialogState.show()
                            }
                        }
                    )
                } + listOf(
                    Preference.TextPreference(
                        icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_add_fill,
                        title = stringResource(R.string.common_fab_title_add),
                        onClick = {
                            if (subState.isSubscribed) {
                                addTemplateDialog.show()
                            } else {
                                subscribeDialogState.show()
                            }
                        }
                    ),
                ),
            ),
        )
    }
}


@Composable
private fun dataSettings(
    subState: LVLStateHolder.State,
    state: SettingsState,
    vm: SettingsViewModel,
): List<Preference> {
    val context = LocalContext.current

    val badge = if (subState.isSubscribed) {
        null
    } else stringResource(R.string.module_donate_donated_available)

    val subscribeDialogState = rememberThanoxBottomSheetState()
    FeatureAccessDialog(subscribeDialogState)

    val backUpFileNameDialogState = rememberTextInputState(
        title = stringResource(R.string.pre_title_backup),
        showSymbolButton = false,
        onSelected = {
        })
    TextInputDialog(state = backUpFileNameDialogState)

    val storageHelper = LocalSimpleStorageHelper.current

    val resetConfigDialog = rememberConfirmDialogState()
    ConfirmDialog(
        title = stringResource(R.string.pre_title_restore_default),
        state = resetConfigDialog,
        messageHint = { it },
        data = stringResource(R.string.pre_summary_restore_default)
    ) {
        vm.restoreDefault()
    }

    return listOf(
        Preference.Category(stringResource(R.string.pre_category_data)),
        Preference.TextPreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_upload_cloud_2_fill,
            title = stringResource(R.string.pre_title_backup),
            summary = stringResource(R.string.pre_summary_backup),
            onClick = {
                if (subState.isSubscribed) {
                    storageHelper.createFile(
                        mimeType = "application/zip",
                        fileName = autoGenBackupFileName() + ".zip",
                        requestCode = REQUEST_CODE_CREATE_BACKUP
                    )
                } else {
                    subscribeDialogState.show()
                }
            },
            badge = badge
        ),
        Preference.ExpandablePreference(
            icon = github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_download_cloud_fill,
            title = stringResource(R.string.pre_title_restore),
            summary = stringResource(R.string.pre_sumary_restore),
            onClick = {
                XLog.d("storageHelper openFilePicker")
                if (subState.isSubscribed) storageHelper.openFilePicker(
                    requestCode = 100,
                    allowMultiple = false,
                    initialPath = null,
                    "application/zip"
                ) else subscribeDialogState.show()
            },
            preferences = listOf(
                Preference.TextPreference(
                    title = stringResource(R.string.pre_title_restore_default),
                    summary = stringResource(R.string.pre_summary_restore_default),
                    onClick = {
                        resetConfigDialog.show()
                    }
                ),
            ),
            badge = badge
        ),
    )
}


@Composable
private fun SubscriptionStatus(subscribeState: LVLStateHolder.State) {
    Box(modifier = Modifier) {
        val context = LocalContext.current
        if (subscribeState.isSubscribed) {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                onClick = {
                    AppFeatureManager.launchSubscribeActivity?.invoke(context)
                }) {
                LottieLoadingView(
                    file = "47603-twinkle-crown.json",
                    modifier = Modifier.size(30.dp),
                )
                SmallSpacer()
                Text(text = stringResource(R.string.module_donate_donated), fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun FooterText() {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Since 2016",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            ),
        )
        StandardSpacer()
        @Suppress("KotlinConstantConditions")
        if (BuildProp.THANOS_BUILD_FLAVOR != "row") {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "备案号：陕ICP备20012350号-3A",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                ),
            )
        }
    }
}