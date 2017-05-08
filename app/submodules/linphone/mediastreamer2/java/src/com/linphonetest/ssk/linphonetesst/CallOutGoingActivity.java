package com.linphonetest.ssk.linphonetesst;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.Reason;
import org.linphone.mediastream.Log;

import java.util.ArrayList;
import java.util.List;

import main.java.com.linphonetest.ssk.linphonetesst.ContactsManager;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneContact;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneManager;
import main.java.com.linphonetest.ssk.linphonetesst.LinphonePreferences;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneUtils;
import main.java.com.linphonetest.ssk.linphonetesst.MainActivity;

public class CallOutGoingActivity extends AppCompatActivity {

    private Button hangUp;
    private TextView outGoingName;
    private LinphoneCoreListenerBase mListener;
    private LinphoneCall mCall;
    private CallOutGoingActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_out_going);
        instance = this;

        outGoingName = (TextView) findViewById(R.id.outgoingcall);
        hangUp = (Button) findViewById(R.id.outgoing_hang_up);
        hangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decline();
            }
        });
        mListener = new LinphoneCoreListenerBase() {
            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                if (call == mCall && LinphoneCall.State.Connected == state) {
                    if (!MainActivity.isInstanciated()) {
                        return;
                    }
                    MainActivity.instance().startIncallActivity(mCall);
                    finish();
                    return;
                } else if (state == LinphoneCall.State.Error) {
                    // Convert LinphoneCore message for internalization
                    if (message != null && call.getErrorInfo().getReason() == Reason.Declined) {
                        displayCustomToast(getString(R.string.error_call_declined), Toast.LENGTH_SHORT);
                    } else if (message != null && call.getErrorInfo().getReason() == Reason.NotFound) {
                        displayCustomToast(getString(R.string.error_user_not_found), Toast.LENGTH_SHORT);
                    } else if (message != null && call.getErrorInfo().getReason() == Reason.Media) {
                        displayCustomToast(getString(R.string.error_incompatible_media), Toast.LENGTH_SHORT);
                    } else if (message != null && call.getErrorInfo().getReason() == Reason.Busy) {
                        displayCustomToast(getString(R.string.error_user_busy), Toast.LENGTH_SHORT);
                    } else if (message != null) {
                        displayCustomToast(getString(R.string.error_unknown) + " - " + message, Toast.LENGTH_SHORT);
                    }
                }

                if (LinphoneManager.getLc().getCallsNb() == 0) {
                    finish();
                    return;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }

        // Only one call ringing at a time is allowed
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
            List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
            for (LinphoneCall call : calls) {
                LinphoneCall.State cstate = call.getState();
                if (LinphoneCall.State.OutgoingInit == cstate || LinphoneCall.State.OutgoingProgress == cstate
                        || LinphoneCall.State.OutgoingRinging == cstate || LinphoneCall.State.OutgoingEarlyMedia == cstate) {
                    mCall = call;
                    break;
                }
                if (LinphoneCall.State.StreamsRunning == cstate) {
                    if (!MainActivity.isInstanciated()) {
                        return;
                    }
                    MainActivity.instance().startIncallActivity(mCall);
                    finish();
                }
            }
        }
        if (mCall == null) {
            Log.e("Couldn't find outgoing call");
//            LinphoneActivity.instance().goToDialerFragment();
            finish();
            return;
        }

        LinphoneAddress address = mCall.getRemoteAddress();
        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        if (contact != null) {
//            LinphoneUtils.setImagePictureFromUri(this, contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
            outGoingName.setText(contact.getFullName());
        } else {
            outGoingName.setText(LinphoneUtils.getAddressDisplayName(address));
        }
//        number.setText(address.asStringUriOnly());
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestCallPermissions();
    }

    @Override
    protected void onPause() {
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.removeListener(mListener);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public void displayCustomToast(final String message, final int duration) {
//        LayoutInflater inflater = getLayoutInflater();
//        View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toastRoot));
//
//        TextView toastText = (TextView) layout.findViewById(R.id.toastMessage);
//        toastText.setText(message);

        final Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(duration);
//        toast.setView(layout);
        toast.setText(message);
        toast.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
            LinphoneManager.getLc().terminateCall(mCall);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    private void decline() {
        LinphoneManager.getLc().terminateCall(mCall);
        finish();
    }

    private void checkAndRequestCallPermissions() {
        ArrayList<String> permissionsList = new ArrayList<String>();

        int recordAudio = getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
//        Log.i("[Permission] Record audio permission is " + (recordAudio == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
        int camera = getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
//        Log.i("[Permission] Camera permission is " + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            if (LinphonePreferences.instance().firstTimeAskingForPermission(Manifest.permission.RECORD_AUDIO) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
//                Log.i("[Permission] Asking for record audio");
                permissionsList.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        if (LinphonePreferences.instance().shouldInitiateVideoCall() || LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()) {
            if (camera != PackageManager.PERMISSION_GRANTED) {
                if (LinphonePreferences.instance().firstTimeAskingForPermission(Manifest.permission.CAMERA) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//                    Log.i("[Permission] Asking for camera");
                    permissionsList.add(Manifest.permission.CAMERA);
                }
            }
        }

        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i("[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
        }
    }


}



