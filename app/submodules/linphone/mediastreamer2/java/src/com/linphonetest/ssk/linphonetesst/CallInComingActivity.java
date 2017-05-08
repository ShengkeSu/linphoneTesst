package com.linphonetest.ssk.linphonetesst;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

import java.util.List;

import main.compatibility.Compatibility;
import main.java.com.linphonetest.ssk.linphonetesst.ContactsManager;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneContact;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneManager;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneService;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneUtils;
import main.java.com.linphonetest.ssk.linphonetesst.MainActivity;

public class CallInComingActivity extends AppCompatActivity implements View.OnClickListener {

    private Button decline;
    private Button accept;
    private boolean isScreenActive;
    private boolean alreadyAcceptedOrDeniedCall;
    private LinphoneCall mCall;
    private LinphoneCoreListenerBase mListener;
    private TextView name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_incoming);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isScreenActive = Compatibility.isScreenOn(pm);
        name = (TextView) findViewById(R.id.fromname);
        decline = (Button) findViewById(R.id.decline);
        accept = (Button) findViewById(R.id.accept);
        decline.setOnClickListener(this);
        accept.setOnClickListener(this);

        mListener = new LinphoneCoreListenerBase(){
            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                if (call == mCall && LinphoneCall.State.CallEnd == state) {
                    finish();
                }
                if (state == LinphoneCall.State.StreamsRunning) {
                    // The following should not be needed except some devices need it (e.g. Galaxy S).
                    LinphoneManager.getLc().enableSpeaker(LinphoneManager.getLc().isSpeakerEnabled());
                }
            }
        };


    }
    @Override
    protected void onResume() {
        super.onResume();
//        instance = this;
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }

        alreadyAcceptedOrDeniedCall = false;
        mCall = null;

        // Only one call ringing at a time is allowed
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
            List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
            for (LinphoneCall call : calls) {
                if (LinphoneCall.State.IncomingReceived == call.getState()) {
                    mCall = call;
                    break;
                }
            }
        }
        if (mCall == null) {
            //The incoming call no longer exists.
//            Log.d("Couldn't find incoming call");
            android.util.Log.i("aaa","Couldn't find incoming call");
            finish();
            return;
        }


        LinphoneAddress address = mCall.getRemoteAddress();
        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        if (contact != null) {
//            LinphoneUtils.setImagePictureFromUri(this, contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
            name.setText(contact.getFullName());
        } else {
            name.setText(LinphoneUtils.getAddressDisplayName(address));
        }
//        number.setText(address.asStringUriOnly());
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
            LinphoneManager.getLc().terminateCall(mCall);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.decline:
                if(isScreenActive) {
                    decline();
                } else {
                    accept.setVisibility(View.GONE);
                    //滑动解锁
//                    acceptUnlock.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.accept:
                if(isScreenActive) {
                    answer();
                } else {
                    decline.setVisibility(View.GONE);
                    //滑动接听
//                    acceptUnlock.setVisibility(View.VISIBLE);

                }
                break;
        }
    }
    private void decline() {
        if (alreadyAcceptedOrDeniedCall) {
            return;
        }
        alreadyAcceptedOrDeniedCall = true;

        LinphoneManager.getLc().terminateCall(mCall);
        finish();
    }

    private void answer() {
        if (alreadyAcceptedOrDeniedCall) {
            return;
        }
        alreadyAcceptedOrDeniedCall = true;

        LinphoneCallParams params = LinphoneManager.getLc().createCallParams(mCall);

        boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(LinphoneService.instance().getApplicationContext());

        if (params != null) {
            params.enableLowBandwidth(isLowBandwidthConnection);
        }else {
            Log.e("Could not create call params for call");
        }

        if (params == null || !LinphoneManager.getInstance().acceptCallWithParams(mCall, params)) {
            // the above method takes care of Samsung Galaxy S
            Toast.makeText(this, R.string.couldnt_accept_call, Toast.LENGTH_LONG).show();
        } else {
            if (!MainActivity.isInstanciated()) {
                return;
            }
            LinphoneManager.getInstance().routeAudioToReceiver();
//            MainActivity.instance().startIncallActivity(mCall);
            startActivity(new Intent(this,CallActivity.class));

        }
    }
}
