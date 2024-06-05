package com.arnacon.arnaconapp.webinterface;


import android.content.Context;

import com.arnacon.arnaconapp.arnacon.ArnaconSip;
import com.arnacon.arnaconapp.DataSaveHelper;

public class GuiHandler {
    private Context context;
    private static GuiHandler instance;
    private ArnaconSip arnaconSip;
    private GuiController guiController;
    private DataSaveHelper dataSaveHelper;

    public GuiHandler(Context context) throws Exception {
        this.context = context;
        arnaconSip = getArnaconSip(context);
        guiController = GuiController.getInstance(context);
        dataSaveHelper = DataSaveHelper.getInstance(context);

    }

    public static synchronized GuiHandler getInstance(Context context) throws Exception {
        if (instance == null) {
            instance = new GuiHandler(context);
        }
        return instance;
    }

    private ArnaconSip getArnaconSip(Context context) throws Exception {
        if (arnaconSip == null) {
            synchronized (GuiHandler.class) {
                if (arnaconSip == null) { // double-checked locking
                    arnaconSip = ArnaconSip.getInstance(context);
                }
            }
        }
        return arnaconSip;
    }

    public void call(String to) {
        arnaconSip.performSipInvite(to, onSuccess -> guiController.ring(to));
    }

    public void endCall() {
        arnaconSip.hangUp();
    }

    public void acceptCall() {
        arnaconSip.answer();
    }
}
