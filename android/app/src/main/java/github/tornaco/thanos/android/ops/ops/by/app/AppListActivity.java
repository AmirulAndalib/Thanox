package github.tornaco.thanos.android.ops.ops.by.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import github.tornaco.android.thanos.common.AppItemClickListener;
import github.tornaco.android.thanos.common.AppListItemDescriptionComposer;
import github.tornaco.android.thanos.common.AppListModel;
import github.tornaco.android.thanos.common.CommonAppListFilterActivity;
import github.tornaco.android.thanos.common.CommonAppListFilterViewModel;
import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.core.pm.AppInfo;
import github.tornaco.android.thanos.util.ActivityUtils;
import github.tornaco.android.thanos.widget.SwitchBar;
import util.CollectionUtils;

public class AppListActivity extends CommonAppListFilterActivity {

    public static void start(Context context) {
        ActivityUtils.startActivity(context, AppListActivity.class);
    }

    @Override
    protected int getTitleRes() {
        return github.tornaco.android.thanos.res.R.string.module_ops_activity_title_app_ops_list;
    }

    @NonNull
    @Override
    protected AppItemClickListener onCreateAppItemViewClickListener() {
        return appInfo -> AppOpsListActivity.start(AppListActivity.this, appInfo);
    }

    @NonNull
    @Override
    protected CommonAppListFilterViewModel.ListModelLoader onCreateListModelLoader() {
        AppListItemDescriptionComposer composer = new AppListItemDescriptionComposer(thisActivity());
        return index -> {
            ThanosManager thanos = ThanosManager.from(getApplicationContext());
            if (!thanos.isServiceInstalled()) {
                return Lists.newArrayList(new AppListModel(AppInfo.dummy()));
            }
            List<AppInfo> installed = thanos.getPkgManager().getInstalledPkgsByPackageSetId(index.pkgSetId);
            List<AppListModel> res = new ArrayList<>();
            CollectionUtils.consumeRemaining(installed, appInfo ->
                    res.add(new AppListModel(appInfo, null, null, composer.getAppItemDescription(appInfo))));
            Collections.sort(res);
            return res;
        };
    }

    @Override
    protected void onSetupSwitchBar(SwitchBar switchBar) {
        super.onSetupSwitchBar(switchBar);
        switchBar.hide();
    }
}
