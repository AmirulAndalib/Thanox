package now.fortuitous.thanos.apps

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
import com.elvishew.xlog.XLog
import github.tornaco.android.thanos.core.app.ThanosManager
import github.tornaco.android.thanos.core.pm.AppInfo
import github.tornaco.android.thanos.core.pm.ComponentInfo
import github.tornaco.android.thanos.core.pm.Pkg
import github.tornaco.android.thanos.core.util.GsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import util.JsonFormatter

class AppDetailsViewModel : ViewModel() {
    @SuppressWarnings("UnstableApiUsage")
    fun performComponentsBackup(
        context: Context,
        listener: BackupListener,
        pickedFile: DocumentFile,
        app: AppInfo
    ) {
        val pickedFileOS = pickedFile.openOutputStream(context)
        if (pickedFileOS == null) {
            Toast.makeText(context, "Unable to open output stream.", Toast.LENGTH_LONG).show()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val thanos = ThanosManager.from(context)
                    val disabledComponents: List<String> =
                        thanos.pkgManager.getAllDisabledComponentsForPackage(Pkg.fromAppInfo(app))
                            .map {
                                it.flattenToString()
                            }
                    val backup = AppComponentBackup(disabledComponents)

                    val json = JsonFormatter.toPrettyJson(backup)
                    XLog.d(
                        "performComponentsBackup, json: $json"
                    )
                    pickedFileOS.use {
                        it.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        listener.onSuccess()
                    }
                }.onFailure {
                    XLog.e(it, "performComponentsBackup error")
                    withContext(Dispatchers.Main) {
                        listener.onFail(it.stackTraceToString())
                    }
                }
            }
        }
    }

    fun performComponentsRestore(
        context: Context,
        listener: RestoreListener,
        pickedFile: DocumentFile,
        app: AppInfo
    ) {
        val pickedFileIS = pickedFile.openInputStream(context)
        if (pickedFileIS == null) {
            Toast.makeText(context, "Unable to open input stream.", Toast.LENGTH_LONG).show()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    pickedFileIS.use { stream ->
                        runCatching {
                            val json = stream.bufferedReader().readText()
                            val backup =
                                GsonUtils.GSON.fromJson(json, AppComponentBackup::class.java)
                            XLog.d("performComponentsRestore, backup: $backup")
                            val pm = ThanosManager.from(context).pkgManager

                            fun isDisabledInBackup(comp: ComponentInfo): Boolean {
                                return backup.disabledComponents.any { it == comp.componentName.flattenToString() || it == comp.componentName.flattenToShortString() }
                            }

                            // Activity
                            for (i in 0 until Int.MAX_VALUE) {
                                val batch: List<ComponentInfo> =
                                    pm.getActivitiesInBatch(
                                        app.userId,
                                        app.pkgName,
                                        20,
                                        i
                                    ) ?: break
                                batch.forEach { comp ->
                                    val newState =
                                        if (isDisabledInBackup(comp)) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    if (pm.getComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName
                                        ) != newState
                                    ) {
                                        pm.setComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName,
                                            newState,
                                            0
                                        )
                                    }
                                }
                            }

                            // Service
                            for (i in 0 until Int.MAX_VALUE) {
                                val batch: List<ComponentInfo> =
                                    pm.getServicesInBatch(
                                        app.userId,
                                        app.pkgName,
                                        20,
                                        i
                                    ) ?: break
                                batch.forEach { comp ->
                                    val newState =
                                        if (isDisabledInBackup(comp)) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    if (pm.getComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName
                                        ) != newState
                                    ) {
                                        pm.setComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName,
                                            newState,
                                            0
                                        )
                                    }

                                }
                            }

                            // Receiver
                            for (i in 0 until Int.MAX_VALUE) {
                                val batch: List<ComponentInfo> =
                                    pm.getReceiversInBatch(
                                        app.userId,
                                        app.pkgName,
                                        20,
                                        i
                                    ) ?: break
                                batch.forEach { comp ->
                                    val newState =
                                        if (isDisabledInBackup(comp)) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    if (pm.getComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName
                                        ) != newState
                                    ) {
                                        pm.setComponentEnabledSetting(
                                            app.userId,
                                            comp.componentName,
                                            newState,
                                            0
                                        )
                                    }

                                }
                            }

                            // Provider
                            val batch: List<ComponentInfo> = pm.getProviders(
                                app.userId,
                                app.pkgName,
                            )
                            batch.forEach { comp ->
                                val newState =
                                    if (isDisabledInBackup(comp)) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                if (pm.getComponentEnabledSetting(
                                        app.userId,
                                        comp.componentName
                                    ) != newState
                                ) {
                                    pm.setComponentEnabledSetting(
                                        app.userId,
                                        comp.componentName,
                                        newState,
                                        0
                                    )
                                }
                            }

                            withContext(Dispatchers.Main) {
                                listener.onSuccess()
                            }

                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Unable to read file. ${it.toString()}",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                }.onFailure {
                    XLog.e(it, "performComponentsRestore error")
                    withContext(Dispatchers.Main) {
                        listener.onFail(it.stackTraceToString())
                    }
                }
            }
        }
    }


    interface BackupListener {
        @UiThread
        fun onSuccess()

        @UiThread
        fun onFail(errMsg: String?)
    }


    interface RestoreListener {
        @UiThread
        fun onSuccess()

        @UiThread
        fun onFail(errMsg: String?)
    }

}