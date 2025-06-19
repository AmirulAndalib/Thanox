package github.tornaco.thanos.android.module.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import github.tornaco.android.thanos.BasePreferenceFragmentCompat;
import github.tornaco.android.thanos.R;
import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.support.AppFeatureManager;
import github.tornaco.android.thanos.widget.EditTextDialog;
import github.tornaco.thanos.android.module.profile.engine.DateTimeEngineActivity;
import github.tornaco.thanos.android.module.profile.engine.danmu.DanmuUISettingsActivity;

public class RuleEngineSettingsFragment extends BasePreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.module_profile_rule_engines, rootKey);
    }

    @Override
    protected void onBindPreferences() {
        super.onBindPreferences();
        ThanosManager thanos = ThanosManager.from(getContext());
        if (!thanos.isServiceInstalled()) {
            getPreferenceScreen().setEnabled(false);
            return;
        }

        SwitchPreferenceCompat suPref = findPreference(getString(R.string.module_profile_pref_key_rule_engine_su));
        Objects.requireNonNull(suPref).setChecked(thanos.getProfileManager().isShellSuSupportInstalled());
        suPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean value = (boolean) newValue;
            thanos.getProfileManager().setShellSuSupportInstalled(value);
            return true;
        });

        Preference customSuPref = findPreference(getString(R.string.module_profile_pref_key_rule_engine_custom_su));

        Runnable updateSummary = () -> {
            String customSu = thanos.getProfileManager().getCustomSuCommand();
            if (customSu != null) {
                customSuPref.setSummary(thanos.getProfileManager().getCustomSuCommand()
                        + "\n"
                        + getString(github.tornaco.android.thanos.res.R.string.module_profile_pref_summary_rule_engine_custom_su));
            }
        };
        updateSummary.run();

        customSuPref.setOnPreferenceClickListener(preference -> {
            EditTextDialog.show(requireActivity(),
                    getString(github.tornaco.android.thanos.res.R.string.module_profile_pref_title_rule_engine_custom_su),
                    thanos.getProfileManager().getCustomSuCommand(),
                    newValue -> {
                        thanos.getProfileManager().setCustomSuCommand(newValue);
                        updateSummary.run();
                    });

            return true;
        });

        SwitchPreferenceCompat automationPref = findPreference(getString(R.string.module_profile_pref_key_rule_engine_automation));
        Objects.requireNonNull(automationPref).setChecked(thanos.getProfileManager().isProfileEngineUiAutomationEnabled());
        automationPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean value = (boolean) newValue;
            thanos.getProfileManager().setProfileEngineUiAutomationEnabled(value);
            return true;
        });

        SwitchPreferenceCompat pushPref = findPreference(getString(R.string.module_profile_pref_key_rule_engine_push));
        Objects.requireNonNull(pushPref).setChecked(thanos.getProfileManager().isProfileEnginePushEnabled());
        pushPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean value = (boolean) newValue;
            thanos.getProfileManager().setProfileEnginePushEnabled(value);
            return true;
        });

        findPreference(R.string.module_profile_pref_key_rule_engine_from_shortcut).setOnPreferenceClickListener(
                preference -> {
                    onRequestAddShortcut();
                    return true;
                }
        );

        findPreference(R.string.module_profile_pref_key_rule_engine_date_time).setOnPreferenceClickListener(
                preference -> {
                    DateTimeEngineActivity.Starter.INSTANCE.start(requireActivity());
                    return true;
                }
        );

        findPreference(R.string.module_profile_pref_key_rule_engine_danmu).setOnPreferenceClickListener(
                preference -> {
                    AppFeatureManager.INSTANCE.withSubscriptionStatus(requireContext(), isSubscribed -> {
                        if (isSubscribed) {
                            DanmuUISettingsActivity.Starter.INSTANCE.start(requireActivity());
                        } else {
                            AppFeatureManager.INSTANCE.showSubscribeDialog(requireActivity());
                        }
                        return null;
                    });

                    return true;
                }
        );
    }

    void onRequestAddShortcut() {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.module_profile_dialog_create_shortcut, null, false);
        EditText factValueText = dialogView.findViewById(R.id.fact_value);
        EditText labelText = dialogView.findViewById(R.id.label);

        new MaterialAlertDialogBuilder(requireActivity())
                .setView(dialogView)
                .setTitle(github.tornaco.android.thanos.res.R.string.module_profile_pref_title_rule_engine_shortcut)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String factValue = factValueText.getText().toString();
                    String label = labelText.getText().toString();
                    ProfileShortcutEngineActivity.ShortcutHelper.addShortcut(getActivity(),
                            label,
                            factValue);
                }).show();
    }
}
