package github.tornaco.android.thanox.module.activity.trampoline;

import static com.nononsenseapps.filepicker.FilePickerActivityUtils.pickSingleDirIntent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.Observable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elvishew.xlog.XLog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.common.io.Files;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.core.app.component.ComponentReplacement;
import github.tornaco.android.thanos.core.pm.ComponentNameBrief;
import github.tornaco.android.thanos.core.util.DateUtils;
import github.tornaco.android.thanos.core.util.OsUtils;
import github.tornaco.android.thanos.support.ThanoxAppContext;
import github.tornaco.android.thanos.theme.ThemeActivity;
import github.tornaco.android.thanos.util.ActivityUtils;
import github.tornaco.android.thanos.util.IntentUtils;
import github.tornaco.android.thanos.widget.SwitchBar;
import github.tornaco.android.thanox.module.activity.trampoline.databinding.ModuleActivityTrampolineActivityBinding;
import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;

@RuntimePermissions
public class ActivityTrampolineActivity extends ThemeActivity
        implements ActivityTrampolineItemClickListener {

    private static final int REQUEST_CODE_PICK_IMPORT_PATH = 0x111;

    private static final SparseArray<String> requestCodeExportMapping = new SparseArray<>();
    private static final SparseArray<String> requestCodeExportMappingQ = new SparseArray<>();

    private static final AtomicInteger _REQ_ID = new AtomicInteger(0x222);

    private ModuleActivityTrampolineActivityBinding binding;
    private TrampolineViewModel viewModel;

    public static void start(Context context) {
        ActivityUtils.startActivity(context, ActivityTrampolineActivity.class);
    }

    @Override
    public Context getApplicationContext() {
        return new ThanoxAppContext(super.getApplicationContext());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ModuleActivityTrampolineActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setupView();
        setupViewModel();
    }

    private void setupView() {
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // List.
        binding.replacements.setLayoutManager(new LinearLayoutManager(this));
        binding.replacements.setAdapter(new ActivityTrampolineAdapter(this));
        binding.swipe.setOnRefreshListener(() -> viewModel.start());
        binding.swipe.setColorSchemeColors(getResources().getIntArray(github.tornaco.android.thanos.module.common.R.array.common_swipe_refresh_colors));


        // Search.
        binding.searchView.setOnQueryTextListener(
                new MaterialSearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        viewModel.setSearchText(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        viewModel.setSearchText(newText);
                        return true;
                    }
                });

        binding.searchView.setOnSearchViewListener(
                new MaterialSearchView.SearchViewListener() {
                    @Override
                    public void onSearchViewShown() {
                        binding.toolbarLayout.setTitleEnabled(false);
                        binding.appbar.setExpanded(false, true);
                    }

                    @Override
                    public void onSearchViewClosed() {
                        viewModel.clearSearchText();
                        binding.toolbarLayout.setTitleEnabled(true);
                    }
                });

        // Switch.
        onSetupSwitchBar(binding.switchBarContainer.switchBar);

        binding.fab.setOnClickListener(v -> {
            ThanosManager.from(getApplicationContext())
                    .ifServiceInstalled(thanosManager -> showAddReplacementDialog(null, null, null, false));
        });
    }

    private void onSetupSwitchBar(SwitchBar switchBar) {
        switchBar.setOnLabel(getString(github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_activity_trampoline_app_name)));
        switchBar.setOffLabel(getString(github.tornaco.android.thanos.res.R.string.common_switchbar_title_format,
                getString(github.tornaco.android.thanos.res.R.string.module_activity_trampoline_app_name)));
        switchBar.setChecked(getSwitchBarCheckState());
        switchBar.addOnSwitchChangeListener(this::onSwitchBarCheckChanged);
    }

    private boolean getSwitchBarCheckState() {
        return ThanosManager.from(getApplicationContext()).isServiceInstalled()
                && ThanosManager.from(getApplicationContext()).getActivityStackSupervisor()
                .isActivityTrampolineEnabled();
    }

    private void onSwitchBarCheckChanged(MaterialSwitch switchBar, boolean isChecked) {
        ThanosManager.from(getApplicationContext())
                .ifServiceInstalled(thanosManager -> thanosManager.getActivityStackSupervisor()
                        .setActivityTrampolineEnabled(isChecked));
    }

    private void setupViewModel() {
        viewModel = obtainViewModel(this);
        viewModel.start();

        viewModel.getExportSuccessSignal().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                Toast.makeText(thisActivity(), "\uD83D\uDC4D", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getExportFailSignal().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                Toast.makeText(thisActivity(), "\uD83D\uDC4E", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getImportSuccessSignal().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                Toast.makeText(thisActivity(), "\uD83D\uDC4D", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getImportFailSignal().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                Toast.makeText(thisActivity(), "\uD83D\uDC4E", Toast.LENGTH_SHORT).show();
            }
        });

        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        binding.executePendingBindings();
    }

    private void showAddReplacementDialog(String from, String to, String note, boolean canDelete) {
        View layout = LayoutInflater.from(this).inflate(R.layout.module_activity_trampoline_comp_replace_editor, null, false);

        final AppCompatEditText fromEditText = layout.findViewById(R.id.from_comp);
        fromEditText.setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        fromEditText.setText(from);

        final AppCompatEditText toEditText = layout.findViewById(R.id.to_comp);
        toEditText.setFilters(new InputFilter[]{new EmojiExcludeFilter()});
        toEditText.setText(to);

        final AppCompatEditText noteEditText = layout.findViewById(R.id.note);
        noteEditText.setText(note);

        AlertDialog d = new MaterialAlertDialogBuilder(ActivityTrampolineActivity.this)
                .setTitle(canDelete
                        ? github.tornaco.android.thanos.res.R.string.module_activity_trampoline_edit_dialog_title
                        : github.tornaco.android.thanos.res.R.string.module_activity_trampoline_add_dialog_title)
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        onRequestAddNewReplacement(
                                fromEditText.getEditableText().toString(),
                                toEditText.getEditableText().toString(),
                                noteEditText.getEditableText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        if (canDelete) {
            d.setButton(DialogInterface.BUTTON_NEUTRAL,
                    getString(github.tornaco.android.thanos.res.R.string.module_activity_trampoline_add_dialog_delete),
                    (dialog, which) -> onRequestDeleteNewReplacement(from, to));
        }
        d.show();
    }

    private void onRequestDeleteNewReplacement(String f, String t) {
        if (TextUtils.isEmpty(f) || TextUtils.isEmpty(t) || TextUtils.isEmpty(f.trim()) || TextUtils.isEmpty(t.trim())) {
            showComponentEmptyTips();
            return;
        }
        ComponentNameBrief fromCompName = ComponentNameBrief.unflattenFromString(f);
        if (fromCompName == null) {
            showComponentFromInvalidTips();
            return;
        }
        ComponentNameBrief toCompName = ComponentNameBrief.unflattenFromString(t);
        if (toCompName == null) {
            showComponentToInvalidTips();
            return;
        }
        viewModel.onRequestRemoveNewReplacement(fromCompName, toCompName);
    }

    private void onRequestAddNewReplacement(String f, String t, String note) {
        if (TextUtils.isEmpty(f) || TextUtils.isEmpty(t) || TextUtils.isEmpty(f.trim()) || TextUtils.isEmpty(t.trim())) {
            showComponentEmptyTips();
            return;
        }
        ComponentNameBrief fromCompName = ComponentNameBrief.unflattenFromString(f);
        if (fromCompName == null) {
            showComponentFromInvalidTips();
            return;
        }
        ComponentNameBrief toCompName = ComponentNameBrief.unflattenFromString(t);
        if (toCompName == null) {
            showComponentToInvalidTips();
            return;
        }
        viewModel.onRequestAddNewReplacement(fromCompName, toCompName, note);
    }

    private void showComponentFromInvalidTips() {
        Toast.makeText(
                        ActivityTrampolineActivity.this,
                        github.tornaco.android.thanos.res.R.string.module_activity_trampoline_add_invalid_from_component
                        , Toast.LENGTH_LONG)
                .show();
    }

    private void showComponentToInvalidTips() {
        Toast.makeText(
                        ActivityTrampolineActivity.this,
                        github.tornaco.android.thanos.res.R.string.module_activity_trampoline_add_invalid_to_component
                        , Toast.LENGTH_LONG)
                .show();
    }

    private void showComponentEmptyTips() {
        Toast.makeText(
                        ActivityTrampolineActivity.this,
                        github.tornaco.android.thanos.res.R.string.module_activity_trampoline_add_empty_component
                        , Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void onItemClick(@NonNull View view, @NonNull ComponentReplacement replacement) {
        showItemPopMenu(view, replacement);
    }

    private void showItemPopMenu(@NonNull View anchor, @NonNull ComponentReplacement replacement) {
        PopupMenu popupMenu = new PopupMenu(thisActivity(), anchor);
        popupMenu.inflate(R.menu.module_activity_trampoline_menu_trampoline_item);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                showAddReplacementDialog(replacement.from.flattenToString(), replacement.to.flattenToString(), replacement.note, true);
                return true;
            }
            if (item.getItemId() == R.id.action_export) {
                onRequestExport(replacement.from.flattenToString());
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.module_activity_trampoline_menu_trampoline, menu);
        MenuItem item = menu.findItem(github.tornaco.android.thanos.module.common.R.id.action_search);
        binding.searchView.setMenuItem(item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            onRequestExport(null);
            return true;
        }

        if (item.getItemId() == R.id.action_import) {
            onRequestImport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (closeSearch()) {
            return;
        }
        super.onBackPressed();
    }

    protected boolean closeSearch() {
        if (binding.searchView.isSearchOpen()) {
            binding.searchView.closeSearch();
            return true;
        }
        return false;
    }

    // Null means all.
    private void onRequestExport(@Nullable String componentReplacementKey) {
        String[] items = getResources().getStringArray(github.tornaco.android.thanos.res.R.array.module_common_export_selections);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(github.tornaco.android.thanos.res.R.string.module_activity_trampoline_title_export_comp_replacements)
                .setSingleChoiceItems(items, -1,
                        (d, which) -> {
                            d.dismiss();
                            if (which == 0) {
                                exportToClipboard(componentReplacementKey);
                            } else {
                                if (OsUtils.isTOrAbove()) {
                                    ActivityTrampolineActivityPermissionRequester.exportToFileTOrAboveChecked(componentReplacementKey, this);
                                } else {
                                    ActivityTrampolineActivityPermissionRequester.exportToFileTBelowChecked(componentReplacementKey, this);
                                }
                            }
                        }).create();
        dialog.show();
    }

    private void exportToClipboard(@Nullable String componentReplacementKey) {
        viewModel.exportToClipboard(componentReplacementKey);
    }

    @RequiresPermission({
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
    })
    void exportToFileTOrAbove(@Nullable String componentReplacementKey) {
        if (OsUtils.isQOrAbove()) {
            exportToFileQAndAbove(componentReplacementKey);
        } else {
            exportToFileQBelow(componentReplacementKey);
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportToFileTBelow(@Nullable String componentReplacementKey) {
        if (OsUtils.isQOrAbove()) {
            exportToFileQAndAbove(componentReplacementKey);
        } else {
            exportToFileQBelow(componentReplacementKey);
        }
    }

    private void exportToFileQBelow(@Nullable String componentReplacementKey) {
        Intent intent = pickSingleDirIntent(thisActivity());
        intent.putExtra("componentReplacementKey", componentReplacementKey);
        int reqCode = _REQ_ID.incrementAndGet();
        requestCodeExportMapping.put(reqCode, componentReplacementKey);
        startActivityForResult(intent, reqCode);
    }

    private void exportToFileQAndAbove(@Nullable String componentReplacementKey) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // you can set file mime-type
        intent.setType("*/*");
        // default file name
        String expFileNameWithExt = "Replacements-" + DateUtils.formatForFileName(System.currentTimeMillis()) + ".json";
        intent.putExtra(Intent.EXTRA_TITLE, expFileNameWithExt);
        intent.putExtra("componentReplacementKey", componentReplacementKey);
        int reqCode = _REQ_ID.incrementAndGet();
        requestCodeExportMappingQ.put(reqCode, componentReplacementKey);
        startActivityForResult(intent, reqCode);
    }

    private void onRequestImport() {
        String[] items = getResources().getStringArray(github.tornaco.android.thanos.res.R.array.module_common_import_selections);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(github.tornaco.android.thanos.res.R.string.module_activity_trampoline_title_import_comp_replacements)
                .setSingleChoiceItems(items, -1,
                        (d, which) -> {
                            d.dismiss();
                            if (which == 0) {
                                importFromClipboard();
                            } else {
                                if (OsUtils.isTOrAbove()) {
                                    ActivityTrampolineActivityPermissionRequester.importFromFileTOrAboveChecked(this);
                                } else {
                                    ActivityTrampolineActivityPermissionRequester.importFromFileTBelowChecked(this);
                                }
                            }
                        }).create();
        dialog.show();
    }

    private void importFromClipboard() {
        viewModel.importFromClipboard();
    }

    @RequiresPermission({
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
    })
    void importFromFileTOrAbove() {
        if (OsUtils.isQOrAbove()) {
            importToFileQAndAbove();
        } else {
            importToFileQBelow();
        }
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    void importFromFileTBelow() {
        if (OsUtils.isQOrAbove()) {
            importToFileQAndAbove();
        } else {
            importToFileQBelow();
        }
    }

    private void importToFileQAndAbove() {
        importToFileQBelow();
    }

    private void importToFileQBelow() {
        IntentUtils.startFilePickerActivityForRes(this, REQUEST_CODE_PICK_IMPORT_PATH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCodeExportMapping.indexOfKey(requestCode) >= 0 && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(data);
            File file = Utils.getFileForUri(files.get(0));
            String componentReplacementKey = requestCodeExportMapping.get(requestCode);
            XLog.d("componentReplacementKey=%s", componentReplacementKey);
            onExportFilePick(file, componentReplacementKey);
        }

        if (requestCode == REQUEST_CODE_PICK_IMPORT_PATH && resultCode == Activity.RESULT_OK) {
            onImportFilePick(data);
        }

        if (requestCodeExportMappingQ.indexOfKey(requestCode) >= 0 && resultCode == Activity.RESULT_OK) {
            String componentReplacementKey = requestCodeExportMapping.get(requestCode);
            onExportFilePickQ(data, componentReplacementKey);
        }
    }

    private void onExportFilePick(final File file, @Nullable String componentReplacementKey) {
        try {
            String expFileNameWithExt = "Replacements-" + DateUtils.formatForFileName(System.currentTimeMillis()) + ".json";
            File expFile = new File(file, expFileNameWithExt);
            //noinspection UnstableApiUsage
            Files.createParentDirs(expFile);
            //noinspection UnstableApiUsage
            viewModel.exportToFile(Files.asByteSink(expFile).openStream(), componentReplacementKey);
        } catch (IOException e) {
            Toast.makeText(thisActivity(), Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
        }
    }

    private void onExportFilePickQ(Intent data, String componentReplacementKey) {
        if (data == null) {
            XLog.e("onExportFilePickQ, No data.");
            return;
        }

        Uri fileUri = data.getData();

        if (fileUri == null) {
            Toast.makeText(thisActivity(), "fileUri == null", Toast.LENGTH_LONG).show();
            XLog.e("onExportFilePickQ, No fileUri.");
            return;
        }

        XLog.d("onExportFilePickQ, fileUri == %s", fileUri);
        XLog.d("onExportFilePickQ, componentReplacementKey=%s", componentReplacementKey);

        onExportFilePickAvailableQ(fileUri, componentReplacementKey);
    }

    private void onExportFilePickAvailableQ(@NonNull Uri fileUri, @Nullable String componentReplacementKey) {
        try {
            OutputStream os = Objects.requireNonNull(thisActivity()).getContentResolver().openOutputStream(fileUri);
            viewModel.exportToFile(os, componentReplacementKey);
        } catch (IOException e) {
            XLog.e(e);
            Toast.makeText(thisActivity(), Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
        }
    }

    private void onImportFilePick(final Intent data) {
        if (data == null) {
            XLog.e("No data.");
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            Toast.makeText(thisActivity(), "uri == null", Toast.LENGTH_LONG).show();
            XLog.e("No uri.");
            return;
        }
        viewModel.importFromFile(uri);
    }

    public static TrampolineViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelProvider.AndroidViewModelFactory factory = ViewModelProvider.AndroidViewModelFactory
                .getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(TrampolineViewModel.class);
    }

    private class EmojiExcludeFilter implements InputFilter {

        @Override
        public CharSequence filter(CharSequence source,
                                   int start,
                                   int end,
                                   Spanned dest,
                                   int dstart,
                                   int dend) {
            for (int i = start; i < end; i++) {
                int type = Character.getType(source.charAt(i));
                if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                    return "";
                }
            }
            return null;
        }
    }
}
