package github.tornaco.thanos.android.ops.ops.by.ops;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import github.tornaco.android.thanos.common.CommonPreferences;
import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.widget.SwitchBar;
import github.tornaco.thanos.android.ops.R;
import github.tornaco.thanos.android.ops.databinding.ModuleOpsLayoutAllOpsBinding;
import github.tornaco.thanos.android.ops.model.Op;
import github.tornaco.thanos.android.ops.ops.OpItemClickListener;

public class AllOpsListFragment extends Fragment {

    private ModuleOpsLayoutAllOpsBinding binding;
    private AllOpsListViewModel viewModel;

    public static AllOpsListFragment newInstance() {
        return new AllOpsListFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ModuleOpsLayoutAllOpsBinding.inflate(LayoutInflater.from(requireContext()));
        setupView();
        setupViewModel();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.start();
    }

    private void setupView() {
        binding.toolbar.setTitle(getString(github.tornaco.android.thanos.res.R.string.module_ops_feature_title_ops_app_list));

        binding.apps.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.apps.setAdapter(new AllOpsListAdapter(new OpItemClickListener() {
            @Override
            public void onOpItemClick(@NonNull Op op, @NonNull View anchor) {
                OpsAppListActivity.start(requireActivity(), op);
            }
        }));

        binding.swipe.setOnRefreshListener(() -> viewModel.start());
        binding.swipe.setColorSchemeColors(
                getResources().getIntArray(github.tornaco.android.thanos.module.common.R.array.common_swipe_refresh_colors));

        // Switch.
        onSetupSwitchBar(binding.switchBarContainer.switchBar);

        binding.toolbar.inflateMenu(R.menu.module_ops_list);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (R.id.action_reset_all_modes == item.getItemId()) {
                new MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(github.tornaco.android.thanos.res.R.string.module_ops_title_reset_ops_mode_for_all)
                        .setMessage(github.tornaco.android.thanos.res.R.string.common_dialog_message_are_you_sure)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                ThanosManager.from(requireContext())
                                        .ifServiceInstalled(thanosManager ->
                                                thanosManager.getAppOpsManager().resetAllModes("*")))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            }
            return false;
        });

        boolean alreadyRead =
                CommonPreferences.getInstance()
                        .isFeatureDescRead(requireContext(), "LegacyOps");
        if (alreadyRead) {
            binding.featureDescription.getRoot().setVisibility(View.GONE);
            return;
        }

        binding.featureDescription.featureDescView.setDescription(getString(github.tornaco.android.thanos.res.R.string.legacy_ops_feature_summary));
        binding.featureDescription.featureDescView.setOnCloseClickListener(() -> {
            CommonPreferences.getInstance()
                    .setFeatureDescRead(requireContext(), "LegacyOps", true);
            binding.featureDescription.getRoot().setVisibility(View.GONE);
            return null;
        });
    }

    protected void onSetupSwitchBar(SwitchBar switchBar) {
        switchBar.setOnLabel(getString(github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_ops_feature_title_ops_app_list)));
        switchBar.setOffLabel(getString(github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_ops_feature_title_ops_app_list)));
        switchBar.setChecked(getSwitchBarCheckState());
        switchBar.addOnSwitchChangeListener(this::onSwitchBarCheckChanged);
    }

    protected boolean getSwitchBarCheckState() {
        return ThanosManager.from(requireContext())
                .isServiceInstalled()
                && ThanosManager.from(requireContext())
                .getAppOpsManager().isOpsEnabled();
    }

    protected void onSwitchBarCheckChanged(MaterialSwitch switchBar, boolean isChecked) {
        ThanosManager.from(requireContext())
                .ifServiceInstalled(thanosManager -> thanosManager.getAppOpsManager()
                        .setOpsEnabled(isChecked));
    }

    private void setupViewModel() {
        viewModel = obtainViewModel(requireActivity());
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        binding.executePendingBindings();
    }

    public static AllOpsListViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelProvider.AndroidViewModelFactory factory = ViewModelProvider.AndroidViewModelFactory
                .getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(AllOpsListViewModel.class);
    }
}
