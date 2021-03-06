package com.linphonetest.ssk.linphonetesst;

import android.Manifest;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

import main.java.com.linphonetest.ssk.linphonetesst.CallAudioFragment;
import main.java.com.linphonetest.ssk.linphonetesst.CallVideoFragment;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneManager;
import main.java.com.linphonetest.ssk.linphonetesst.LinphonePreferences;
import main.java.com.linphonetest.ssk.linphonetesst.LinphoneUtils;

public class CallActivity extends AppCompatActivity implements View.OnClickListener ,SensorEventListener {
    private final static int SECONDS_BEFORE_HIDING_CONTROLS = 4000;
    private final static int SECONDS_BEFORE_DENYING_CALL_UPDATE = 30000;
    private static final int PERMISSIONS_REQUEST_CAMERA = 202;
    private static final int PERMISSIONS_ENABLED_CAMERA = 203;
    private static final int PERMISSIONS_ENABLED_MIC = 204;

    private static CallActivity instance;
    private boolean isTransferAllowed = true;
    private boolean mProximitySensingEnabled;
    private CallVideoFragment videoCallFragment;
    private CallAudioFragment audioCallFragment;

    public static CallActivity instance() {
        return instance;
    }

    public static boolean isInstanciated() {
        return instance != null;
    }


    private Button video,hangUp;
    private EditText editText;
    private LinphoneCoreListenerBase mListener;
    private CountDownTimer timer;

    private SensorManager mSensorManager;
    private Sensor mProximity;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        instance = this;

        video = (Button) findViewById(R.id.call_video);
        video.setOnClickListener(this);

        hangUp = (Button) findViewById(R.id.hang_up);
        hangUp.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);




        mListener = new LinphoneCoreListenerBase() {
//            @Override
//            public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
//                displayMissedChats();
//            }

            @Override
            public void callState(LinphoneCore lc, final LinphoneCall call, LinphoneCall.State state, String message) {
                android.util.Log.i("aaa","callactivity-state="+state.toString());

                if (LinphoneManager.getLc().getCallsNb() == 0) {
                    finish();
                    return;
                }

                if (state == LinphoneCall.State.IncomingReceived) {
//                    startIncomingCallActivity();
                    return;
                } else if (state == LinphoneCall.State.Paused || state == LinphoneCall.State.PausedByRemote ||  state == LinphoneCall.State.Pausing) {
//                    if(LinphoneManager.getLc().getCurrentCall() != null) {
//                        enabledVideoButton(false);
//                    }
//                    if(isVideoEnabled(call)){
//                        showAudioView();
//                    }
                } else if (state == LinphoneCall.State.Resuming) {
                    if(LinphonePreferences.instance().isVideoEnabled()){
//                        status.refreshStatusItems(call, isVideoEnabled(call));
                        if(call.getCurrentParamsCopy().getVideoEnabled()){
                            showVideoView();

                        }
                    }
                    if(LinphoneManager.getLc().getCurrentCall() != null) {
                        enabledVideoButton(true);
                    }
                } else if (state == LinphoneCall.State.StreamsRunning) {
                    android.util.Log.i("aaa","call=StreamsRunnig---isVideoEnable=="+isVideoEnabled(call));
                    switchVideo(isVideoEnabled(call));
                    enableAndRefreshInCallActions();

//                    if (status != null) {
//                        videoProgress.setVisibility(View.GONE);
//                        status.refreshStatusItems(call, isVideoEnabled(call));
//                    }
                } else if (state == LinphoneCall.State.CallUpdatedByRemote) {
                    // If the correspondent proposes video while audio call
                    boolean videoEnabled = LinphonePreferences.instance().isVideoEnabled();
                    if (!videoEnabled) {
                        acceptCallUpdate(false);
                    }

                    boolean remoteVideo = call.getRemoteParams().getVideoEnabled();
                    boolean localVideo = call.getCurrentParamsCopy().getVideoEnabled();
                    boolean autoAcceptCameraPolicy = LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests();
                    if (remoteVideo && !localVideo && !autoAcceptCameraPolicy && !LinphoneManager.getLc().isInConference()) {
                        showAcceptCallUpdateDialog();
                        timer = new CountDownTimer(SECONDS_BEFORE_DENYING_CALL_UPDATE, 1000) {
                            public void onTick(long millisUntilFinished) { }
                            public void onFinish() {
                                //TODO dismiss dialog
                                acceptCallUpdate(false);
                            }
                        }.start();

							/*showAcceptCallUpdateDialog();

							timer = new CountDownTimer(SECONDS_BEFORE_DENYING_CALL_UPDATE, 1000) {
								public void onTick(long millisUntilFinished) { }
								public void onFinish() {
									//TODO dismiss dialog

								}
							}.start();*/

                    }
//        			else if (remoteVideo && !LinphoneManager.getLc().isInConference() && autoAcceptCameraPolicy) {
//        				mHandler.post(new Runnable() {
//        					@Override
//        					public void run() {
//        						acceptCallUpdate(true);
//        					}
//        				});
//        			}
                }

//                refreshIncallUi();
//                transfer.setEnabled(LinphoneManager.getLc().getCurrentCall() != null);
            }

            @Override
            public void callEncryptionChanged(LinphoneCore lc, final LinphoneCall call, boolean encrypted, String authenticationToken) {
//                if (status != null) {
//                    if(call.getCurrentParamsCopy().getMediaEncryption().equals(LinphoneCore.MediaEncryption.ZRTP) && !call.isAuthenticationTokenVerified()){
//                        status.showZRTPDialog(call);
//                    }
//                    status.refreshStatusItems(call, call.getCurrentParamsCopy().getVideoEnabled());
//                }
            }

        };


        if (findViewById(R.id.fragmentContainer) != null) {
//            initUI();

            if (LinphoneManager.getLc().getCallsNb() > 0) {
                LinphoneCall call = LinphoneManager.getLc().getCalls()[0];

                if (LinphoneUtils.isCallEstablished(call)) {
                    enableAndRefreshInCallActions();
                }
            }

//            if (savedInstanceState != null) {
//                // Fragment already created, no need to create it again (else it will generate a memory leak with duplicated fragments)
//                isSpeakerEnabled = savedInstanceState.getBoolean("Speaker");
//                isMicMuted = savedInstanceState.getBoolean("Mic");
//                isVideoCallPaused = savedInstanceState.getBoolean("VideoCallPaused");
//                refreshInCallActions();
//                return;
//            } else {
//                isSpeakerEnabled = LinphoneManager.getLc().isSpeakerEnabled();
//                isMicMuted = LinphoneManager.getLc().isMicMuted();
//            }

            Fragment callFragment = new CallVideoFragment();
            if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
                callFragment = new CallVideoFragment();
                videoCallFragment = (CallVideoFragment) callFragment;
//                displayVideoCall(false);
                LinphoneManager.getInstance().routeAudioToSpeaker();
//                isSpeakerEnabled = true;
            } else {
                callFragment = new CallAudioFragment();
                audioCallFragment = (CallAudioFragment) callFragment;
            }

