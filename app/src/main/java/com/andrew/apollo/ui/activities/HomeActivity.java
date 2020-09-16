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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.andrew.apollo.R;
import com.andrew.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends AppCompatBase {

    @SuppressLint("InlinedApi")
    private static final String[] PERMISSIONS = {READ_EXTERNAL_STORAGE, READ_PHONE_STATE};
    private static final int REQ_CHECK_PERM = 1;


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(PERMISSIONS[0]) != PERMISSION_GRANTED || checkSelfPermission(PERMISSIONS[1]) != PERMISSION_GRANTED) {
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
        // check if permissions are granted
        if (requestCode == REQ_CHECK_PERM) {
            boolean permDenied = false;
            for (int i = 0; i < PERMISSIONS.length; i++) {
                if (permissions[i].equals(PERMISSIONS[i]) && grantResults[i] == PERMISSION_DENIED) {
                    permDenied = true;
                    break;
                }
            }
            if (permDenied) {
                finish();
            } else {
                init();
            }
        }
    }

    private void init() {
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_base_content, new MusicBrowserPhoneFragment()).commit();
    }
}