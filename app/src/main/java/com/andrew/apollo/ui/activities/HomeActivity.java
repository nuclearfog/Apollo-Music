/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import com.andrew.apollo.utils.MusicUtils;

import static android.Manifest.permission.ACCESS_MEDIA_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.andrew.apollo.utils.MusicUtils.REQUEST_DELETE_FILES;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends AppCompatBase {

    /**
     * request code for permission result
     */
    private static final int REQ_CHECK_PERM = 0x1139398F;

    /**
     * permissions needed for this app
     */
    private static final String[] PERMISSIONS;

    /**
     * audio
     */
    private MusicBrowserPhoneFragment fragment;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE, READ_PHONE_STATE, ACCESS_MEDIA_LOCATION};
        } else {
            PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE, READ_PHONE_STATE};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean permissionDenied = false;
            for (String permission : PERMISSIONS) {
                if (checkSelfPermission(permission) != PERMISSION_GRANTED) {
                    permissionDenied = true;
                }
            }
            if (permissionDenied) {
                requestPermissions(PERMISSIONS, REQ_CHECK_PERM);
            } else if (savedInstanceState == null) {
                init();
            }
        } else if (savedInstanceState == null) {
            init();
        }
    }


    @Override
    public View getContentView() {
        return View.inflate(this, R.layout.activity_base, null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // check if permissions are granted
        if (requestCode == REQ_CHECK_PERM) {
            boolean permDenied = false;
            int permCount = PERMISSIONS.length;
            for (int i = 0; i < permCount; i++) {
                if (permissions[i].equals(PERMISSIONS[i]) && grantResults[i] == PERMISSION_DENIED) {
                    permDenied = true;
                    break;
                }
            }
            if (permDenied) {
                Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_LONG).show();
                finish();
            } else {
                init();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
            MusicUtils.onPostDelete(this);
            fragment.refresh();
        }
    }

    /**
     * initialize fragment
     */
    private void init() {
        fragment = new MusicBrowserPhoneFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_base_content, fragment).commit();
    }
}