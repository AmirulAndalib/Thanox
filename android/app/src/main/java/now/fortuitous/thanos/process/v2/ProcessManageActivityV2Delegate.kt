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


import android.app.Activity
import android.os.Bundle
import github.tornaco.android.thanos.support.AppFeatureManager
import github.tornaco.android.thanos.support.AppFeatureManager.withSubscriptionStatus

class ProcessManageActivityV2Delegate : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        withSubscriptionStatus(this) { isSubscribed ->
            if (isSubscribed) {
                ProcessManageActivityV2.Starter.start(this)
                finish()
            } else {
                AppFeatureManager.showSubscribeDialog(this)
            }
        }
    }
}