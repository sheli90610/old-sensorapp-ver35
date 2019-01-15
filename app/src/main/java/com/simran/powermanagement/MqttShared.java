package com.simran.powermanagement;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class MqttShared extends Service {

    //Receiving messages
    protected static final int MSG_STATUS_OF_DEVICE_SHARED = 0;
    protected static final int MSG_ON_SHARED = 1;
    protected static final int MSG_OFF_SHARED = 2;
    protected static final int MSG_SHARE_SEND = 3;
    protected static final int MSG_SHARE_RECEIVE = 4;
    static final String LOG_TAG = "BACKGROUND_MqttShared";
    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "aeq35woso3zup-ats.iot.us-east-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:92c2c44a-8aa3-4e40-b2f8-e9b6c63c35a5";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "PubSubAttachCert";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";
    public static boolean IS_SERVICE_RUNNING = false;
    public static List<String> deviceList = new ArrayList<>();
    protected static String mac;
    final Messenger mMessenger = new Messenger(new IncomingHandler());       //Target we publish for clients to send messages to IncomingHandler
    protected JSONObject jsON, jsOFF, jsSHARE;
    String topic;
    // --- Constants to modify per your configuration ---
    TextView tvStatus;
    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    KeyStore clientKeyStore = null;
    String certificateId;
    CognitoCachingCredentialsProvider credentialsProvider;
    boolean isConnected = false;
    private String action = null;          //Used to keep track of to which activity message should be sent

    private static void sendMsgToActivity(String msg, String action) {
        Intent intent = new Intent(action);
        // You can also include some extra data.
        intent.putExtra("Status", msg);
        //Bundle b = new Bundle();
        //b.putParcelable("Location", l);
        //intent.putExtra("Location", b);
        LocalBroadcastManager.getInstance(TempApplication.getAppContext()).sendBroadcast(intent);
    }

    public void devices(String str) {
        deviceList.add(str);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        FileHelper.readMacForMqttShared();

        disconnect();
        setup();

        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                connect();
                while (i < deviceList.size()) {
                    if (isConnected) {
                        topic = "Report_" + deviceList.get(i);
                        subs();
                        i++;
                    }
                }
                deviceList.clear();
            }
        }).start();
        return START_NOT_STICKY;
    }

    public void setup() {

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            Thread set = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            });
            set.start();
            try {
                set.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void connect() {

        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));


                    if (status == AWSIotMqttClientStatus.Connecting) {
                        Log.d(LOG_TAG, "Connecting...");
                        isConnected = false;

                    } else if (status == AWSIotMqttClientStatus.Connected) {
                        Log.d(LOG_TAG, "Connected");
                        isConnected = true;

                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        Log.d(LOG_TAG, "Reconnecting");

                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        Log.d(LOG_TAG, "Disconnected");
                    } else {
                        Log.d(LOG_TAG, "Disconnected");
                    }
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            Log.d(LOG_TAG, "Error! " + e.getMessage());
        }
    }

    public void subs() {
        Log.d(LOG_TAG, "topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    (topic, data) -> {

                        try {
                            String message = new String(data, "UTF-8");
                            Log.d(LOG_TAG, "Topic: " + topic);
                            Log.d(LOG_TAG, "Message: " + message);

                            payload(message);

                        } catch (UnsupportedEncodingException e) {
                            Log.e(LOG_TAG, "Message encoding error.", e);
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }

    }

    public void disconnect() {
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }

    public void payload(String pl) {

        try {
            JSONObject jsonObject = null;
            jsonObject = new JSONObject(pl);

            //Checking Status
            String status = jsonObject.getString("Status");
            Log.i("Checking/Getting Status", status);
            if (status.equals("0")) {
                SharedPreferences.Editor editor = getSharedPreferences("myStatus", MODE_PRIVATE).edit();
                editor.putString("currentStatus", "0");
                editor.apply();
                sendMsgToActivity("off", action);
                action = null;
            }
            if (status.equals("1")) {
                SharedPreferences.Editor editor = getSharedPreferences("myStatus", MODE_PRIVATE).edit();
                editor.putString("currentStatus", "1");
                editor.apply();
                sendMsgToActivity("on", action);
                action = null;
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Status Strings not found");
        }

        try {
            JSONObject jsonObject = null;
            jsonObject = new JSONObject(pl);

            //Checking temperature
            String name = jsonObject.getString("name");
            String icon = jsonObject.getString("icon");
            String mac = jsonObject.getString("mac");

            sendMsgToActivity(name, action);
            action = null;

            Log.i("Device Received", name);

            runOnUiThread(new Runnable() {
                public void run() {
                    FileHelper.createFileIfNotThere(FileHelper.sharedDevices);

                    if (!(FileHelper.readString(mac, FileHelper.sharedDevices))) {    //If shared.txt file doesnot contain that MacAddress. Then,
                        FileHelper.saveToFile(mac, FileHelper.sharedDevices);       //Save the MacAddress to shared.txt
                        FileHelper.saveToFile(name, FileHelper.sharedDevices);       //Save the Name to shared.txt
                        FileHelper.saveToFile(icon, FileHelper.sharedDevices);       //Save the icon to shared.txt
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Shared Device Strings not found");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "MqttShared Binding...", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            topic = "Report_" + mac;
            switch (msg.what) {

                case MSG_STATUS_OF_DEVICE_SHARED:

                    try {
                        JSONObject json = new JSONObject();
                        json.put("Status", "*status*");

                        action = "ShareActivity";
                        mqttManager.publishString(json.toString(), topic, AWSIotMqttQos.QOS0);

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Publish error", e);
                    }
                    break;

                case MSG_ON_SHARED:
                    try {
                        jsON = new JSONObject();
                        jsON.put("ONOFF", "(on)");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error.", e);
                    }
                    action = null;
                    mqttManager.publishString(jsON.toString(), topic, AWSIotMqttQos.QOS0);

                    break;

                case MSG_OFF_SHARED:
                    try {
                        jsOFF = new JSONObject();
                        jsOFF.put("ONOFF", "(off)");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error.", e);
                    }
                    action = null;
                    mqttManager.publishString(jsOFF.toString(), topic, AWSIotMqttQos.QOS0);

                    break;

                case MSG_SHARE_SEND:

                    SharedPreferences prefs;
                    prefs = getSharedPreferences("myShare", MODE_PRIVATE);
                    String topic_share_send = prefs.getString("topic", null);
                    String name = prefs.getString("name", null);
                    String mac = prefs.getString("mac", null);
                    String icon = prefs.getString("icon", null);

                    try {
                        jsSHARE = new JSONObject();
                        jsSHARE.put("name", name);
                        jsSHARE.put("mac", mac);
                        jsSHARE.put("icon", icon);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error.", e);
                    }

                    action = null;
                    mqttManager.publishString(jsSHARE.toString(), topic_share_send, AWSIotMqttQos.QOS0);

                    break;

                case MSG_SHARE_RECEIVE:

                    action = "ShareReceive";

                    SharedPreferences preferences;
                    preferences = getSharedPreferences("myShare", MODE_PRIVATE);
                    String topic_share_recv = preferences.getString("topic", null);

                    Log.d(LOG_TAG, "topic = " + topic_share_recv);

                    try {
                        mqttManager.subscribeToTopic(topic_share_recv, AWSIotMqttQos.QOS0,
                                (topic, data) -> {

                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Topic: " + topic);
                                        Log.d(LOG_TAG, "Message: " + message);

                                        payload(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                });

                        new CountDownTimer(90000, 3000) {

                            public void onTick(long millisUntilFinished) {
                                Toast.makeText(getApplicationContext(), "Time Remaining: " + millisUntilFinished/1000 + " seconds", Toast.LENGTH_SHORT).show();
                            }

                            public void onFinish() {
                                mqttManager.unsubscribeTopic(topic_share_recv);
                            }

                        }.start();

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Subscription error.", e);
                    }

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


}
