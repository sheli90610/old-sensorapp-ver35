package com.simran.powermanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class DeviceEdit extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static String deviceName;
    public static String currentMACForIcon;
    public static Integer[] imageIDs = {
            R.drawable.ic_fridge,
            R.drawable.ic_kettle,
            R.drawable.ic_lamp,
            R.drawable.ic_tv,
            R.drawable.ic_watermelon

    };
    private EditText input;
    private ImageView save, selection, deleteBtn;
    private int imagePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_edit);

        GridView gridView = findViewById(R.id.gridView);
        gridView.setAdapter(new ImageAdapter(this));
        gridView.setNumColumns(GridView.AUTO_FIT);
        //gridLayout .setColumnCount(4);
        gridView.setColumnWidth(250);
        gridView.setVerticalSpacing(100);
        // gridView.setHorizontalSpacing(100);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                imagePosition = imageIDs[position];
                selection.setImageResource(imageIDs[position]);
            }
        });


        input = findViewById(R.id.editInp);
        input.setHint(deviceName);

        selection = findViewById(R.id.setIcon);
        save = findViewById(R.id.saveBtn);
        deleteBtn = findViewById(R.id.deleteBtn);

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int line = FileHelper.returnLineNumber(currentMACForIcon, FileHelper.mainDevices);
                String mac = FileHelper.readLine(FileHelper.mainDevices, line-1);
                String name = FileHelper.readLine(FileHelper.mainDevices, line);
                String icon = FileHelper.readLine(FileHelper.mainDevices, line+1);

                try {
                    FileHelper.removeLine(FileHelper.mainDevices, line-1);
                    FileHelper.removeLine(FileHelper.mainDevices, line-1);
                    FileHelper.removeLine(FileHelper.mainDevices, line-1);

                    FileHelper.createFileIfNotThere(FileHelper.deletedDevices);
                    FileHelper.saveToFile(mac, FileHelper.deletedDevices);
                    FileHelper.saveToFile(name,FileHelper.deletedDevices);
                    FileHelper.saveToFile(icon, FileHelper.deletedDevices);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent mainPageIntent = new Intent(DeviceEdit.this, MainPage.class);
                mainPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainPageIntent);
            }

        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = input.getText().toString().toLowerCase();

                if (!name.equals("")) {

                    if (!FileHelper.readString(name, FileHelper.mainDevices)) {           //If the entered input does not exist,then save it
                        FileHelper.readNReplace(deviceName, name);                       //Replace the older String with a new one
                    } else {
                        Toast.makeText(getBaseContext(), "Device Name already exist", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }

                FileHelper.changeIcon(Integer.toString(imagePosition), currentMACForIcon);

                Intent mainPageIntent = new Intent(DeviceEdit.this, MainPage.class);
                mainPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainPageIntent);

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    public class ImageAdapter extends BaseAdapter {
        private Context context;

        public ImageAdapter(Context c) {
            context = c;
        }

        //---returns the number of images---
        public int getCount() {
            return imageIDs.length;
        }

        //---returns the ID of an item---
        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        //---returns an ImageView view---
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(185, 185));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(5, 5, 5, 5);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageResource(imageIDs[position]);
            return imageView;
        }
    }


}