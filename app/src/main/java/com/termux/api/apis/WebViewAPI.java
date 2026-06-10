package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.JsonWriter;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/**
 * API for WebView-based content viewing.
 * Fix for #802.
 * Provides a simple in-app browser for viewing web content.
 */
public class WebViewAPI {

    private static final String LOG_TAG = "WebViewAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "open":
                openWebView(context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void openWebView(Context context, Intent intent) {
        Intent i = new Intent(context, WebViewActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        context.startActivity(i);
    }

    /**
     * Simple WebView activity for viewing web content.
     */
    public static class WebViewActivity extends AppCompatActivity {
        private WebView webView;
        private boolean done = false;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String url = getIntent().getStringExtra("url");
            if (url == null || url.isEmpty()) {
                url = "about:blank";
            }

            webView = new WebView(this);
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            boolean allowJavascript = "https".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme);
            webView.getSettings().setJavaScriptEnabled(allowJavascript);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            setContentView(webView);

            webView.loadUrl(url);
        }

        @Override
        public void onBackPressed() {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                done = true;
                finishAndRemoveTask();
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (webView != null) {
                webView.destroy();
                webView = null;
            }
        }
    }
}
