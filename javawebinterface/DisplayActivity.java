package com.arnacon.arnaconapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.arnacon.arnaconapp.arnacon.ArnaconSip;
import com.arnacon.arnaconapp.webinterface.GuiController;
import com.arnacon.arnaconapp.webinterface.WebAppInterface;

public class DisplayActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 201;
    private static final String CHANNEL_ID = "CALL_NOTIFICATION_CHANNEL";
    private ArnaconSip arnaconSip;
    private GuiController guiController;
    private DataSaveHelper dataSaveHelper;
    private String ens;
    private WebView webView;
    private TextView tvENS;
    private WebAppInterface webAppInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DisplayActivity","Attempting");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        try {
            arnaconSip = ArnaconSip.getInstance(getApplicationContext());
            guiController = GuiController.getInstance(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        dataSaveHelper = DataSaveHelper.getInstance(getApplicationContext());


        ens = getIntent().getStringExtra("ENS");
        tvENS = findViewById(R.id.tvENS);
        if (ens != null) {
            tvENS.setText(ens);
            performSipRegister();
        } else {
            Toast.makeText(this, "ENS not found. Please try again.", Toast.LENGTH_SHORT).show();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(fcmMessageReceiver,
                new IntentFilter("com.arnacon.arnaconapp.FCM_MESSAGE"));

        FirebaseNotificationManager.setNotificationCallback(new FirebaseNotificationManager.NotificationCallback() {
            @Override
            public void onNewToken(String token) {
                sendFCMToken(token);
            }

            @Override
            public void onMessageReceived(String messageBody) {
                Log.d("DisplayActivity", "Received a notification: " + messageBody);
                performSipRegister();
            }

            @Override
            public void onError(Exception e) {
                Log.e("DisplayActivity", "FCM error: ", e);
            }
        });

        FirebaseNotificationManager.getFcmToken();
        createNotificationChannel();

        webView = findViewById(R.id.webview);
        try {
            webAppInterface = new WebAppInterface(this, webView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        guiController.setWebAppInterface(webAppInterface);

        requestPermissions();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.addJavascriptInterface(webAppInterface, "Android");
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.addJavascriptInterface(webAppInterface, "AndroidBridge");
        handleIntent(getIntent());

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

    private void requestPermissions() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.POST_NOTIFICATIONS};

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION || requestCode == REQUEST_POST_NOTIFICATIONS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        webView.loadUrl("https://htmltest-theta.vercel.app/background.html");
        if (intent != null && "INCOMING_CALL".equals(intent.getStringExtra("ACTION"))) {
            // Load the incoming call page in the WebView
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    view.loadUrl("javascript:controller.receivingCall('" + intent.getStringExtra("callerEns") + "');");
                }
            });
        }
    }

    private void sendFCMToken(String token) {
        arnaconSip.sendFCMToken(token);
    }

    private void performSipRegister() {
        Log.d("DispActivity","registering");
        arnaconSip.performSipRegister(ens, true);
    }

    private void deregisterAccount() {
        arnaconSip.deregisterAccount(() -> runOnUiThread(() -> {
            Toast.makeText(DisplayActivity.this, "Deregistered successfully", Toast.LENGTH_SHORT).show();
        }));
    }

    private BroadcastReceiver fcmMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("DisplayActivity", "Received FCM message: " + message);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fcmMessageReceiver);
        deregisterAccount();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Call Notifications";
            String description = "Channel for incoming call notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
