package com.arnacon.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import com.arnacon.myapplication.webinterface.GuiController;
import com.arnacon.myapplication.webinterface.WebAppInterface;

public class MainActivity extends AppCompatActivity {

    private GuiController guiController;
    private WebAppInterface webAppInterface;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        try {
            webAppInterface = new WebAppInterface(this, webView);
            guiController = GuiController.getInstance(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        guiController.setWebAppInterface(webAppInterface);


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.addJavascriptInterface(webAppInterface, "AndroidBridge");
            }
        });


        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.addJavascriptInterface(webAppInterface, "AndroidBridge");
        webView.loadUrl("https://htmltest-theta.vercel.app/background.html");


        WebView.setWebContentsDebuggingEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " +
                        consoleMessage.sourceId());
                return true;
            }
        });
    }
}
