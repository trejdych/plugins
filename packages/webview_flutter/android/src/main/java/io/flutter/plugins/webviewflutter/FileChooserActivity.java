// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static io.flutter.plugins.webviewflutter.Constants.ACTION_FILE_CHOOSER_FINISHED;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_FILE_URI;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_SHOW_CAMERA_OPTION;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_SHOW_VIDEO_OPTION;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_TITLE;
import static io.flutter.plugins.webviewflutter.Constants.EXTRA_TYPE;
import static io.flutter.plugins.webviewflutter.Constants.WEBVIEW_STORAGE_DIRECTORY;

public class FileChooserActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST_CODE = 12322;
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private Uri cameraImageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFileChooser(getIntent().getBooleanExtra(EXTRA_SHOW_CAMERA_OPTION, false),
                getIntent().getBooleanExtra(EXTRA_SHOW_VIDEO_OPTION, false));
    }

    private void showFileChooser(boolean enableCamera, boolean enableVideo) {
        Intent galleryIntent = createGalleryIntent();
        Intent takePictureIntent = enableCamera ? createCameraIntent() : null;
        Intent takeVideoIntent = enableVideo ? createVideoIntent() : null;
        Log.d("FileChooserActivity", "Enable video is " + enableVideo);
        if (galleryIntent == null && takePictureIntent == null) {
            // cannot open anything: cancel file chooser
            sendBroadcast(new Intent(ACTION_FILE_CHOOSER_FINISHED));
            finish();
        } else {
            ArrayList<Intent> intentArrayList = new ArrayList<>();

            if (takePictureIntent != null) intentArrayList.add(takePictureIntent);
            if (takeVideoIntent != null) intentArrayList.add(takeVideoIntent);

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(
                    Intent.EXTRA_INTENT, galleryIntent != null ? galleryIntent : takePictureIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, getIntent().getStringExtra(EXTRA_TITLE));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArrayList.toArray(new Intent[0]));

            startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
        }
    }

    private Intent createGalleryIntent() {
        Intent filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
        filesIntent.setType("*/*");
        ArrayList<String> mimeTypes = new ArrayList<>();
        for (String type : getIntent().getStringArrayExtra(EXTRA_TYPE)) {
            mimeTypes.add(getMimeType(type));
        }
        for (String type : mimeTypes) {
            Log.d("Log", "Accepted type is " + type);
        }
        filesIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray());

        return (filesIntent.resolveActivity(getPackageManager()) != null) ? filesIntent : null;
    }

    private Intent createVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 20971520L);//20*1024*1024
        if (takeVideoIntent.resolveActivity(getPackageManager()) == null) {
            return null;
        }
        return takeVideoIntent;
    }

    private Intent createCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            return null;
        }
        // Create the File where the photo should go
        cameraImageUri = getTempImageUri();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        return takePictureIntent;
    }

    private File getStorageDirectory() {
        File imageDirectory = new File(getCacheDir(), WEBVIEW_STORAGE_DIRECTORY);
        if (!imageDirectory.exists() && !imageDirectory.mkdir()) {
            Log.e("WEBVIEW", "Unable to create storage directory");
        }
        return imageDirectory;
    }

    private Uri getTempImageUri() {
        String imageFileName = "IMG-" + simpleDateFormat.format(new Date()) + ".jpg";
        File imageFile = new File(getStorageDirectory(), imageFileName);
        return FileProvider.getUriForFile(
                this, getApplicationContext().getPackageName() + ".generic.provider", imageFile);
    }

    private String getFileNameFromUri(Uri uri) {
        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    private Uri copyToLocalUri(Uri uri) {
        InputStream in = null;
        OutputStream out = null;
        try {
            File destination = new File(getStorageDirectory(), getFileNameFromUri(uri));
            in = getContentResolver().openInputStream(uri);
            out = new FileOutputStream(destination);

            int cnt = 0;
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                cnt += len;
            }

            return FileProvider.getUriForFile(
                    this, getApplicationContext().getPackageName() + ".generic.provider", destination);
        } catch (Exception e) {
            Log.e("WEBVIEW", "Unable to copy selected image", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Intent intent = new Intent(ACTION_FILE_CHOOSER_FINISHED);
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getDataString() != null) {
                    // result from file browser
                    final Uri uri = copyToLocalUri(data.getData());
                    intent.putExtra(EXTRA_FILE_URI, uri.toString());
                } else {
                    // result from camera
                    intent.putExtra(EXTRA_FILE_URI, cameraImageUri.toString());
                }
            }
            sendBroadcast(intent);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    static String getMimeType(String extension) {
        String type = null;
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.replace(".", "").toLowerCase());
        }
        if (type == null) {
            type = extension;
        }
        return type;
    }
}
