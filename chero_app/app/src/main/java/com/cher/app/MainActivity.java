package com.cher.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private CredentialManager credentialManager;
    private DrawerLayout drawerLayout;
    private EditText editUrl;
    private EditText editPort;
    private SharedPreferences sharedPreferences;
    private long lastBackPressTime = 0;

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
        credentialManager = CredentialManager.create(this);

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    if (System.currentTimeMillis() - lastBackPressTime < 2000) {
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                        lastBackPressTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Fix 'disallowed_useragent' by removing WebView-specific strings
        String userAgent = webSettings.getUserAgentString();
        if (userAgent != null) {
            userAgent = userAgent.replaceAll("Version/\\d+\\.\\d+\\s+", "");
            userAgent = userAgent.replace("; wv", "");
            webSettings.setUserAgentString(userAgent);
        }

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAuthInterface(), "AndroidAuth");

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_AUTO);
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            WebSettingsCompat.setWebAuthenticationSupport(webSettings, WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP);
        }
    }

    public class WebAuthInterface {
        @JavascriptInterface
        public void requestGoogleAuth() {
            runOnUiThread(() -> {
                GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.google_web_client_id))
                        .setAutoSelectEnabled(true)
                        .build();

                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

                credentialManager.getCredentialAsync(
                        MainActivity.this,
                        request,
                        null,
                        ContextCompat.getMainExecutor(MainActivity.this),
                        new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse result) {
                                if (result.getCredential() instanceof GoogleIdTokenCredential) {
                                    GoogleIdTokenCredential credential = (GoogleIdTokenCredential) result.getCredential();
                                    String idToken = credential.getIdToken();
                                    webView.evaluateJavascript("if(window.onGoogleTokenReceived) { window.onGoogleTokenReceived('" + idToken + "'); }", null);
                                }
                            }

                            @Override
                            public void onError(GetCredentialException e) {
                                Toast.makeText(MainActivity.this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        }
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

}
