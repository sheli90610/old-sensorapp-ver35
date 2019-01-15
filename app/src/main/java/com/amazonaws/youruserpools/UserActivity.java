/*
 * Copyright 2013-2017 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.youruserpools;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler;
import com.simran.powermanagement.DDBMain;
import com.simran.powermanagement.EventMonitor;
import com.simran.powermanagement.MainPage;
import com.simran.powermanagement.Mqtt;
import com.simran.powermanagement.R;
import com.simran.powermanagement.Simran_FGService;

import java.util.ArrayList;
import java.util.List;

import static com.simran.powermanagement.TempApplication.CHANNEL_ID;

public class UserActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private final String TAG = "UserActivity";
    // To track changes to user details
    private final List<String> attributesToDelete = new ArrayList<>();
    private NavigationView nDrawer;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;
    private ListView attributesList;
    // Cognito user objects
    private CognitoUser user;
    private CognitoUserSession session;
    private CognitoUserDetails details;
    // User details
    private String username;
    GenericHandler trustedDeviceHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            // Close wait dialog
            closeWaitDialog();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Failed to update device status", AppHelper.formatException(exception), true);
        }
    };
    UpdateAttributesHandler updateHandler = new UpdateAttributesHandler() {
        @Override
        public void onSuccess(List<CognitoUserCodeDeliveryDetails> attributesVerificationList) {
            // Update successful
            if (attributesVerificationList.size() > 0) {
                showDialogMessage("Updated", "The updated attributes has to be verified", false);
            }
            getDetails();
        }

        @Override
        public void onFailure(Exception exception) {
            // Update failed
            closeWaitDialog();
            showDialogMessage("Update failed", AppHelper.formatException(exception), false);
        }
    };
    GenericHandler deleteHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            closeWaitDialog();
            // Attribute was deleted
            Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT);

            // Fetch user details from the the service
            getDetails();
        }

        @Override
        public void onFailure(Exception e) {
            closeWaitDialog();
            // Attribute delete failed
            showDialogMessage("Delete failed", AppHelper.formatException(e), false);

            // Fetch user details from the service
            getDetails();
        }
    };
    GetDetailsHandler detailsHandler = new GetDetailsHandler() {
        @Override
        public void onSuccess(CognitoUserDetails cognitoUserDetails) {
            closeWaitDialog();
            // Store details in the AppHandler
            AppHelper.setUserDetails(cognitoUserDetails);
            showAttributes();
            // Trusted devices?
            handleTrustedDevice();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Could not fetch user details!", AppHelper.formatException(exception), true);
        }
    };
    private TextView fs, aud, cl, ls;
    private Button cont;
    static ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        dialog = new ProgressDialog(UserActivity.this); // this = YourActivity
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Checking for updates. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);

        // Set toolbar for this screen
        toolbar = findViewById(R.id.main_toolbar);
        toolbar.setTitle("");
        TextView main_title = findViewById(R.id.main_toolbar_title);
        main_title.setText("Account");
        setSupportActionBar(toolbar);

        // Set navigation drawer for this screen
        mDrawer = findViewById(R.id.user_drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        nDrawer = findViewById(R.id.nav_view);
        setNavDrawer();
        init();
        View navigationHeader = nDrawer.getHeaderView(0);
        TextView navHeaderSubTitle = navigationHeader.findViewById(R.id.textViewNavUserSub);
        navHeaderSubTitle.setText(username);


        fs = findViewById(R.id.fileSystemView);
        aud = findViewById(R.id.audioRecordView);
        cl = findViewById(R.id.coarseLocationView);
        ls = findViewById(R.id.locationServicesView);

        checkPermission();


        cont = findViewById(R.id.btnContinue);
        cont.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent myInt = new Intent(UserActivity.this, MainPage.class);
                startActivity(myInt);
            }
        });

    }

    public static void ddbMainStarted() {
        dialog.show();
    }

    public static void ddbMainFinished() {
        dialog.dismiss();
    }


    public void startService() {
        String input = "Monitoring";
        Intent serviceIntent = new Intent(this, Simran_FGService.class);
        serviceIntent.putExtra("inputExtra", input);
        ContextCompat.startForegroundService(this, serviceIntent);
        if (!getApplicationContext().getPackageName().equals("com.simran.powermanagement")) {
            moveTaskToBack(true);
            finishAndRemoveTask();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the toolbar_main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Find which toolbar_main_menu item was selected
        int menuItem = item.getItemId();

        // Do the task
       /* if (menuItem == R.id.user_update_attribute) {
            //updateAllAttributes();
            showWaitDialog("Updating...");
            getDetails();
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 20:
                // Settings
                if (resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                        showAttributes();
                    }
                }
                break;
            case 21:
                // Verify attributes
                if (resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                        showAttributes();
                    }
                }
                break;
            case 22:
                // Add attributes
                if (resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                        showAttributes();
                    }
                }
                break;
        }
    }

    // Handle when the a navigation item is selected
    private void setNavDrawer() {
        nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                performAction(item);
                return true;
            }
        });
    }

    // Perform the action for the selected navigation item
    private void performAction(MenuItem item) {
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Find which item was selected
        switch (item.getItemId()) {

            case R.id.nav_user_change_password:
                // Change password
                changePassword();
                break;

            case R.id.nav_user_sign_out:
                // Sign out from this account
                signOut();
                break;
        }
    }

    // Get user details from CIP service
    private void getDetails() {
        AppHelper.getPool().getUser(username).getDetailsInBackground(detailsHandler);
    }

    // Show user attributes from CIP service
    private void showAttributes() {
        final UserAttributesAdapter attributesAdapter = new UserAttributesAdapter(getApplicationContext());
        final ListView attributesListView;
        attributesListView = findViewById(R.id.listViewUserAttributes);
        attributesListView.setAdapter(attributesAdapter);
        attributesList = attributesListView;

        attributesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView data = view.findViewById(R.id.editTextUserDetailInput);
                String attributeType = data.getHint().toString();
                String attributeValue = data.getText().toString();
                showUserDetail(attributeType, attributeValue);
            }
        });
    }

    // Update attributes
    private void updateAttribute(String attributeType, String attributeValue) {

        if (attributeType == null || attributeType.length() < 1) {
            return;
        }
        CognitoUserAttributes updatedUserAttributes = new CognitoUserAttributes();
        updatedUserAttributes.addAttribute(attributeType, attributeValue);
        Toast.makeText(getApplicationContext(), attributeType + ": " + attributeValue, Toast.LENGTH_LONG);
        showWaitDialog("Updating...");
        AppHelper.getPool().getUser(AppHelper.getCurrUser()).updateAttributesInBackground(updatedUserAttributes, updateHandler);
    }

    // Delete attribute
    private void deleteAttribute(String attributeName) {
        showWaitDialog("Deleting...");
        List<String> attributesToDelete = new ArrayList<>();
        attributesToDelete.add(attributeName);
        AppHelper.getPool().getUser(AppHelper.getCurrUser()).deleteAttributesInBackground(attributesToDelete, deleteHandler);
    }

    // Change user password
    private void changePassword() {
        Intent changePssActivity = new Intent(this, ChangePasswordActivity.class);
        startActivity(changePssActivity);
    }

    // Sign out user
    private void signOut() {
        user.signOut();

        Intent myService = new Intent(getApplicationContext(), DDBMain.class);
        getApplicationContext().stopService(myService);

        myService = new Intent(getApplicationContext(), DDBMain.class);
        getApplicationContext().stopService(myService);

        myService = new Intent(getApplicationContext(), Mqtt.class);
        getApplicationContext().stopService(myService);

        myService = new Intent(getApplicationContext(), EventMonitor.class);
        getApplicationContext().stopService(myService);

        myService = new Intent(getApplicationContext(), Simran_FGService.class);
        getApplicationContext().stopService(myService);

        exit();
    }

    // Initialize this activity
    private void init() {
        // Get the user name
        Bundle extras = getIntent().getExtras();
        username = AppHelper.getCurrUser();
        user = AppHelper.getPool().getUser(username);
        getDetails();
        if (!getApplicationContext().getPackageName().equals("com.simran.powermanagement")) {
            moveTaskToBack(true);
            finishAndRemoveTask();
        }
    }

    // Callback handlers

    private void handleTrustedDevice() {
        CognitoDevice newDevice = AppHelper.getNewDevice();
        if (newDevice != null) {
            AppHelper.newDevice(null);
            trustedDeviceDialog(newDevice);
        }
    }

    private void updateDeviceStatus(CognitoDevice device) {
        device.rememberThisDeviceInBackground(trustedDeviceHandler);
    }

    private void trustedDeviceDialog(final CognitoDevice newDevice) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remember this device?");
        //final EditText input = new EditText(UserActivity.this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        //input.setLayoutParams(lp);
        //input.requestFocus();
        //builder.setView(input);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    //String newValue = input.getText().toString();
                    showWaitDialog("Remembering this device...");
                    updateDeviceStatus(newDevice);
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void showUserDetail(final String attributeType, final String attributeValue) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(attributeType);
        final EditText input = new EditText(UserActivity.this);
        input.setText(attributeValue);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);
        input.requestFocus();
        builder.setView(input);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String newValue = input.getText().toString();
                    if (!newValue.equals(attributeValue)) {
                        showWaitDialog("Updating...");
                        updateAttribute(AppHelper.getSignUpFieldsC2O().get(attributeType), newValue);
                    }
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        }).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    deleteAttribute(AppHelper.getSignUpFieldsC2O().get(attributeType));
                } catch (Exception e) {
                    // Log failure
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if (exit) {
                        exit();
                    }
                } catch (Exception e) {
                    // Log failure
                    Log.e(TAG, " -- Dialog dismiss failed");
                    if (exit) {
                        exit();
                    }
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void closeWaitDialog() {
        try {
            waitDialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    private void exit() {
        Intent intent = new Intent();
        if (username == null)
            username = "";
        intent.putExtra("name", username);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS is disabled, we have to enable it. Let's do it.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected void checkPermission() {

        int audioPermission = ContextCompat.checkSelfPermission(UserActivity.this, Manifest.permission.RECORD_AUDIO);
        int storagePermission = ContextCompat.checkSelfPermission(UserActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int locationPermission = ContextCompat.checkSelfPermission(UserActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ls.setText("Location Service is disabled");
            ls.setTextColor(Color.RED);
            buildAlertMessageNoGps();
        } else {
            ls.setText("Location Service is enabled");
            ls.setTextColor(Color.BLACK);
        }

        //This will get executed when one or more permission are not granted
        if (audioPermission + storagePermission + locationPermission != PackageManager.PERMISSION_GRANTED) {

            //If audio permission is not granted then print it
            if (audioPermission != PackageManager.PERMISSION_GRANTED) {
                aud.setText("Audio Permission not Granted");
                aud.setTextColor(Color.RED);
            } else {
                aud.setText("Audio Permission Granted");
                aud.setTextColor(Color.BLACK);
            }
            //If Storage permission not granted then print it
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                fs.setText("Read/Write Storage Permission not Granted");
                fs.setTextColor(Color.RED);
            } else {
                fs.setText("Read/Write Storage Permission Granted");
                fs.setTextColor(Color.BLACK);
            }
            //If Location permission not granted then print it
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                cl.setText("Coarse Location Permission not Granted");
                cl.setTextColor(Color.RED);
            } else {
                cl.setText("Coarse Location Permission Granted");
                cl.setTextColor(Color.BLACK);
            }

            // Directly request for required permissions, without explanation
            ActivityCompat.requestPermissions(UserActivity.this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_CODE);
        } else {
            // Do something, when permissions are already granted
            fs.setText("Read/Write Storage Permission Granted");
            fs.setTextColor(Color.BLACK);
            aud.setText("Audio Permission Granted");
            aud.setTextColor(Color.BLACK);
            cl.setText("Coarse Location Permission Granted");
            cl.setTextColor(Color.BLACK);

            startService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CODE: {
                // When request is cancelled, the results array are empty
                //When all permissions are granted this will be executed
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1] + grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissions are granted
                    aud.setText("Audio Permission Granted");
                    aud.setTextColor(Color.BLACK);
                    fs.setText("Read/Write Storage Permission Granted");
                    fs.setTextColor(Color.BLACK);
                    cl.setText("Coarse Location Permission Granted");
                    cl.setTextColor(Color.BLACK);
                }
                //This will be executed when one or permissions are not granted
                else {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        aud.setText("Audio Permission not Granted");
                        aud.setTextColor(Color.RED);

                    } else {
                        aud.setText("Audio Permission Granted");
                        aud.setTextColor(Color.BLACK);
                    }

                    if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                        fs.setText("Read/Write Storage Permission not Granted");
                        fs.setTextColor(Color.RED);
                    } else {
                        fs.setText("Read/Write Storage Permission Granted");
                        fs.setTextColor(Color.BLACK);
                    }

                    if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                        cl.setText("Coarse Location Permission not Granted");
                        cl.setTextColor(Color.RED);
                    } else {
                        cl.setText("Coarse Location Permission Granted");
                        cl.setTextColor(Color.BLACK);
                    }
                }
                return;

            }

        }
    }
}
