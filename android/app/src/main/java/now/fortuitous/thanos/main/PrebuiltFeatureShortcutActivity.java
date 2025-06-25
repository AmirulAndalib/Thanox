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

package now.fortuitous.thanos.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.elvishew.xlog.XLog;

import java.util.Objects;

import github.tornaco.android.thanos.R;
import github.tornaco.android.thanos.util.BitmapUtil;
import github.tornaco.android.thanos.util.ShortcutReceiver;

public class PrebuiltFeatureShortcutActivity extends Activity {
    private static final String KEY_FEATURE_ID = "key_feature_id";

    public static Intent createIntent(Context context, int featureId) {
        XLog.d("PrebuiltFeatureShortcutActivity, createIntent: " + featureId);
        Intent intent = new Intent(context, PrebuiltFeatureShortcutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_FEATURE_ID, featureId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int featId = getIntent().getIntExtra(KEY_FEATURE_ID, Integer.MIN_VALUE);
        XLog.d("PrebuiltFeatureShortcutActivity, featId: " + featId);
        if (PrebuiltFeatureIds.INSTANCE.isValidId(featId)) {
            new PrebuiltFeatureLauncher(this, () -> null).launch(featId);
        }
        finish();
    }

    public static class ShortcutHelper {

        public static void addShortcut(Context context, FeatureItem tile) {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), tile.getPackedIconRes(), null);
                LayerDrawable ld = (LayerDrawable) drawable;
                Drawable layer = ld.findDrawableByLayerId(R.id.settings_ic_foreground);
                if (layer != null) {
                    layer.setTint(ContextCompat.getColor(context, tile.getThemeColor()));
                    ld.setDrawableByLayerId(R.id.settings_ic_foreground, layer);
                }
                Bitmap resource = BitmapUtil.getBitmap(context, drawable);
                Intent shortcutInfoIntent = PrebuiltFeatureShortcutActivity.createIntent(context, tile.getId());
                shortcutInfoIntent.setAction(Intent.ACTION_VIEW);
                ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(context, "Shortcut-of-thanox-for-feature-" + tile.getId())
                        .setIcon(IconCompat.createWithBitmap(Objects.requireNonNull(resource)))
                        .setShortLabel(context.getString(tile.getTitleRes()))
                        .setIntent(shortcutInfoIntent)
                        .build();
                ShortcutManagerCompat.requestPinShortcut(context, info, ShortcutReceiver.getPinRequestAcceptedIntent(context).getIntentSender());
            }
        }
    }

}
