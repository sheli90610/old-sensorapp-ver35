package com.simran.powermanagement;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedList;
import com.amazonaws.models.nosql.FinalPowerTableDO;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.youruserpools.UserActivity;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class DDBMain extends Service {

    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/simran/";
    final static String fileDevices = FileHelper.mainDevices;
    protected static String MAC;
    String mac, time, power;
    String deleteUser;
    String deleteTime;
    ArrayList<String> finalData = new ArrayList<>();
    DynamoDBMapper dynamoDBMapper;      //Declare a DynamoDBMapper object
    private String LOG_TAG = "DynamoDB";
    public static boolean IS_SERVICE_RUNNING = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IS_SERVICE_RUNNING = true;
        Log.d(LOG_TAG, "Initialize called");
        final Handler handler = new Handler();

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        // Log.i("INIT", "onResult: " + userStateDetails.getUserState());

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                readDevicesMac();
                            }
                        }, 2000);

                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("INIT", "Initialization error.", e);
                    }
                }
        );

        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();

        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);                  //Add code to instantiate a AmazonDynamoDBClient
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();

        return START_NOT_STICKY;
    }

    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }

    private void readDevicesMac() {

        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileDevices));
            String line = bufferedReader.readLine();
            while (line != null) {

                Log.e(LOG_TAG, "********************************************************************************************************");
                //Set  (Device Name, Device Mac Address)
                System.out.println("I am given Device from File: " + line);
                MAC = line;

                queryNews();

                SharedPreferences prefs;
                prefs = getSharedPreferences("myDDBMain", MODE_PRIVATE);
                String isResultEmpty = prefs.getString("isResultEmpty", null);

                if ("false".equals(isResultEmpty)) {
                    updateFiles();
                }

                //Read next line
                bufferedReader.readLine(); //Ignore 1 reading (Device Name)
                bufferedReader.readLine(); //Ignore second line (Icon)
                line = bufferedReader.readLine();

            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Self Destroy
        Log.e("SelfDestruction", "DDBMain will be destroyed");
        IS_SERVICE_RUNNING = false;
        if (isForeground("com.simran.powermanagement")) {
            UserActivity.ddbMainFinished();
        }
        Intent service = new Intent(getApplicationContext(), DDBMain.class);
        getApplicationContext().stopService(service);
    }

    protected void queryNews() {

        Thread query = new Thread(new Runnable() {
            @Override
            public void run() {

                FinalPowerTableDO news = new FinalPowerTableDO();
                news.setUserId(MAC);                               //This is the item in Table under underId
                news.setTime("");                               //This is the item in Table under TimeStamp


                Condition rangeKeyCondition = new Condition()
                        .withComparisonOperator(ComparisonOperator.BEGINS_WITH)
                        .withAttributeValueList(new AttributeValue().withS("#"));                               //This the another Search attribute******Make sure to change it

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues(news)                                                                //Do some research about this line
                        .withRangeKeyCondition("time", rangeKeyCondition)         //This is the name of the Sort-Key
                        .withConsistentRead(false);

                PaginatedList<FinalPowerTableDO> result = dynamoDBMapper.query(FinalPowerTableDO.class, queryExpression);

                Gson gson = new Gson();
                StringBuilder stringBuilder = new StringBuilder();

                // Loop through query results
                for (int i = 0; i < result.size(); i++) {
                    String jsonFormOfItem = gson.toJson(result.get(i));
                    stringBuilder.append(jsonFormOfItem + "\n\n");
                }

                // Add your code here to deal with the data result

                String data = stringBuilder.toString();                                     //If it is empty then its fine
                //  Log.d("Query result ", data);                                           //Otherwise call it again to get the data
                if (result.isEmpty()) {
                    Log.d("Error", "There were no items matching your Query");
                    SharedPreferences.Editor editor = getSharedPreferences("myDDBMain", MODE_PRIVATE).edit();
                    editor.putString("isResultEmpty", "true");
                    editor.apply();
                    return;
                } else {
                    SharedPreferences.Editor editor = getSharedPreferences("myDDBMain", MODE_PRIVATE).edit();
                    editor.putString("isResultEmpty", "false");
                    editor.apply();
                }
                String dataCount = data;
                //****************************************************************************************
                int indexPower = dataCount.indexOf("{");
                int count = 0;
                while (indexPower != -1) {
                    count++;
                    dataCount = dataCount.substring(indexPower + 1);
                    indexPower = dataCount.indexOf("{");
                }
                System.out.println("No of times '{' in the input is : " + count);

                //*****************************************************************************************************
                //Filter the Data
                int i = 0;
                int indexPower0, indexPower1;
                String data0, tempData;

                tempData = data;
                while (i < count) {
                    indexPower0 = tempData.indexOf("{");
                    indexPower1 = tempData.indexOf("}");
                    data0 = tempData.substring(indexPower0, indexPower1 + 1);

                    dataProcess(data0);         //Process the data to the array list

                    data0 = tempData.replace(data0, "");
                    tempData = data0;
                    i++;
                }
                //**************************************************************************************
                //Now All the query is done
                //All the items are deleted from table
                //All the items are stored in array list
            }
        });
        query.start();
        try {
            query.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void dataProcess(String data) {
        //Same thread of query News
        try {
            JSONObject jsonObject = null;
            jsonObject = new JSONObject(data);

            mac = jsonObject.getString("_userId");

            time = jsonObject.getString("_time");
            time = time.substring(1);                       //To remove # from start

            power = jsonObject.getString("_power");

        } catch (JSONException e) {
            e.printStackTrace();
        }


        //Storing the data in ArrayList for Adding it to files later on.
        //We don't wanna implement MAC address in file because its in the name of file

        long correctedTimeLong = Long.parseLong(time) - 3600;     //Offset Applied
        String correctedTimeStr = Long.toString(correctedTimeLong);
        Long systemTime = System.currentTimeMillis() / 1000;

        //If (currentTime-timeServer) is lessEqual than 24hrs then only save the data
        //In other words, if data is less than 24Hrs old store it
    /*    if ((systemTime - correctedTimeLong) <= 86400) {          //86400 is equal to 24 hrs
            finalData.add(power);
            finalData.add(correctedTimeStr);           //Store the corrected time
        }
        */
        finalData.add(power);
        finalData.add(correctedTimeStr);           //Store the corrected time

        deleteUser = mac;
        deleteTime = "#" + time;                //Remove original time

        //Delete item from DynamoDB with each call of dataProcess
        deleteNews();
    }

    public void updateFiles() {
        runOnUiThread(new Runnable() {
            public void run() {

                if (mac != null) {
                    FileHelper.createFileIfNotThere("xxx" + mac + ".txt");              //Last 24 hour file
                    FileHelper.createFileIfNotThere("xwx" + mac + "Add.txt");           //Last 7 days temp file
                    FileHelper.createFileIfNotThere("xwx" + mac + ".txt");              //Last 7 days main file
                    FileHelper.createFileIfNotThere("xxm" + mac + "Add.txt");           //Last 8 weeks temp file
                    FileHelper.createFileIfNotThere("xxm" + mac + ".txt");              //Last 8 weeks main file
                } else {
                    return;
                }

                //24hr file -> Single day -> Single Week -> Single Month
                Log.e("Updating Files", mac + " ********************************************************************************************");
                int numOfItems;
                float totalPower;

                String fileName;
                final int siz = finalData.size();
                //***************************************************************************************************


                //Handling file which stores latest 24x2 items
                //***************************************************************************************************
                int i = 0;
                fileName = ("xxx" + mac + ".txt");
                while (i < finalData.size()) {
                    //Hourly
                    FileHelper.saveToFile(finalData.get(i), fileName);
                    i++;
                }

                numOfItems = FileHelper.countItems(fileName);
                //We should store maximum of 24x2 latest items
                if (numOfItems > 48) {
                    i = 0;
                    i = numOfItems - 48;
                    int x = 0;
                    while (x < i) {
                        try {
                            FileHelper.removeLine(fileName, 0);     //This will remove the first line
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        x++;
                    }
                }
                System.out.println("24hr Hourly file write complete");
                //***************************************************************************************************

                //Single Day
                //We should store maximum of 24x2 iems         //When 24x2 items are complete, add them and then move them to xwxMAC.txt (Daily) (7 days)
                i = 0;
                fileName = ("xwx" + mac + "Add.txt");           //This file stores data temporarily

                while (i < siz) {
                    FileHelper.saveToFile(finalData.get(i), fileName);
                    i++;
                }
                int numOfItemsToRemove;
                numOfItems = FileHelper.countItems(fileName);   //Counts the number of items.
                //Number of Items is 48 So we add them, move them to original file, and delete add file
                //If we have less than 48 items it means that a day is not complete
                //If we have 60 items it means we have less than 2 days
                //If we have 96 itemsit means we have 2 days
                int numOfDays = numOfItems / 48;             //48 items=1 Day****60 items=1Day****96 items=2 Days
                System.out.println("Number of Days: " + numOfDays);
                //If we have 2 Days, Then it will executed 2 times

                while (numOfDays > 0) {
                    //We should add Maximum of 24 items at a time because it constitutes a day
                    totalPower = FileHelper.addEveryOtherItem(fileName, 24);        //Alternative Items
                    System.out.println("Total Power for Day " + numOfDays + " is " + totalPower);
                    //Put that value in the file where it will generate a graph from it
                    FileHelper.saveToFile(Float.toString(totalPower), "xwx" + mac + ".txt");
                    //Read the last time of one day

                    String lastTime = FileHelper.readLine("xwx" + mac + "Add.txt", 47);
                    FileHelper.saveToFile(lastTime, "xwx" + mac + ".txt");

                    //Remove the whole day
                    numOfItemsToRemove = 48;
                    while (numOfItemsToRemove > 0) {
                        try {
                            FileHelper.removeLine(fileName, 0);     //This will remove the first line
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        numOfItemsToRemove--;
                    }
                    numOfDays--;
                }

                //We should store maminum of 7 days in main file
                fileName = ("xwx" + mac + ".txt");
                numOfItems = FileHelper.countItems(fileName);   //Counts the number of items.
                if (numOfItems > 14) {
                    i = 0;
                    i = numOfItems - 14;
                    int x = 0;
                    System.out.println("Removing items from main file: " + i);
                    while (x < i) {
                        try {
                            FileHelper.removeLine(fileName, 0);     //This will remove the first line
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        x++;
                    }
                }

                System.out.println("Daily file write complete");
                //*******************************************************************************************************

//336
                //168 items to add
                //336
                //One Week
                //We should store maximum of 168x2 iems        //When 168x2 items are complete, add them and then move them to xxmMAC.txt (Weekly)
                i = 0;
                fileName = ("xxm" + mac + "Add.txt");           //This file stores data temporarily
                while (i < siz) {
                    FileHelper.saveToFile(finalData.get(i), fileName);
                    i++;
                }

                numOfItems = FileHelper.countItems(fileName);   //Counts the number of items.
                //Number of Items is 48 So we add them, move them to original file, and delete add file
                //If we have less than 48 items it means that a day is not complete
                //If we have 60 items it means we have less than 2 days
                //If we have 96 itemsit means we have 2 days
                int numOfWeeks = numOfItems / 336;             //48 items=1 Day****60 items=1Day****96 items=2 Days
                System.out.println("Number of Weeks: " + numOfWeeks);
                //If we have 2 Days, Then it will executed 2 times
                while (numOfWeeks > 0) {
                    //We should add Maximum of 24 items at a time because it constitutes a day
                    totalPower = FileHelper.addEveryOtherItem(fileName, 168);        //Alternative Items
                    //Put that value in the file where it will generate a graph from it
                    FileHelper.saveToFile(Float.toString(totalPower), "xxm" + mac + ".txt");
                    //Read the last time of one day
                    String lastTime = FileHelper.readLine("xxm" + mac + "Add.txt", 335);
                    FileHelper.saveToFile(lastTime, "xxm" + mac + ".txt");

                    numOfItemsToRemove = 336;
                    //Remove the whole day
                    while (numOfItemsToRemove > 0) {
                        try {
                            FileHelper.removeLine(fileName, 0);     //This will remove the first line
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        numOfItemsToRemove--;
                    }
                    numOfWeeks--;
                }

                //We should store maminum of 4 weeks in main file
                fileName = ("xxm" + mac + ".txt");
                numOfItems = FileHelper.countItems(fileName);   //Counts the number of items.
                if (numOfItems > 8) {
                    i = 0;
                    i = numOfItems - 8;
                    int x = 0;
                    System.out.println("Removing items from main file: " + i);
                    while (x < i) {
                        try {
                            FileHelper.removeLine(fileName, 0);     //This will remove the first line
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        x++;
                    }
                }

                System.out.println("Weekly file write complete");

                Log.e("Updating Files", mac + " Done ****************************************************************************************");

                finalData.clear();      //Clear the Array List
            }
        });
    }

    public void deleteNews() {
        //Same thread as of query news
        FinalPowerTableDO newsItem = new FinalPowerTableDO();
        System.out.println("Deleting: " + deleteUser + " with time " + deleteTime);

        newsItem.setUserId(deleteUser);    //partition key
        newsItem.setTime(deleteTime);  //range (sort) key
        dynamoDBMapper.delete(newsItem);
    }

    public void readNews() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FinalPowerTableDO newsItem = dynamoDBMapper.load(
                        FinalPowerTableDO.class, "myuser", "myid");
                // Item read
                //*********Convert to string*******************
                Log.d("News Item:", newsItem.toString());
            }
        }).start();
    }

    private void updateNews() {
        final FinalPowerTableDO newsItem = new FinalPowerTableDO();
        newsItem.setUserId("user");
        newsItem.setTime("unique-mac");
        newsItem.setPower("18");

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("value");
                dynamoDBMapper.save(newsItem);
                // Item updated
            }
        }).start();
    }

}
