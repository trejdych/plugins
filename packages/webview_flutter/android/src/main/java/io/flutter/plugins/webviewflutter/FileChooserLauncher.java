// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.*;

import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;
import android.util.Log;
import static io.flutter.plugins.webviewflutter.Constants.ACTION_FILE_CHOOSER_FINISHED;
import static io.flutter.plugins.webviewflutter.Constants.ACTION_REQUEST_CAMERA_PERMISSION_FINISHED;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_FILE_URI;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_SHOW_CAMERA_OPTION;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_SHOW_VIDEO_OPTION;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_TITLE;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_TYPE;

public class FileChooserLauncher extends BroadcastReceiver {

    private Context context;
    private String title;
    private String type;
    private boolean showCameraOption;
    private ValueCallback<Uri[]> filePathCallback;
    private FileChooserParams fileChooserParams;

    public FileChooserLauncher(
            Context context,
            String title,
            String type,
            boolean showCameraOption,
            ValueCallback<Uri[]> filePathCallback,
            FileChooserParams fileChooserParams) {
        this.context = context;
        this.title = title;
        this.type = type;
        this.showCameraOption = showCameraOption;
        this.filePathCallback = filePathCallback;
        this.fileChooserParams = fileChooserParams;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void start() {
        if (!showCameraOption || hasCameraPermission()) {
            showFileChooser();
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_REQUEST_CAMERA_PERMISSION_FINISHED);
            context.registerReceiver(this, intentFilter);

            Intent intent = new Intent(context, RequestCameraPermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private void showFileChooser() {
        IntentFilter intentFilter = new IntentFilter(ACTION_FILE_CHOOSER_FINISHED);
        context.registerReceiver(this, intentFilter);
        final String[] acceptTypes = fileChooserParams.getAcceptTypes();
        Intent intent = new Intent(context, FileChooserActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TYPE, acceptTypes);

        List<String> acceptTypesList = Arrays.asList(acceptTypes);
        intent.putExtra(
                EXTRA_SHOW_CAMERA_OPTION,
                acceptTypesList.contains("image/*") || acceptTypesList.contains("image/jpeg")
        );
        intent.putExtra(
                EXTRA_SHOW_VIDEO_OPTION,
                acceptTypesList.contains("video/*") || acceptTypesList.contains("video/mp4")
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_REQUEST_CAMERA_PERMISSION_FINISHED)) {
            context.unregisterReceiver(this);
            showFileChooser();
        } else if (intent.getAction().equals(ACTION_FILE_CHOOSER_FINISHED)) {
            String uriString = intent.getStringExtra(EXTRA_FILE_URI);
            Uri[] result = uriString != null ? new Uri[]{Uri.parse(uriString)} : null;
            filePathCallback.onReceiveValue(result);
            context.unregisterReceiver(this);
            filePathCallback = null;
        }
    }
}
