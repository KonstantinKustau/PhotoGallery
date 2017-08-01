package com.example.ibanez_xiphos.photogallery.ui;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ibanez_xiphos.photogallery.R;

public class PhotoPageFragment extends VisibleFragment {

    private static final String TAG = "PhotoPageFragment";
    private static final String ARG_URI = "photo_page_url";

    WebView webView;
    private Uri mUri;
    private ProgressBar mProgressBar;

    static PhotoPageFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUri = getArguments().getParcelable(ARG_URI);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);

        final TextView titleTextView = (TextView) v.findViewById(R.id.titleTextView);

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_photo_page_progress_bar);
        mProgressBar.setMax(100);

        webView = (WebView) v.findViewById(R.id.fragment_photo_page_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            public void onReceivedTitle(WebView webView, String title) {
                titleTextView.setText(title);
            }

        });
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "the method worked");
                return false;
            }
        });
        webView.loadUrl(mUri.toString());

        return v;
    }
}
