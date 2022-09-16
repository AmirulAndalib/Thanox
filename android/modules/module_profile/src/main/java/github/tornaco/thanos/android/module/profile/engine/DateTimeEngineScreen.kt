/*
 * (C) Copyright 2022 Thanox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package github.tornaco.thanos.android.module.profile.engine

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.enro.core.compose.registerForNavigationResult
import github.tornaco.android.thanos.core.alarm.AlarmRecord
import github.tornaco.android.thanos.module.compose.common.theme.TypographyDefaults
import github.tornaco.android.thanos.module.compose.common.widget.*
import github.tornaco.android.thanos.module.compose.common.widget.Switch
import github.tornaco.thanos.android.module.profile.R
import java.util.*
import kotlin.math.min
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val ID_TIME_OF_A_DAY = "tod"
private const val ID_REGULAR_INTERVAL = "ri"

sealed class BottomNavItem(var title: String, var icon: Int, var screenRoute: String) {
    object TimeOfADay :
        BottomNavItem("TimeOfADay", R.drawable.ic_account_circle_fill, "TimeOfADay")

    object RegularInterval :
        BottomNavItem("RegularInterval", R.drawable.ic_alipay_fill, "RegularInterval")
}

@Composable
fun Activity.DateTimeEngineScreen() {
    val viewModel = hiltViewModel<DateTimeEngineViewModel>()
    val state by viewModel.state.collectAsState()

    SideEffect {
        viewModel.loadPendingWorks()
        viewModel.loadAlarms()
    }

    val newRegularIntervalHandle =
        registerForNavigationResult<NewRegularIntervalResult> { regularInterval ->
            viewModel.schedulePeriodicWork(
                regularInterval.tag,
                regularInterval.durationMillis.toDuration(DurationUnit.MILLISECONDS)
            )
        }

    val newAlarmHandle = registerForNavigationResult<NewAlarmResult> { alarm ->
        viewModel.addAlarm(alarm.alarm)
    }

    val typeSelectDialogState =
        rememberSingleChoiceDialogState(title = stringResource(id = R.string.module_profile_rule_new),
            items = listOf(
                SingleChoiceItem(
                    id = ID_TIME_OF_A_DAY,
                    icon = Icons.Filled.Schedule,
                    label = stringResource(id = R.string.module_profile_date_time_alarm)
                ),
                SingleChoiceItem(
                    id = ID_REGULAR_INTERVAL,
                    icon = Icons.Filled.Timer,
                    label = stringResource(id = R.string.module_profile_date_time_regular_interval)
                ),
            ),
            onItemClick = {
                when (it) {
                    ID_REGULAR_INTERVAL -> {
                        newRegularIntervalHandle.open(NewRegularInterval)
                    }
                    ID_TIME_OF_A_DAY -> {
                        newAlarmHandle.open(NewAlarm)
                    }
                }
            })

    val navController = rememberNavController()

    ThanoxSmallAppBarScaffold(
        title = {
            Text(
                text = stringResource(id = R.string.module_profile_pref_title_rule_engine_date_time),
                style = TypographyDefaults.appBarTitleTextStyle()
            )
        },
        onBackPressed = { finish() },
        actions = {
        },
        bottomBar = {
            NavigationContent(navController)
        },
        floatingActionButton = {
            ExtendableFloatingActionButton(
                extended = false,
                text = { Text(text = stringResource(id = R.string.module_profile_rule_new)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.module_profile_rule_new)
                    )
                }) {
                typeSelectDialogState.show()
            }
        }) { contentPadding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)) {
            SingleChoiceDialog(state = typeSelectDialogState)
            NavigationGraph(navController = navController, viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun NavigationContent(
    navController: NavHostController,
) {
    val items = listOf(
        BottomNavItem.TimeOfADay,
        BottomNavItem.RegularInterval
    )
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = item.icon),
                        contentDescription = item.title)
                },
                label = {
                    Text(text = item.title, fontSize = 9.sp)
                },
                alwaysShowLabel = true,
                selected = currentRoute == item.screenRoute,
                onClick = {
                    navController.navigate(item.screenRoute) {
                        navController.graph.startDestinationRoute?.let { screen_route ->
                            popUpTo(screen_route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    viewModel: DateTimeEngineViewModel,
    state: DateTimeState,
) {
    NavHost(navController, startDestination = BottomNavItem.TimeOfADay.screenRoute) {
        composable(BottomNavItem.TimeOfADay.screenRoute) {
            WorkList(state = state.workStates) {
                viewModel.deleteWorkById(it)
            }
        }

        composable(BottomNavItem.RegularInterval.screenRoute) {
            AlarmList(state = state.alarms,
                delete = { viewModel.deleteAlarm(it) },
                onCheckChange = { record, checked ->
                    viewModel.setAlarmEnabled(record.alarm, checked)
                })
        }
    }
}


@Composable
private fun WorkList(
    state: List<WorkState>,
    delete: (id: UUID) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                ) {
                    val typeText = if (it.type == Type.Periodic) {
                        stringResource(id = R.string.module_profile_date_time_regular_interval)
                    } else {
                        "Unknown"
                    }
                    val valueText = if (it.type == Type.Periodic) {
                        it.value.toDuration(DurationUnit.MILLISECONDS).toString()
                    } else {
                        ""
                    }

                    Text(
                        text = typeText, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    TinySpacer()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = "tag"
                        )
                        TinySpacer()
                        Text(
                            text = it.tag.substring(0, min(18, it.tag.length)),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "interval"
                        )
                        TinySpacer()
                        Text(
                            text = valueText, style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = {
                    delete(it.id)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.module_profile_ic_delete_bin_fill),
                        contentDescription = "Remove"
                    )
                }
            }

        }
    }
}


@Composable
private fun AlarmList(
    state: List<AlarmRecord>,
    delete: (alarm: AlarmRecord) -> Unit,
    onCheckChange: (alarm: AlarmRecord, checked: Boolean) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state) { record ->
            Column(modifier = Modifier
                .fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val typeText = stringResource(id = R.string.module_profile_date_time_alarm)
                        val valueText = record.alarm.triggerAt.toString()

                        Text(
                            text = typeText, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        TinySpacer()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = "tag"
                            )
                            TinySpacer()
                            Text(
                                text = record.alarm.label.substring(0,
                                    min(18, record.alarm.label.length)),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "interval"
                            )
                            TinySpacer()
                            Text(
                                text = valueText, style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    Switch(
                        modifier = Modifier,
                        checked = record.isEnabled,
                        onCheckedChange = { checked ->
                            onCheckChange(record, checked)
                        })
                }

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        onClick = {
                            delete(record)
                        }) {
                        Icon(
                            painter = painterResource(id = R.drawable.module_profile_ic_delete_bin_fill),
                            contentDescription = "Remove"
                        )
                    }
                }
            }
        }
    }
}