package com.cher.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private DrawerLayout drawerLayout;
    private EditText editUrl;
    private EditText editPort;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "CheroPrefs";
    private static final String KEY_URL = "url";
    private static final String KEY_PORT = "port";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        drawerLayout = findViewById(R.id.drawer_layout);
        editUrl = findViewById(R.id.edit_url);
        editPort = findViewById(R.id.edit_port);
        Button btnSave = findViewById(R.id.btn_save);
        View btnMenu = findViewById(R.id.btn_menu);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupWebView();

        String savedUrl = sharedPreferences.getString(KEY_URL, "https://google.com");
        String savedPort = sharedPreferences.getString(KEY_PORT, "");

        editUrl.setText(savedUrl);
        editPort.setText(savedPort);

        loadUrl(savedUrl, savedPort);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnSave.setOnClickListener(v -> {
            String url = editUrl.getText().toString();
            String port = editPort.getText().toString();

            sharedPreferences.edit()
                .putString(KEY_URL, url)
                .putString(KEY_PORT, port)
                .apply();

            loadUrl(url, port);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Fix for "disallowed_useragent" error when using Google OAuth
        String userAgent = webSettings.getUserAgentString();
        // Remove "Version/X.X" and "; wv" from the user agent string
        userAgent = userAgent.replaceAll("Version/[^\\s]+", "").replace("; wv", "");
        webSettings.setUserAgentString(userAgent);

        webView.setWebViewClient(new WebViewClient());
    }

    private void loadUrl(String url, String port) {
        if (url == null || url.isEmpty()) return;

        String fullUrl = url;
        if (!fullUrl.contains("://")) {
            fullUrl = "http://" + fullUrl;
        }

        if (!port.isEmpty()) {
            int protoIndex = fullUrl.indexOf("://");
            String protocol = fullUrl.substring(0, protoIndex + 3);
            String rest = fullUrl.substring(protoIndex + 3);

            int slashIndex = rest.indexOf("/");
            if (slashIndex != -1) {
                String host = rest.substring(0, slashIndex);
                String path = rest.substring(slashIndex);
                if (!host.contains(":")) {
                    fullUrl = protocol + host + ":" + port + path;
                }
            } else {
                if (!rest.contains(":")) {
                    fullUrl = protocol + rest + ":" + port;
                }
            }
        }

        webView.loadUrl(fullUrl);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
