package com.example.ibanez_xiphos.photogallery.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {

    private static final String TAG = "PhotoPageActivity";

    private PhotoPageFragment mPhotoPageFragment;

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        mPhotoPageFragment = PhotoPageFragment.newInstance(getIntent().getData());
        return mPhotoPageFragment;
    }

    @Override
    public void onBackPressed() {
        if (mPhotoPageFragment.webView.canGoBack()) {
            mPhotoPageFragment.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
