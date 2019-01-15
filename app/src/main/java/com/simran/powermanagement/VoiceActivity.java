package com.simran.powermanagement;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.youruserpools.SignIn;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VoiceActivity extends AppCompatActivity implements InteractiveVoiceView.InteractiveVoiceListener {

    private static final String TAG = "VoiceActivity";
    private static final String TAG2 = "PollyDemo";
    private static final String KEY_VOICES = "Voices";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // Amazon Polly permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:41bbe537-66ef-46a2-baff-506f945495d2";
    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Region of Amazon Polly.
    public static String speechInput;

    CognitoCachingCredentialsProvider credentialsProvider;
    MediaPlayer mediaPlayer;

    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
    private Context appContext;
    private InteractiveVoiceView voiceView;
    private TextView transcriptTextView;
    private TextView responseTextView;
    private AmazonPollyPresigningClient client;
    private List<Voice> voices;
    //**********************************************************************************************
    private String currentConv = null;

    private String deviceName = null;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String statusOfDevice = intent.getStringExtra("Status");
            //Bundle b = intent.getBundleExtra("Location");
            //lastKnownLoc = (Location) b.getParcelable("Location");
            speechInput = "Device is " + statusOfDevice;
            responseTextView.setText(speechInput);
            play();

            try {
                unregisterReceiver(mMessageReceiver);
                Log.i("MainPage", "mMessageReceiver is unregistered");
            } catch (Exception e) {
                Log.e("MainPage", "mMessageReceiver is already unregistered");
            }
        }
    };
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };


    //Polly
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // voices = getSavedVoiceList("voices");
        //if (voices == null) {
        //  Log.i("Polly", "Voice was null");
        setupVoicesSpinner();
        // }else {
        // Log.i("Polly", "Voice was not null");
        //   findViewById(R.id.voicesProgressBar).setVisibility(View.INVISIBLE);
        // }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        //Always run it when main Page executes
        Mqtt.IS_SERVICE_RUNNING = false;

        transcriptTextView = findViewById(R.id.transcriptTextView);
        responseTextView = findViewById(R.id.responseTextView);

        startService();
        verifyService();

        initPolly();
        initPollyClient();

        setupNewMediaPlayer();

        LocalBroadcastManager.getInstance(VoiceActivity.this).registerReceiver(mMessageReceiver, new IntentFilter("MainPageDeviceStatusByUser"));
    }

    public void dialog(String title, String message) {
        android.app.AlertDialog.Builder builder;
        builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })


                /*
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                */
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void startService() {
        String input = "Monitoring";
        Intent serviceIntent = new Intent(this, Simran_FGService.class);
        serviceIntent.putExtra("inputExtra", input);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View v) {
        Log.e("SelfDestruction", "Simran_FGService will be destroyed");
        Intent serviceIntent = new Intent(this, Simran_FGService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initPolly() {
        appContext = getApplicationContext();
        voiceView = findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName(appContext.getResources().getString(R.string.cognito_region)));
        voiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        voiceView.getViewAdapter().setInteractionConfig(
                new InteractionConfig(appContext.getString(R.string.bot_name),
                        appContext.getString(R.string.bot_alias)));
        voiceView.getViewAdapter().setAwsRegion(appContext.getString(R.string.lex_region));
    }

    @Override
    public void dialogReadyForFulfillment(final Map<String, String> slots, final String intent) {
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
        deviceName = slots.get("name");
    }

    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "User: " + response.getInputTranscript());
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        responseTextView.setText(response.getTextResponse());
        transcriptTextView.setText(response.getInputTranscript());

        boolean validStatement = false;
        //This is where ************For what device would you like to know the #****************
        if (currentConv == null) {
            if (response.getTextResponse() == null) {
                //validStatement = false;
                currentConv = null;
                Toast.makeText(getApplicationContext(), "Bot response is null", Toast.LENGTH_SHORT).show();
                return;
            }
            if (response.getTextResponse().contains("temperature")) {
                validStatement = true;
                currentConv = "temp";
                return;
            }
            if (response.getTextResponse().contains("power")) {
                validStatement = true;
                currentConv = "power";
                return;
            }
            if (response.getTextResponse().contains("status")) {
                validStatement = true;
                currentConv = "status";
                return;
            }
            if (response.getTextResponse().contains("turn on")) {
                validStatement = true;
                currentConv = "on";
                return;
            }
            if (response.getTextResponse().contains("turn off")) {
                validStatement = true;
                currentConv = "off";
                return;
            }
            if(!validStatement){
                currentConv = null;
                Toast.makeText(getApplicationContext(), "Not a valid statement", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (deviceName != null) {
            //If there is device with name provided by user then execute this block
            boolean test = FileHelper.readString(deviceName, FileHelper.mainDevices);
            if (!test) {
                speechInput = "Device Not Found";
                responseTextView.setText(speechInput);
                play();
                deviceName = null;               //Since Device is not found, Make it null
                currentConv = null;

            } else {
                //If Program reaches here it means deviceName is found
                int num = FileHelper.returnLineNumber(deviceName, FileHelper.mainDevices);
                String mac = FileHelper.readLine(FileHelper.mainDevices, num - 2);
                Mqtt.macAddress = mac;
                deviceName = null;              //Since Device its executed now, Make it null for next time

                switch (currentConv) {
                    default:
                        return;

                    case "temp":

                        SharedPreferences prefs;
                        prefs = getSharedPreferences("myTemp" + mac, MODE_PRIVATE);
                        String minTemperature = prefs.getString("minTemp", null);
                        String maxTemperature = prefs.getString("maxTemp", null);
                        String temperature = prefs.getString("currentTemp", null);

                        speechInput = "Current Temperature is " + temperature + "° Celsius. Minimum temperature limit set is " + minTemperature + "° Celsius. Maximum temperature limit set is " + maxTemperature + "° Celsius.";

                        responseTextView.setText(speechInput);
                        //Play the sound
                        play();
                        currentConv = null;

                        break;

                    case "power":

                        int numOfItems = FileHelper.countItems("xxx" + mac + ".txt");
                        float tPower24 = FileHelper.addEveryOtherItem("xxx" + mac + ".txt", numOfItems);

                        speechInput = "Power used in last 24 hours is " + Float.toString(tPower24) + " kiloWatt.";
                        responseTextView.setText(speechInput);
                        play();
                        currentConv = null;
                        break;

                    case "status":
                        checkStatus();
                        currentConv = null;
                        break;

                    case "on":
                        turnON();
                        speechInput = "I have switched on the Device";
                        responseTextView.setText(speechInput);
                        play();
                        currentConv = null;
                        break;

                    case "off":
                        turnOFF();
                        speechInput = "I have switched off the Device";
                        responseTextView.setText(speechInput);
                        play();
                        currentConv = null;
                        break;
                }
            }
        }

    }

    @Override
    public void onError(final String responseText, final Exception e) {
        Log.e(TAG, "Error: " + responseText, e);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_VOICES, (Serializable) voices);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        voices = (List<Voice>) savedInstanceState.getSerializable(KEY_VOICES);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void initPollyClient() {
        // Initialize the Amazon Cognito credentials provider.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(credentialsProvider);
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    void setupVoicesSpinner() {
        findViewById(R.id.voicesProgressBar).setVisibility(View.VISIBLE);

        // Asynchronously get available Polly voices.
        new VoiceActivity.GetPollyVoices().execute();
    }

    public void saveVoiceList(List<Voice> voices, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(VoiceActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(voices);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public List<Voice> getSavedVoiceList(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(VoiceActivity.this);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<List<Voice>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    void play() {

        // Voice selectedVoice = (Voice) voicesSpinner.getSelectedItem();

        Voice selectedVoice = new Voice();

        selectedVoice.setGender("Female");
        selectedVoice.setId("Salli");
        selectedVoice.setLanguageCode("en-US");
        selectedVoice.setLanguageName("US English");
        selectedVoice.setName("Salli");

        Log.d("PollyDemo ", selectedVoice.toString());

        //String textToRead = sampleTextEditText.getText().toString();

        String textToRead = speechInput;

        // Use voice's sample text if user hasn't provided any text to read.
        if (textToRead.trim().isEmpty()) {
            textToRead = getSampleText(selectedVoice);
        }

        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set text to synthesize.
                        .withText(textToRead)
                        // Set voice selected by the user.
                        .withVoiceId(selectedVoice.getId())
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

        Log.i(TAG2, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

        // Create a media player to play the synthesized audio stream.
        if (mediaPlayer.isPlaying()) {
            setupNewMediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG2, "Unable to set data source for the media player! " + e.getMessage());
        }

        // Start the playback asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();
    }

    String getSampleText(Voice voice) {
        if (voice == null) {
            return "";
        }

        String resourceName = "sample_" +
                voice.getLanguageCode().replace("-", "_").toLowerCase() + "_" +
                voice.getId().toLowerCase();
        int sampleTextResourceId =
                getResources().getIdentifier(resourceName, "string", getPackageName());
        if (sampleTextResourceId == 0)
            return "";

        return getString(sampleTextResourceId);
    }

    private void verifyService() {
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("REPEAT", "enable").equals("disable")) {
            Intent myInt = new Intent(VoiceActivity.this, SignIn.class);
            startActivity(myInt);
        }
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("REPEAT", "enable").equals("back")) {
            moveTaskToBack(true);
        }
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("REPEAT", "enable").equals("finish")) {
            finishAndRemoveTask();
        }
        if (!getApplicationContext().getPackageName().equals("com.simran.powermanagement")) {
            moveTaskToBack(true);
            finishAndRemoveTask();
        }
    }

    private void checkStatus() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, Mqtt.MSG_STATUS_OF_DEVICE_MAIN_PAGE, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void turnON() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, Mqtt.MSG_ON, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void turnOFF() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, Mqtt.MSG_OFF, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //When Activity is Started
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, Mqtt.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    //When Activity is Stopped
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private class GetPollyVoices extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (voices != null) {
                return null;
            }

            // Create describe voices request.
            DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

            DescribeVoicesResult describeVoicesResult;
            try {
                // Synchronously ask the Polly Service to describe available TTS voices.
                describeVoicesResult = client.describeVoices(describeVoicesRequest);
            } catch (RuntimeException e) {
                Log.e(TAG2, "Unable to get available voices. " + e.getMessage());
                return null;
            }

            // Get list of voices from the result.
            voices = describeVoicesResult.getVoices();

            Log.i("Polly", "Saving voices to shared prefrences");
            saveVoiceList(voices, "voices");

            // Log a message with a list of available TTS voices.
            Log.i(TAG2, "Available Polly voices: " + voices);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (voices == null) {
                return;
            }
            findViewById(R.id.voicesProgressBar).setVisibility(View.INVISIBLE);

        }
    }
}

