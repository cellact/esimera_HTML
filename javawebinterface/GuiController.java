package com.arnacon.arnaconapp.webinterface;

import android.content.Context;

import com.arnacon.arnaconapp.DataSaveHelper;

public class GuiController {

    private static GuiController instance;
    private WebAppInterface webAppInterface;

    public GuiController(Context c) throws Exception {
    }

    public static synchronized GuiController getInstance(Context context) throws Exception {
        if (instance == null) {
            instance = new GuiController(context);
        }
        return instance;
    }

    public void setWebAppInterface(WebAppInterface webAppInterface) {
        this.webAppInterface = webAppInterface;
    }

    public void ring(String to) {
        webAppInterface.ring(to);
    }
    public void callStarted(String from) {
        webAppInterface.callStarted(from);
    }
    public void callEnded() {
        webAppInterface.callEnded();
    }
    public void receivingCall(String from) {
        webAppInterface.receivingCall(from);
    }
}
