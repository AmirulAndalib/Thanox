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

package now.fortuitous.thanos.apps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import github.tornaco.android.thanos.common.AppItemClickListener;
import github.tornaco.android.thanos.common.AppListItemDescriptionComposer;
import github.tornaco.android.thanos.common.AppListModel;
import github.tornaco.android.thanos.common.CommonAppListFilterActivity;
import github.tornaco.android.thanos.common.CommonAppListFilterViewModel;
import github.tornaco.android.thanos.core.pm.AppInfo;
import github.tornaco.android.thanos.core.pm.Pkg;
import github.tornaco.android.thanos.support.ContextExtKt;
import github.tornaco.android.thanos.util.ActivityUtils;
import github.tornaco.android.thanos.widget.SwitchBar;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class AppsManageActivity extends CommonAppListFilterActivity {

    public static void start(Context context) {
        ActivityUtils.startActivity(context, AppsManageActivity.class);
    }

    @Override
    protected int getTitleRes() {
        return github.tornaco.android.thanos.res.R.string.activity_title_apps_manager;
    }

    @Override
    protected void onSetupFab(ExtendedFloatingActionButton fab1, ExtendedFloatingActionButton fab2) {
        super.onSetupFab(fab1, fab2);
        fab1.show();
        fab1.setText(github.tornaco.android.thanos.res.R.string.title_package_sets);
        fab1.setIconResource(github.tornaco.android.thanos.icon.remix.R.drawable.ic_remix_folder_2_line);
        fab1.setOnClickListener(v -> PackageSetListActivity.start(thisActivity()));
    }

    @NonNull
    @Override
    protected AppItemClickListener onCreateAppItemViewClickListener() {
        return appInfo -> AppDetailsActivity.start(AppsManageActivity.this, appInfo);
    }

    @NonNull
    @Override

    protected CommonAppListFilterViewModel.ListModelLoader onCreateListModelLoader() {
        AppListItemDescriptionComposer composer = new AppListItemDescriptionComposer(thisActivity());
        String runningBadge = getString(github.tornaco.android.thanos.res.R.string.badge_app_running);
        String idleBadge = getApplicationContext().getString(github.tornaco.android.thanos.res.R.string.badge_app_idle);
        return index -> ContextExtKt.withThanos(getApplicationContext(), thanos -> {
            List<AppListModel> res = new ArrayList<>();
            CompositeDisposable disposable = new CompositeDisposable();
            disposable.add(Observable.fromIterable(thanos.getPkgManager().getInstalledPkgsByPackageSetId(index.pkgSetId))
                    .distinct()
                    .doOnComplete(disposable::dispose)
                    .subscribe(appInfo -> res.add(new AppListModel(
                            appInfo,
                            thanos.getActivityManager().isPackageRunning(Pkg.fromAppInfo(appInfo)) ? runningBadge : null,
                            thanos.getActivityManager().isPackageIdle(Pkg.fromAppInfo(appInfo)) ? idleBadge : null,
                            composer.getAppItemDescription(appInfo)))));
            Collections.sort(res);
            return res;
        }, Lists.newArrayList(new AppListModel(AppInfo.dummy())));
    }

    @Override
    protected void onSetupSwitchBar(SwitchBar switchBar) {
        super.onSetupSwitchBar(switchBar);
        switchBar.hide();
    }
}
