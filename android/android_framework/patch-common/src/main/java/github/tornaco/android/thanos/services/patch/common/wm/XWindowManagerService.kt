package github.tornaco.android.thanos.services.patch.common.wm

import com.elvishew.xlog.XLog
import util.XposedHelpers
import java.util.function.Consumer

object XWindowManagerService {
    @JvmStatic
    fun wmsClass(classLoader: ClassLoader): Class<*> {
        return XposedHelpers.findClass(
            "com.android.server.wm.WindowManagerService",
            classLoader
        )
    }

    @JvmStatic
    fun wpcClass(classLoader: ClassLoader): Class<*> {
        return XposedHelpers.findClass(
            "com.android.server.wm.ConfigurationContainer",
            classLoader
        )
    }

    @JvmStatic
    fun getRoot(wms: Any): Any? {
        return kotlin.runCatching {
            XposedHelpers.getObjectField(
                wms,
                "mRoot"
            )
        }.getOrElse {
            XLog.e("XWindowManagerService#getRoot error", it)
            null
        }
    }

    // void forAllWindows(Consumer<WindowState> var1, boolean var2)
    @JvmStatic
    fun forAllWindows(root: Any, consumer: Consumer<*>) {
        kotlin.runCatching {
            XposedHelpers.callMethod(
                root,
                "forAllWindows",
                consumer,
                false /* traverseTopToBottom */
            )
        }.onFailure {
            XLog.e("XWindowManagerService#forAllWindows error", it)
        }
    }
}