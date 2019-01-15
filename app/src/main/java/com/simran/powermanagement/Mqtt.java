package com.simran.powermanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;
import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class Mqtt extends Service {

    //Receiving messages
    protected static final int MSG_STATUS_OF_DEVICE_MAIN_PAGE = 0;
    protected static final int MSG_STATUS_OF_DEVICE_DFS = 1;
    protected static final int MSG_ON = 2;
    protected static final int MSG_OFF = 3;

    static final String LOG_TAG = "BACKGROUND";
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

    final Messenger mMessenger = new Messenger(new IncomingHandler());       //Target we publish for clients to send messages to IncomingHandler
    protected JSONObject jsON, jsOFF;
    protected String topic;
    protected static String macAddress;
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

        FileHelper.readMacForMqtt();

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
        return START_STICKY;
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
                "Android client lost connection", AWSIotMqttQos.QOS1);
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
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1,
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

    //When Device gets connected id is 1
    public void sendNotification(String title, String message, int id) {

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_bulb);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        //If on Oreo then notification required a notification channel.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(id, notification.build());
    }

    public void disconnect() {
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Already Disconnected");
        }
    }

    public static String timeZone(long timestamp) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = null;

        // convert seconds to milliseconds
        Date date = new java.util.Date(timestamp * 1000L);
        sdf = new SimpleDateFormat("HH:mm");

        // give a timezone reference for formatting (see comment at the bottom)
        sdf.setTimeZone(tz);
        return sdf.format(date);
    }

    public void payload(String pl) {

        try {
            JSONObject jsonObject = null;
            jsonObject = new JSONObject(pl);

            //Checking temperature
            String mac = jsonObject.getString("userId");
            String temperature = jsonObject.getString("temperature");

            String time = jsonObject.getString("time");
            time = time.substring(1);                       //To remove # from start
            long correctedTimeLong = Long.parseLong(time) - 3600;     //Offset Applied
            String timeCorrected = timeZone(correctedTimeLong);

            Log.i("Getting Temperature", temperature);

            SharedPreferences.Editor editorTemp = getSharedPreferences("myTemp" + mac, MODE_PRIVATE).edit();
            editorTemp.putString("currentTemp", temperature);
            editorTemp.putString("currentTempTime",timeCorrected);
            editorTemp.apply();





            checkTemp(mac);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Temperature Strings not found");
        }


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
    }

    public void checkTemp(String mac) {

        SharedPreferences prefs;
        prefs = getSharedPreferences("myTemp" + mac, MODE_PRIVATE);
        String minTemperature = prefs.getString("minTemp", null);
        String maxTemperature = prefs.getString("maxTemp", null);
        String temperature = prefs.getString("currentTemp", null);

        if (!(minTemperature == null && maxTemperature == null)) {
            float minTemp, maxTemp;
            minTemp = Float.parseFloat(minTemperature);
            maxTemp = Float.parseFloat(maxTemperature);

            int number = FileHelper.returnLineNumber(mac, FileHelper.mainDevices);
            String nameOfDevice = FileHelper.readLine(FileHelper.mainDevices, number);

            if (Float.parseFloat(temperature) < minTemp) {
                sendNotification(nameOfDevice, "Temperature is " + temperature + "°C. Which is lower.", 2);
            } else if (Float.parseFloat(temperature) > maxTemp) {
                sendNotification(nameOfDevice, "Temperature is " + temperature + "°C. Which is higher.", 2);
            }
        }

        //This is not sticky
        if (!DDBMain.IS_SERVICE_RUNNING) {
            Intent myService = new Intent(Mqtt.this, DDBMain.class);
            getApplicationContext().startService(myService);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "Mqtt Binding...", Toast.LENGTH_SHORT).show();
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
            topic = "Report_" + macAddress;
            switch (msg.what) {
                case MSG_STATUS_OF_DEVICE_MAIN_PAGE:

                    try {
                        JSONObject json = new JSONObject();
                        json.put("Status", "*status*");

                        action = "MainPageDeviceStatusByUser";
                        mqttManager.publishString(json.toString(), topic, AWSIotMqttQos.QOS1);

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Publish error", e);
                    }
                    break;

                case MSG_STATUS_OF_DEVICE_DFS:

                    try {
                        JSONObject json = new JSONObject();
                        json.put("Status", "*status*");

                        action = "DFSDeviceStatusByUser";
                        mqttManager.publishString(json.toString(), topic, AWSIotMqttQos.QOS1);

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Publish error", e);
                    }
                    break;

                case MSG_ON:
                    try {
                        jsON = new JSONObject();
                        jsON.put("ONOFF", "(on)");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error.", e);
                    }
                    action = null;
                    mqttManager.publishString(jsON.toString(), topic, AWSIotMqttQos.QOS1);

                    break;

                case MSG_OFF:
                    try {
                        jsOFF = new JSONObject();
                        jsOFF.put("ONOFF", "(off)");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error.", e);
                    }
                    action = null;
                    mqttManager.publishString(jsOFF.toString(), topic, AWSIotMqttQos.QOS1);

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


}
