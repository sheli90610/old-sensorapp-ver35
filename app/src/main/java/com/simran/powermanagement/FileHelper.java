package com.simran.powermanagement;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class FileHelper {
    final static String mainDevices = "devicesMac.txt";
    final static String sharedDevices = "shared.txt";
    final static String deletedDevices = "deleted.txt";

    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/simran/";
    final static String TAG = FileHelper.class.getName();

    //Check EspTouchActivity
    //Check DDBMain
    //Check DeviceMain_F

    //Creates devicesMac.txt if it is not there
    public static void createFileIfNotThere(String fileName) {
        new File(path).mkdir();
        File file = new File(path + fileName);           //create devicesMac file if it doesn't exist
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    //Read and return all the data inside file
    public static String ReadFile(String fileName) {
        String line = null;

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(path + fileName));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + System.getProperty("line.separator"));
            }
            fileInputStream.close();
            line = stringBuilder.toString();

            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return line;
    }

    //Search for a String in a specified file and returns true if it contains that string
    public static boolean readString(String srchString, String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(path + fileName));

            int len = 0;
            byte[] data1 = new byte[1024];
            while (-1 != (len = fileInputStream.read(data1))) {

                if (new String(data1, 0, len).contains(srchString)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static int returnLineNumber(String str, String fileName) {
        int lineIndex = 0;
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileName));

            String line0 = bufferedReader.readLine();
            while (line0 != null) {

                lineIndex++;
                if (line0.equals(str)) {
                    return lineIndex;
                }

                //Read next line
                line0 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //Read from devicesMac.txt and add (Device Name, MAC Address) to devicesList(ArrayList) in DeviceMain_F.java
    public static void readMac() {

        DeviceMain_F object = new DeviceMain_F();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + mainDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null && line1 != null && line2 != null) {

                System.out.println("I am reading and adding: " + line0);

                //Set  (Device Name, Device Mac Address, Icon)
                object.setDeviceList(line1, line0, line2);
                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Read from devicesMac.txt and add (Device Name, MAC Address) to devicesList(ArrayList) in DeletedMain.java
    public static void readMacDeleted() {

        DeletedMain object = new DeletedMain();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + deletedDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null && line1 != null && line2 != null) {

                System.out.println("I am reading and adding: " + line0);

                //Set  (Device Name, Device Mac Address, Icon)
                object.setDeviceList(line1, line0, line2);
                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readMacShared() {

        ShareMain_F object = new ShareMain_F();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + sharedDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null && line1 != null && line2 != null) {

                System.out.println("I am reading and adding: " + line0);

                //Set  (Device Name, Device Mac Address, Icon)
                object.setDeviceList(line1, line0, line2);
                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readNameForShare() {

        ShareSend object = new ShareSend();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + mainDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();

            int i = 0;
            while (line0 != null && line1 != null && line2 != null) {


                System.out.println("I am reading and adding: " + line1);

                //Set  (Device Name, Device Mac Address, Icon)
                object.setDeviceList(i, line1);
                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
                i++;
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readEvent(String fileName) {

        EventMain object = new EventMain();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileName));
            String line0 = bufferedReader.readLine();       //Mac
            String line1 = bufferedReader.readLine();       //Id
            String line2 = bufferedReader.readLine();       //State
            String line3 = bufferedReader.readLine();       //finalTime

            while (line0 != null) {

                //Set (State, Time, Id)
                object.setEventList(line2, line3, line1);
                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
                line3 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            Log.e("FileHelper.readEvent", "File Not Found");
        }
    }

    public static void readMacForMqtt() {

        Mqtt object = new Mqtt();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + mainDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null) {

                //Set  (Device Name, Device Mac Address)
                System.out.println("I am reading and adding: " + line0);

                //Set  (Device Name, Device Mac Address)
                object.devices(line0);

                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //This is for shared devices
    public static void readMacForMqttShared() {

        MqttShared object = new MqttShared();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + sharedDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null) {

                //Set  (Device Name, Device Mac Address)
                System.out.println("I am reading and adding: " + line0);

                //Set  (Device Name, Device Mac Address)
                object.devices(line0);

                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readMacForEventMonitor() {

        EventMonitor object = new EventMonitor();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + mainDevices));
            String line0 = bufferedReader.readLine();
            String line1 = bufferedReader.readLine();
            String line2 = bufferedReader.readLine();
            while (line0 != null) {

                //Set  (Device Name, Device Mac Address)
                Log.i("readMacForEventMonitor", "I am reading and adding for Event check: " + line0);

                //Set  (Device Name, Device Mac Address)
                object.devices(line0);

                //Read next line
                line0 = bufferedReader.readLine();
                line1 = bufferedReader.readLine();
                line2 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Reads a specified String (Device Name) and Edits it according to the User Input**********************************Needs more  work, what of user enters two dame names
    public static void readNReplace(String current, String replacement) {
        File fileToBeModified = new File(path + mainDevices);
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));
            //Reading all the lines of input text file into oldContent
            String line = reader.readLine();
            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();
            }
            //Replacing oldString with newString in the oldContent
            String newContent = oldContent.replace(current, replacement);
            //Rewriting the input text file with newContent
            writer = new FileWriter(fileToBeModified);

            writer.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //Closing the resources
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Save specified strings to a specified file
    public static void saveToFile(String data, String fileName) {
        try {
            new File(path).mkdir();
            File file = new File(path + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    //Count the number of items in a File and return the value
    public static int countItems(String fileName) {
        int count = 0;
        DeviceMain_F object = new DeviceMain_F();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileName));

            String line = bufferedReader.readLine();
            while (line != null) {
                count++;
                //Read next line
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    //Add the power values of a Specified file
    public static float addEveryOtherItem(String fileName, int numOfItemsToAdd) {
        float total = 0;
        DeviceMain_F object = new DeviceMain_F();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileName));

            String line = bufferedReader.readLine();
            while (line != null && (numOfItemsToAdd > 0)) {
                total += Float.parseFloat(line);
                //Read next line and Ignore it. It is the Time
                bufferedReader.readLine();
                //Read the next power value
                line = bufferedReader.readLine();
                numOfItemsToAdd--;
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DecimalFormat twoDPlaces = new DecimalFormat("#.##");
        return Float.valueOf(twoDPlaces.format(total));

    }

    public static void deleteFile(String fileName) {
        File file = new File(path + fileName);
        boolean deleted = file.delete();
    }

    public static void removeLine(String fileName, int lineIndex) throws IOException {
        final List<String> lines = new LinkedList<>();
        String currentLine;

        final Scanner reader = new Scanner(new FileInputStream(path + fileName), "UTF-8");

        while (reader.hasNextLine()) {
            currentLine = reader.nextLine();
            lines.add(currentLine);
        }

        reader.close();
        if (lineIndex < 0 || (lineIndex > lines.size() - 1)) {
            throw new AssertionError();
        }

        lines.remove(lineIndex);

        final BufferedWriter writer = new BufferedWriter(new FileWriter(path + fileName, false));

        for (String line : lines) {
            writer.write(line + "\n");
        }
        writer.flush();
        writer.close();
    }

    public static void replaceLine(String fileName, int lineIndex, String str) throws IOException {
        final List<String> lines = new LinkedList<>();
        String currentLine;

        final Scanner reader = new Scanner(new FileInputStream(path + fileName), "UTF-8");

        while (reader.hasNextLine()) {
            currentLine = reader.nextLine();
            lines.add(currentLine);
        }

        reader.close();
        if (lineIndex < 0) {
            throw new AssertionError();
        }
        if (lines.size() < 3) {
            lines.add("");
            lines.add("");
        }
        lines.set(lineIndex, str);


        final BufferedWriter writer = new BufferedWriter(new FileWriter(path + fileName, false));

        for (String line : lines) {
            writer.write(line + "\n");
        }
        writer.flush();
        writer.close();
    }

    public static String readLine(String fileName, int lineIndex) {
        BufferedReader bufferedReader;
        String line = "";
        try {
            bufferedReader = new BufferedReader(new FileReader(path + fileName));
            String line0 = bufferedReader.readLine();
            int i = 0;
            while (line0 != null) {

                if (i == lineIndex) {
                    line = line0;
                }
                i++;
                //Read next line
                line0 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    public static void changeIcon(String icon, String mac) {

        final List<String> lines = new LinkedList<>();
        String currentLine;


        try {
            final Scanner reader = new Scanner(new FileInputStream(path + mainDevices), "UTF-8");
            while (reader.hasNextLine()) {
                currentLine = reader.nextLine();
                lines.add(currentLine);
            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int lineIndex = 0;
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(path + mainDevices));
            String line0 = bufferedReader.readLine();

            while (line0 != null) {

                if (line0.equals(mac)) {
                    break;
                }

                lineIndex++;
                //Read next line
                line0 = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (lineIndex < 0 || (lineIndex > lines.size() - 1)) {
            throw new AssertionError();
        }

        lineIndex++;
        lineIndex++;
        System.out.println("Changing Index: " + lineIndex);

        lines.set(lineIndex, icon);

        final BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(path + mainDevices, false));
            for (String line : lines) {
                writer.write(line + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}