//            if(BluetoothManager.getInstance().isBluetoothHeadsetAvailable()){
//                BluetoothManager.getInstance().routeAudioToBluetooth();
//            }

            callFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.fragmentContainer,callFragment).commitAllowingStateLoss();
        }



    }
    @Override
    protected void onResume() {
        instance = this;
        super.onResume();

        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }

//        refreshIncallUi();
//        handleViewIntent();

        if (!isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
            enableProximitySensing(true);
//            removeCallbacks();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.removeListener(mListener);
        }

    }
    @Override
    protected void onDestroy() {
        LinphoneManager.getInstance().changeStatusToOnline();

//        if (mControlsHandler != null && mControls != null) {
//            mControlsHandler.removeCallbacks(mControls);
//        }
//        mControls = null;
//        mControlsHandler = null;
//
        enableProximitySensing(false);

//        unbindDrawables(findViewById(R.id.topLayout));
        instance = null;
        super.onDestroy();
        System.gc();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.call_video:
                    int camera = getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
                    Log.i("[Permission] Camera permission is " + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

                    if (camera == PackageManager.PERMISSION_GRANTED) {
                        disableVideo(isVideoEnabled(LinphoneManager.getLc().getCurrentCall()));
                    } else {
                        checkAndRequestPermission(Manifest.permission.CAMERA, PERMISSIONS_ENABLED_CAMERA);

                    }
                break;
            case R.id.hang_up:
                hangUp();
                break;
        }
    }
    private boolean isVideoEnabled(LinphoneCall call) {
        if(call != null){
            return call.getCurrentParamsCopy().getVideoEnabled();
        }
        return false;
    }

    private void disableVideo(final boolean videoDisabled) {
        final LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }
        android.util.Log.i("aaa","video-click=="+videoDisabled);


        if (videoDisabled) {
            LinphoneCallParams params = LinphoneManager.getLc().createCallParams(call);
            params.setVideoEnabled(false);
            LinphoneManager.getLc().updateCall(call, params);
        } else {
//            videoProgress.setVisibility(View.VISIBLE);
            if (!call.getRemoteParams().isLowBandwidthEnabled()) {
                LinphoneManager.getInstance().addVideo();
                Toast.makeText(this,"addVideo",Toast.LENGTH_SHORT).show();

            } else {
//                displayCustomToast(getString(R.string.error_low_bandwidth), Toast.LENGTH_LONG);
                Toast.makeText(this,"网络状态不好",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void checkAndRequestPermission(String permission, int result) {
        int permissionGranted = getPackageManager().checkPermission(permission, getPackageName());
        Log.i("[Permission] " + permission + " is " + (permissionGranted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
            if (LinphonePreferences.instance().firstTimeAskingForPermission(permission) || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.i("[Permission] Asking for " + permission);
                ActivityCompat.requestPermissions(this, new String[] { permission }, result);
            }
        }
    }
    private void hangUp() {
        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        if (currentCall != null) {
            lc.terminateCall(currentCall);
        } else if (lc.isInConference()) {
            lc.terminateConference();
        } else {
            lc.terminateAllCalls();
        }
    }
    private void enabledVideoButton(boolean enabled){
        if(enabled) {
            video.setEnabled(true);
        } else {
            video.setEnabled(false);
        }
    }
    private void showVideoView() {
//        if (!BluetoothManager.getInstance().isBluetoothHeadsetAvailable()) {
//            Log.w("Bluetooth not available, using speaker");
//            LinphoneManager.getInstance().routeAudioToSpeaker();
//            isSpeakerEnabled = true;
//        }
//        refreshInCallActions();

        enableProximitySensing(false);
        replaceFragmentAudioByVideo();




//        hideStatusBar();
    }
    private void replaceFragmentVideoByAudio() {
        audioCallFragment = new CallAudioFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, audioCallFragment);
        try {
            transaction.commitAllowingStateLoss();
        } catch (Exception e) {
        }
    }

    private void replaceFragmentAudioByVideo() {
//		Hiding controls to let displayVideoCallControlsIfHidden add them plus the callback
        videoCallFragment = new CallVideoFragment();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, videoCallFragment);
        try {
            transaction.commitAllowingStateLoss();
        } catch (Exception e) {
        }
    }
    public void bindAudioFragment(CallAudioFragment fragment) {
        audioCallFragment = fragment;
    }

    public void bindVideoFragment(CallVideoFragment fragment) {
        videoCallFragment = fragment;
    }
    public void acceptCallUpdate(boolean accept) {
        if (timer != null) {
            timer.cancel();
        }

        LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }

        LinphoneCallParams params = LinphoneManager.getLc().createCallParams(call);
        if (accept) {
            params.setVideoEnabled(true);
            LinphoneManager.getLc().enableVideo(true, true);
        }

        android.util.Log.i("aaa",call.toString()+"===="+params.toString());

        try {
            LinphoneManager.getLc().acceptCallUpdate(call, params);
        } catch (LinphoneCoreException e) {
            Log.e(e);
        }
    }
    private void showAcceptCallUpdateDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable d = new ColorDrawable(ContextCompat.getColor(this, R.color.colorC));
        d.setAlpha(200);
        dialog.setContentView(R.layout.dialog);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(d);

        TextView customText = (TextView) dialog.findViewById(R.id.customText);
        customText.setText("对方请求视频");
        Button delete = (Button) dialog.findViewById(R.id.delete_button);
        delete.setText(R.string.accept);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        cancel.setText(R.string.decline);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int camera = getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
                Log.i("[Permission] Camera permission is " + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

                if (camera == PackageManager.PERMISSION_GRANTED) {
                    CallActivity.instance().acceptCallUpdate(true);
                } else {
                    checkAndRequestPermission(Manifest.permission.CAMERA, PERMISSIONS_REQUEST_CAMERA);
                }

                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view){
                if (CallActivity.isInstanciated()) {
                    CallActivity.instance().acceptCallUpdate(false);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void enableAndRefreshInCallActions() {
        int confsize = 0;

        if(LinphoneManager.getLc().isInConference()) {
            confsize = LinphoneManager.getLc().getConferenceSize() - (LinphoneManager.getLc().isInConference() ? 1 : 0);
        }


        if(LinphoneManager.getLc().getCurrentCall() != null && LinphonePreferences.instance().isVideoEnabled() && !LinphoneManager.getLc().getCurrentCall().mediaInProgress()) {
            enabledVideoButton(true);
        } else {
            enabledVideoButton(false);
        }
    }

    private void switchVideo(final boolean displayVideo) {
        final LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
        if (call == null) {
            return;
        }

        //Check if the call is not terminated
        if(call.getState() == LinphoneCall.State.CallEnd || call.getState() == LinphoneCall.State.CallReleased) return;

        if (!displayVideo) {
            showAudioView();
        } else {
            if (!call.getRemoteParams().isLowBandwidthEnabled()) {
                LinphoneManager.getInstance().addVideo();
//                if (videoCallFragment == null || !videoCallFragment.isVisible())
                    showVideoView();
            } else {
//                displayCustomToast(getString(R.string.error_low_bandwidth), Toast.LENGTH_LONG);
                Toast.makeText(instance,"错误 CallActivity switchVideo",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showAudioView() {
        enableProximitySensing(true);
        replaceFragmentVideoByAudio();
//        displayAudioCall();
//        showStatusBar();
//        removeCallbacks();
    }
    private void enableProximitySensing(boolean enable){
        if (enable){
            if (!mProximitySensingEnabled){
                mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
                mProximitySensingEnabled = true;
            }
        }else{
            if (mProximitySensingEnabled){
                mSensorManager.unregisterListener(instance);
                mProximitySensingEnabled = false;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
