package main.java.com.linphonetest.ssk.linphonetesst;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.linphonetest.ssk.linphonetesst.CallActivity;
import com.linphonetest.ssk.linphonetesst.CallInComingActivity;
import com.linphonetest.ssk.linphonetesst.CallOutGoingActivity;
import com.linphonetest.ssk.linphonetesst.R;

import org.linphone.core.*;

import static android.content.Intent.ACTION_MAIN;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int CALL_ACTIVITY = 19;

    private Button button,delete,call;
    private EditText callTo;
    public LinphoneCore mLc;
    private LinphoneProxyConfig linphoneProxyConfig;
    public static MainActivity instance;
    private LinphoneCoreListenerBase mListener ;
    private Handler mHandler;
    private ServiceWaitThread mThread;
    private boolean hasAccount = false;
    private LinphonePreferences mPrefs;



    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    public static final boolean isInstanciated() {
        return instance != null;
    }

    public static final MainActivity instance() {
        if (instance != null)
            return instance;
        throw new RuntimeException("MainActivity not instantiated yet");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        button = (Button) findViewById(R.id.button);
        delete = (Button) findViewById(R.id.button2);
        call = (Button) findViewById(R.id.button3);
        callTo = (EditText) findViewById(R.id.editText);
        callTo.setText("+8650100220005");
        mHandler = new Handler();
        mPrefs = LinphonePreferences.instance();



        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }

        delete.setOnClickListener(this);
        call.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button2:
                Log.i("aaa","mPrefs.delete111=="+mPrefs.getAccountCount());
                mPrefs.deleteAccount(mPrefs.getAccountCount());
                Log.i("aaa","mPrefs.delete222=="+mPrefs.getAccountCount());
                break;
            case R.id.button3:
                    LinphoneManager.getInstance().newOutgoingCall(callTo.getText().toString(),"002");
                break;
        }
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }
    protected void onServiceReady() {

        // We need LinphoneService to start bluetoothManager
//        if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
//            BluetoothManager.getInstance().initBluetooth();
//        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                buildNewCount();

            }
        }, 1000);
    }

    public void buildNewCount(){
        if (!LinphoneManager.isInstanciated())
            LinphoneManager.createAndStart(LinphoneService.instance());

        mLc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();

        LinphoneCoreFactory factory = LinphoneCoreFactory.instance();

        String username = "+8650100220003";
        String userid = "+8650100220003@conf.shec.edu.cn";
        String domain = "conf.shec.edu.cn";
        String password = "123456";
        String proxy = "<sip:222.249.237.98;transport=tcp>";
        String expire = "3600";
        LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(mLc)
                .setUsername(username)
                .setUserId(userid)
                .setDomain(domain)
                .setPassword(password)
                .setProxy(proxy)
                .setTransport(LinphoneAddress.TransportType.LinphoneTransportTcp)
                .setExpires(expire);

        int accountNum = mPrefs.getAccountCount();
        Log.i("aaa","mPrefs.getaccountCount=="+mPrefs.getAccountCount());
        if (accountNum!=0)
            hasAccount = true;

        android.util.Log.i("aaa","count111=="+mPrefs.getAccountCount());

        try {
            if (!hasAccount)
                builder.saveNewAccount();
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
        android.util.Log.i("aaa","count222=="+mPrefs.getAccountCount());

//        LinphoneManager.getLc().refreshRegisters();
        mListener = new LinphoneCoreListenerBase(){
            @Override
            public void registrationState(final LinphoneCore lc, final LinphoneProxyConfig proxy, final LinphoneCore.RegistrationState state, String smessage) {

                Log.i("aaa","listenerstate== lc.getDefaultProxyConfig()=="+lc.getDefaultProxyConfig());
                Log.i("aaa","listenerstate== proxy =="+proxy);
                LinphoneProxyConfig defaultProxyConfig = lc.getDefaultProxyConfig();
                LinphoneAddress address1 = defaultProxyConfig.getAddress();
                LinphoneAddress address2 = proxy.getAddress();
                Log.i("aaa","message=="+smessage);
                if (lc.getDefaultProxyConfig() != null && lc.getDefaultProxyConfig().equals(proxy)) {
                    Toast.makeText(MainActivity.this,state.toString(),Toast.LENGTH_SHORT).show();
                } else if(lc.getDefaultProxyConfig() == null) {
                    Toast.makeText(MainActivity.this,state.toString(),Toast.LENGTH_SHORT).show();

                }
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        lc.refreshRegisters();
                    }
                });


            }

            @Override
            public void notifyReceived(final LinphoneCore lc, LinphoneEvent ev, String eventName, LinphoneContent content) {

                if(!content.getType().equals("application")) return;
                if(!content.getSubtype().equals("simple-message-summary")) return;

                if (content.getData() == null) return;
                int unreadCount = -1;
                String data = content.getDataAsString();
                String[] voiceMail = data.split("voice-message: ");
                final String[] intToParse = voiceMail[1].split("/",0);
            }

            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                super.callState(lc, call, state, message);
                android.util.Log.i("aaa","LinphoneTest:callState==="+state.toString());
                if (state == LinphoneCall.State.IncomingReceived) {
                    Toast.makeText(instance,"来电",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(instance, CallInComingActivity.class));
                } else if (state == LinphoneCall.State.OutgoingInit || state == LinphoneCall.State.OutgoingProgress) {
                    Toast.makeText(instance,"out goning",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(instance, CallOutGoingActivity.class));
                } else if (state == LinphoneCall.State.CallEnd || state == LinphoneCall.State.Error || state == LinphoneCall.State.CallReleased) {
//                    resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
                    Toast.makeText(instance,"callactivity",Toast.LENGTH_SHORT).show();
//                    startActivity(new Intent(instance, CallActivity.class));

                }

                int missedCalls = LinphoneManager.getLc().getMissedCallsCount();
//                displayMissedCalls(missedCalls);
                Log.i("aaa","missecalls==="+missedCalls);
            }
        };

        if (LinphoneManager.getLc() != null) {
            LinphoneManager.getLc().addListener(mListener);

            linphoneProxyConfig = LinphoneManager.getLc().getDefaultProxyConfig();

            LinphoneAuthInfo authInfo = factory.createAuthInfo(username,
                    userid, password, null, null,
                    domain);


            LinphoneManager.getLc().addAuthInfo(authInfo);


            LinphoneAddress address ;
            address = factory.createLinphoneAddress(username,domain,"ceshi");
            address.setDomain(domain);
//			address.setTransport(LinphoneAddress.TransportType.LinphoneTransportTcp);
            try {
                linphoneProxyConfig.setAddress(address);
            } catch (LinphoneCoreException e) {
                e.printStackTrace();
            }
            linphoneProxyConfig.enableRegister(true);
            LinphoneManager.getLc().refreshRegisters();

            Log.i("aaa",linphoneProxyConfig.getState().toString());
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
    protected void onResume() {
        super.onResume();

        if (!LinphoneService.isReady()) {
            startService(new Intent(Intent.ACTION_MAIN).setClass(this, LinphoneService.class));
        }

        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }

    }
    public void startIncallActivity(LinphoneCall currentCall) {
        Intent intent = new Intent(this, CallActivity.class);
//        startOrientationSensor();
        startActivityForResult(intent, CALL_ACTIVITY);
    }
}
