package com.hackathon.digitallifevoice;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.hackathon.digitallifevoice.api.DigitalLifeController;
import com.hackathon.digitallifevoice.api.DigitalLifeDevice;
import com.hackathon.digitallifevoice.data.Action;
import com.hackathon.digitallifevoice.data.DatabaseHandler;

import java.util.List;

/**
 * Created by SWBRADSH on 2/20/2015.
 */
public class OKGoogleService extends AccessibilityService {

    static final String TAG = "OKGoogleService";

    private String sSearchString = null;
    private boolean bFoundSearch = false;
    private boolean bSentEvent = false;
    private DigitalLifeController dlc;

    public String getAppId() { return PreferenceManager.getDefaultSharedPreferences(this).getString("appid", "OE_69B642D383971614_1"); }

    public String getAuthToken() { return PreferenceManager.getDefaultSharedPreferences(this).getString("authtoken", "");    }
    public String getRequestToken() { return PreferenceManager.getDefaultSharedPreferences(this).getString("requesttoken", "");    }
    public String getGatewayGuid() { return PreferenceManager.getDefaultSharedPreferences(this).getString("gatewayguid", "");    }


    private String getEventType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
        }
        return "default";
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }

    private Action getActionForVoiceText(String voiceText) {
        DatabaseHandler db = new DatabaseHandler(this);
        List<Action> actions = db.getAllActions();
        Action matchedAction = null;
        for (Action a : actions) {
            if (a.getVoiceCommand().toLowerCase().equals(voiceText.toLowerCase())){
                return a;
            }
        }
        return null;
    }

    private void processSearch(String searchText) {

        Action targetAction = getActionForVoiceText(searchText);
        if (targetAction != null) {
            String toastText = "AT&T Digital Life Action received";
            Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_SHORT);
            toast.show();

            DigitalLifeDevice dld = new DigitalLifeDevice();

            dld.setDeviceID(targetAction.getDeviceGuid());

            dlc = DigitalLifeController.getInstance();
            dlc.init(getAppId(), "https://systest.digitallife.att.com");
            dlc.setGatewayGUID(this.getGatewayGuid());
            dlc.setAuthToken(this.getAuthToken());
            dlc.setRequestToken(this.getRequestToken());
            dlc.updateDevice(targetAction.getDeviceGuid(), targetAction.getLabel(), targetAction.getOperation());

        }
        bSentEvent = false;

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName().equals("com.google.android.googlequicksearchbox")) {
            if (event.getClassName().equals("android.widget.EditText")) {
                if ((!bSentEvent) && (event.getText().size() > 0)) {
                    bFoundSearch = true;
                    this.sSearchString = getEventText(event);
                }
            }
           // if (event.getClassName().toString().contains("com.android.org.chromium")) {
                if (bFoundSearch) {
                    bFoundSearch = false;
                    bSentEvent = true;
                    processSearch(this.sSearchString);
                }
          //  }


            Log.v(TAG, String.format(
                    "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                    getEventType(event), event.getClassName(), event.getPackageName(),
                    event.getEventTime(), getEventText(event)));
        }
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

}