/*
 * Copyright (C) 2019 The Android Open Source Project
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
 */

package android.support.customtabs.trusted.sharing;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a Web Share Target associated with a Trusted Web Activity.
 *
 * The structure of a ShareTarget object follows the specification [1] of the "share_target" object
 * within web manifest json, with the following exceptions:
 * - The "action" field specifies the full URL of the Share Target, and not only the path.
 * - There is no "url" field in the "params" object, since urls are not supplied separately from
 * text in Android's ACTION_SEND and ACTION_SEND_MULTIPLE intents.
 *
 * [1] https://wicg.github.io/web-share-target/level-2/
 */
public final class ShareTarget  {
    /** Bundle key for {@link #action} */
    public static final String KEY_ACTION = "androidx.browser.trusted.sharing.KEY_ACTION";

    /** Bundle key for {@link #method} */
    public static final String KEY_METHOD = "androidx.browser.trusted.sharing.KEY_METHOD";

    /** Bundle key for {@link #encodingType} */
    public static final String KEY_ENC_TYPE = "androidx.browser.trusted.sharing.KEY_ENC_TYPE";

    /** Bundle key for {@link #params} */
    public static final String KEY_PARAMS = "androidx.browser.trusted.sharing.KEY_PARAMS";

    /** URL of the Web Share Target. */
    @NonNull
    public final String action;

    /**
     * HTTP request method for the Web Share Target. Can be "GET" or "POST"; the default is "GET".
     */
    @Nullable
    public final String method;

    /**
     * Specifies how the share data should be encoded in the body of a POST request.
     */
    @Nullable
    public final String encodingType;

    /**
     * Contains the parameter names to be used for various pieces of data, see {@link Params}.
     */
    @Nullable
    public final Params params;

    /**
     * Constructor.
     * @param action
     * @param method
     * @param encodingType
     * @param params
     */
    public ShareTarget(@NonNull String action, @Nullable String method,
            @Nullable String encodingType, @Nullable Params params) {
        this.action = action;
        this.method = method;
        this.encodingType = encodingType;
        this.params = params;
    }

    /** Packs the object into a {@link Bundle}. */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_METHOD, method);
        bundle.putString(KEY_ENC_TYPE, encodingType);
        if (params != null) {
            bundle.putBundle(KEY_PARAMS, params.toBundle());
        }
        return bundle;
    }

    /** Unpacks the object from a {@link Bundle}. */
    @NonNull
    public static ShareTarget fromBundle(@NonNull Bundle bundle) {
        return new ShareTarget(bundle.getString(KEY_ACTION),
                bundle.getString(KEY_METHOD),
                bundle.getString(KEY_ENC_TYPE),
                Params.fromBundle(bundle.getBundle(KEY_PARAMS)));
    }

    /** Contains parameter names to be used for various pieces of data being shared. */
    public static class Params {
        /** Bundle key for {@link #title}. */
        public static final String KEY_TITLE = "androidx.browser.trusted.sharing.KEY_TITLE";

        /** Bundle key for {@link #text}. */
        public static final String KEY_TEXT = "androidx.browser.trusted.sharing.KEY_TEXT";

        /** Bundle key for {@link #files}. */
        public static final String KEY_FILES = "androidx.browser.trusted.sharing.KEY_FILES";

        /** The name of the query parameter used for the title of the document being shared. */
        @Nullable
        public final String title;

        /** The name of the query parameter used for the body of the document being shared. */
        @Nullable
        public final String text;

        /**
         * Defines form fields for the files being shared, see {@link FileFormField}.
         * Web Share Target can have multiple form fields associated with different mime-types.
         * If a file passes the mime-type filters of several {@link FileFormField}s,
         * the one that has the lowest index in this list is picked; see [1] for details.
         *
         * [1] https://wicg.github.io/web-share-target/level-2/#launching-the-web-share-target
         */
        @Nullable
        public final List<FileFormField> files;

        public Params(@Nullable String title, @Nullable String text,
                @Nullable List<FileFormField> files) {
            this.title = title;
            this.text = text;
            this.files = files;
        }

        /** Packs the object into a {@link Bundle}. */
        @NonNull
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_TITLE, title);
            bundle.putString(KEY_TEXT, text);
            if (files != null) {
                ArrayList<Bundle> fileBundles = new ArrayList<>();
                for (FileFormField file : files) {
                    fileBundles.add(file.toBundle());
                }
                bundle.putParcelableArrayList(KEY_FILES, fileBundles);
            }

            return bundle;
        }

        /** Unpacks the object from a {@link Bundle}. */
        @Nullable
        public static Params fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            List<FileFormField> files = null;
            List<Bundle> fileBundles = bundle.getParcelableArrayList(KEY_FILES);
            if (fileBundles != null) {
                files = new ArrayList<>();
                for (Bundle fileBundle : fileBundles) {
                    files.add(FileFormField.fromBundle(fileBundle));
                }
            }
            return new Params(bundle.getString(KEY_TITLE), bundle.getString(KEY_TEXT),
                    files);
        }
    }

    /** Defines a form field for sharing files. */
    public static class FileFormField {
        /** Bundle key for {@link #name}. */
        public static final String KEY_NAME = "androidx.browser.trusted.sharing.FILE_NAME";

        /** Bundle key for {@link #acceptedTypes}. */
        public static final String KEY_ACCEPTED_TYPES =
                "androidx.browser.trusted.sharing.FILE_ACCEPTED_TYPES";

        /** Name of the form field. */
        @NonNull
        public final String name;

        /** List of MIME types or file extensions to be sent in this query parameter. */
        @NonNull
        public final List<String> acceptedTypes;

        public FileFormField(@NonNull String name, @NonNull List<String> acceptedTypes) {
            this.name = name;
            this.acceptedTypes = acceptedTypes;
        }

        /** Packs the object into a {@link Bundle}. */
        @NonNull
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_NAME, name);
            bundle.putStringArrayList(KEY_ACCEPTED_TYPES, new ArrayList<>(acceptedTypes));
            return bundle;
        }

        /** Unpacks the object from a {@link Bundle}. */
        @Nullable
        public static FileFormField fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            String name = bundle.getString(KEY_NAME);
            ArrayList<String> acceptedTypes = bundle.getStringArrayList(KEY_ACCEPTED_TYPES);
            if (name == null || acceptedTypes == null) {
                return null;
            }
            return new FileFormField(name, acceptedTypes);
        }
    }
}
