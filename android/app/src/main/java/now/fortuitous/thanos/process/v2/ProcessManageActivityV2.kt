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

package now.fortuitous.thanos.process.v2

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import github.tornaco.android.thanos.module.compose.common.ComposeThemeActivity
import github.tornaco.android.thanos.support.ThanoxAppContext
import github.tornaco.android.thanos.util.ActivityUtils

@AndroidEntryPoint
class ProcessManageActivityV2 : ComposeThemeActivity() {

    object Starter {
        fun start(context: Context?) {
            ActivityUtils.startActivity(context, ProcessManageActivityV2::class.java)
        }
    }


    override fun getApplicationContext(): Context {
        return ThanoxAppContext(super.getApplicationContext())
    }

    @Composable
    override fun Content() {
        Surface {
            ProcessManageScreen(
                onBackPressed = {
                    finish()
                })
        }
    }
}