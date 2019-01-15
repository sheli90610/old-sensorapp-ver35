package com.simran.powermanagement;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.DecimalFormat;

public class GraphCost extends AppCompatActivity {

    static EditText kwHighCost, kwLowCost, fromHighView, toHighView, fromLowView, toLowView;
    static String valCost;
    static float costHigh, costLow;
    static ImageView saveCostImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost);

        kwHighCost = findViewById(R.id.kwHighCost);
        kwLowCost = findViewById(R.id.kwLowCost);

        fromHighView = findViewById(R.id.fromHighView);
        toHighView = findViewById(R.id.toHighView);
        fromLowView = findViewById(R.id.fromLowView);
        toLowView = findViewById(R.id.toLowView);
        saveCostImg = findViewById(R.id.saveImg);

        saveCostImg.setOnClickListener(saveClickListener);

        processTime();
        setHighLowCost();

        kwHighCost.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == android.view.KeyEvent.ACTION_DOWN) &&
                        (keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                    valCost = kwHighCost.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(kwHighCost.getWindowToken(), 0);

                    if (valCost.equals("")) {
                        return false;
                    }

                    costHigh = Float.parseFloat(valCost);

                    SharedPreferences.Editor editor = getSharedPreferences("myCost", MODE_PRIVATE).edit();
                    editor.putString("costHigh", valCost);
                    editor.apply();

                    setHighLowCost();

                    return true;
                }
                return false;
            }
        });

        kwLowCost.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    valCost = kwLowCost.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(kwLowCost.getWindowToken(), 0);

                    if (valCost.equals("")) {
                        return false;
                    }

                    costLow = Float.parseFloat(valCost);

                    SharedPreferences.Editor editor = getSharedPreferences("myCost", MODE_PRIVATE).edit();
                    editor.putString("costLow", valCost);
                    editor.apply();

                    setHighLowCost();

                    return true;
                }
                return false;
            }
        });

        fromHighView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String str = fromHighView.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(fromHighView.getWindowToken(), 0);

                    if (Integer.parseInt(str) > 12) {
                        fromHighView.setText("12");
                        str = "12";
                    }
                    toLowView.setText(str);
                    String conv = converted(str, true);
                    toHighView.setText(conv);
                    fromLowView.setText(conv);

                    SharedPreferences.Editor editor = getSharedPreferences("myTime", MODE_PRIVATE).edit();
                    editor.putString("fromHighTime", str);
                    editor.putString("toHighTime", conv);
                    editor.putString("fromLowTime", conv);
                    editor.putString("toLowTime", str);
                    editor.apply();

                    return true;
                }
                return false;
            }
        });

        toHighView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String str = toHighView.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(toHighView.getWindowToken(), 0);

                    if (Integer.parseInt(str) < 12) {
                        toHighView.setText("12");
                        str = "12";
                    }
                    if (Integer.parseInt(str) > 23) {
                        toHighView.setText("0");
                        str = "0";
                    }

                    fromLowView.setText(str);
                    String conv = converted(str, false);
                    fromHighView.setText(conv);
                    toLowView.setText(conv);

                    SharedPreferences.Editor editor = getSharedPreferences("myTime", MODE_PRIVATE).edit();
                    editor.putString("fromHighTime", conv);
                    editor.putString("toHighTime", str);
                    editor.putString("fromLowTime", str);
                    editor.putString("toLowTime", conv);

                    editor.apply();

                    return true;
                }
                return false;
            }
        });

        fromLowView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String str = fromLowView.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(fromLowView.getWindowToken(), 0);

                    if (Integer.parseInt(str) < 12) {
                        fromLowView.setText("12");
                        str = "12";
                    }
                    if (Integer.parseInt(str) > 23) {
                        fromLowView.setText("0");
                        str = "0";
                    }
                    toHighView.setText(str);
                    String conv = converted(str, false);
                    fromHighView.setText(conv);
                    toLowView.setText(conv);

                    SharedPreferences.Editor editor = getSharedPreferences("myTime", MODE_PRIVATE).edit();
                    editor.putString("fromHighTime", conv);
                    editor.putString("toHighTime", str);
                    editor.putString("fromLowTime", str);
                    editor.putString("toLowTime", conv);
                    editor.apply();

                    return true;
                }
                return false;
            }
        });

        toLowView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String str = toLowView.getText().toString();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(toLowView.getWindowToken(), 0);

                    if (Integer.parseInt(str) > 12) {
                        toLowView.setText("12");
                        str = "12";
                    }

                    fromHighView.setText(str);
                    String conv = converted(str, true);
                    toHighView.setText(conv);
                    fromLowView.setText(conv);
                    SharedPreferences.Editor editor = getSharedPreferences("myTime", MODE_PRIVATE).edit();
                    editor.putString("fromHighTime", str);
                    editor.putString("toHighTime", conv);
                    editor.putString("fromLowTime", conv);
                    editor.putString("toLowTime", str);
                    editor.apply();

                    return true;
                }
                return false;
            }
        });

    }

    public String converted(String str, boolean add) {
        int number = Integer.parseInt(str);
        int newNumber = 0;
        if (add) {
            newNumber = 12 + number;
            if (newNumber == 24) {
                newNumber = 0;
            }
        } else {
            newNumber = number - 12;
            if (newNumber == -12) {
                newNumber = 12;
            }
        }

        return Integer.toString(newNumber);
    }

    public void processTime() {
        SharedPreferences prefs;
        prefs = getSharedPreferences("myTime", MODE_PRIVATE);
        String fromHighTime = prefs.getString("fromHighTime", null);
        String toHighTime = prefs.getString("toHighTime", null);
        String fromLowTime = prefs.getString("fromLowTime", null);
        String toLowTime = prefs.getString("toLowTime", null);

        if (fromHighTime != null) {
            fromHighView.setText(fromHighTime);
        } else {
            return;
        }
        if (toHighTime != null) {
            toHighView.setText(toHighTime);
        } else {
            return;
        }
        if (fromLowTime != null) {
            fromLowView.setText(fromLowTime);
        } else {
            return;
        }
        if (toLowTime != null) {
            toLowView.setText(toLowTime);
        } else {
            return;
        }
    }

    public void setHighLowCost() {

        SharedPreferences prefs;
        prefs = getSharedPreferences("myCost", MODE_PRIVATE);
        String strCostHigh = prefs.getString("costHigh", null);
        String strCostLow = prefs.getString("costLow", null);

        if (strCostHigh != null && strCostLow != null) {
            kwHighCost.setText(strCostHigh);
            kwLowCost.setText(strCostLow);
        }
    }

    View.OnClickListener saveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent mainPageIntent = new Intent(GraphCost.this, GraphSelectPeriod.class);
            mainPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainPageIntent);
        }
    };
}

