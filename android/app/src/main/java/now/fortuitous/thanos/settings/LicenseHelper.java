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

package now.fortuitous.thanos.settings;

import android.app.Activity;

import java.util.Objects;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.BSD2ClauseLicense;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.licenses.MozillaPublicLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class LicenseHelper {

    public static void showLicenseDialog(Activity activity) {
        final Notices notices = new Notices();

        notices.addNotice(
                new Notice(
                        "Lombok",
                        "https://projectlombok.org/",
                        " Copyright © 2009-2018 The Project Lombok Authors",
                        new MITLicense()));

        notices.addNotice(
                new Notice(
                        "guava",
                        "https://github.com/google/guava",
                        null,
                        new MITLicense()));

        notices.addNotice(
                new Notice(
                        "retrofit",
                        "https://github.com/square/retrofit",
                        "Copyright 2013 Square, Inc.",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "RxJava",
                        "https://github.com/ReactiveX/RxJava",
                        "Copyright (c) 2016-present, RxJava Contributors.",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "RxAndroid",
                        "https://github.com/ReactiveX/RxAndroid",
                        "Copyright 2015 The RxAndroid authors",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "RecyclerView-FastScroll",
                        "https://github.com/timusus/RecyclerView-FastScroll",
                        null,
                        new MITLicense()));

        notices.addNotice(
                new Notice(
                        "glide",
                        "https://github.com/bumptech/glide",
                        null,
                        new GlideLicense()));

        notices.addNotice(
                new Notice(
                        "material-searchview",
                        "https://github.com/Shahroz16/material-searchview",
                        " Copyright (C) 2016 Tim Malseed",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "easy-rules",
                        "https://github.com/j-easy/easy-rules",
                        "Copyright (c) 2019 Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)",
                        new MITLicense()));

        notices.addNotice(
                new Notice(
                        "Xposed",
                        "https://github.com/rovo89/Xposed",
                        null,
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "MaterialBadgeTextView",
                        "https://github.com/matrixxun/MaterialBadgeTextView",
                        null,
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "FuzzyDateFormatter",
                        "https://github.com/izacus/FuzzyDateFormatter",
                        "Copyright 2015 Jernej Virag",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "MaterialSearchView",
                        "https://github.com/MiguelCatalan/MaterialSearchView",
                        "Copyright 2015 Miguel Catalan Bañuls",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "android_native_code_view",
                        "https://github.com/vic797/android_native_code_view",
                        "Copyright 2017 Victor Campos",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "NoNonsense-FilePicker",
                        "https://github.com/spacecowboy/NoNonsense-FilePicker",
                        null,
                        new MozillaPublicLicense20()));

        notices.addNotice(
                new Notice(
                        "ApkBuilder",
                        "https://github.com/hyb1996/Auto.js-ApkBuilder",
                        null,
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "apk-parser",
                        "https://github.com/hsiafan/apk-parser",
                        null,
                        new BSD2ClauseLicense()));

        notices.addNotice(
                new Notice(
                        "XLog",
                        "https://github.com/elvishew/xLog",
                        "Copyright 2018 Elvis Hew",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "libsu",
                        "https://github.com/topjohnwu/libsu",
                        null,
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "MaterialProgressBar",
                        "https://github.com/zhanghai/MaterialProgressBar",
                        "Copyright 2015 Hai Zhang",
                        new ApacheSoftwareLicense20()));

        notices.addNotice(
                new Notice(
                        "time-duration-picker",
                        "https://github.com/svenwiegand/time-duration-picker",
                        null,
                        new MITLicense()));

        notices.addNotice(
                new Notice(
                        "CodeView",
                        "https://github.com/AmrDeveloper/CodeView",
                        "Copyright (c) 2020 - Present Amr Hesham",
                        new MITLicense()));

        new LicensesDialog.Builder(Objects.requireNonNull(activity))
                .setNotices(notices)
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }
}
