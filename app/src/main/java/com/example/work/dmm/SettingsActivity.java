package com.example.work.dmm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private EditText editText_maxDataPoints;
    private Button confirmIdChange;
    private Button confirmMaxDataPointsChange;
    private BaseApplication base;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        editText_maxDataPoints = (EditText) findViewById(R.id.editText_MaxDataPoints);
        confirmIdChange = (Button)findViewById(R.id.btn_accpetNewID);
        confirmMaxDataPointsChange = (Button)findViewById(R.id.btn_acceptNewMaxDataPoints);
        base = (BaseApplication)getApplicationContext();
        editText_maxDataPoints.setText(String.valueOf(base.getMaxDataPointsToKeep()));
        resetAdapterAddressFromBaseclass();
    }

    public void resetAdapterAddressFromBaseclass(){
        String[] deviceAddress = base.getAdapterAddress().split(":");
        EditText txt1 = ((EditText)findViewById(R.id.adress1));
        EditText txt2 = ((EditText)findViewById(R.id.adress2));
        EditText txt3 = ((EditText)findViewById(R.id.adress3));
        EditText txt4 = ((EditText)findViewById(R.id.adress4));
        EditText txt5 = ((EditText)findViewById(R.id.adress5));
        EditText txt6 = ((EditText)findViewById(R.id.adress6));

        txt1.setText(deviceAddress[0]);
        txt2.setText(deviceAddress[1]);
        txt3.setText(deviceAddress[2]);
        txt4.setText(deviceAddress[3]);
        txt5.setText(deviceAddress[4]);
        txt6.setText(deviceAddress[5]);
    }

    public void onConfirmButton_MaxDataPoints (View view){
        EditText editText = (EditText)findViewById(R.id.editText_MaxDataPoints);
        String text = editText.getText().toString();
        int number =0;
        // input validation
        boolean valid = true;
        if(text.length() <= 0){
            valid = false;
        }else{
            try {
                number = Integer.parseInt(text);
                if (number > 9999 || number < 0){
                    valid = false;
                }
            } catch (NumberFormatException e) {
                //text not an integer
                valid = false;
            }
        }
        if(valid){
            BaseApplication.setMaxDataPointsToKeep(number);
            base.ts("Maximum data points set to:" + number);
        }else{
            base.ts("Invalid value");
            editText.setText(String.valueOf(BaseApplication.getDmmDeviceId()));
        }
    }

    public void onConfirmButton_Address (View view) {
        boolean valid = true;
        try {
            String txt1 = ((EditText)findViewById(R.id.adress1)).getText().toString();
            String txt2 = ((EditText)findViewById(R.id.adress2)).getText().toString();
            String txt3 = ((EditText)findViewById(R.id.adress3)).getText().toString();
            String txt4 = ((EditText)findViewById(R.id.adress4)).getText().toString();
            String txt5 = ((EditText)findViewById(R.id.adress5)).getText().toString();
            String txt6 = ((EditText)findViewById(R.id.adress6)).getText().toString();

            int part1 = Integer.parseInt(txt1,16);
            if (part1 < 0 || part1 > 255){
                valid = false;
            }
            int part2 = Integer.parseInt(txt2,16);
            if (part2 < 0 || part2 > 255){
                valid = false;
            }
            int part3 = Integer.parseInt(txt3,16);
            if (part3 < 0 || part3 > 255){
                valid = false;
            }
            int part4 = Integer.parseInt(txt4,16);
            if (part4 < 0 || part4 > 255){
                valid = false;
            }
            int part5 = Integer.parseInt(txt5,16);
            if (part5 < 0 || part5 > 255){
                valid = false;
            }
            int part6 = Integer.parseInt(txt6,16);
            if (part6 < 0 || part6 > 255){
                valid = false;
            }
            if(valid){
                String newAddress = txt1+":"+txt2+":"+txt3+":"+txt4+":"+txt5+":"+txt6;
                base.setAdapterAddress(newAddress);
                base.ts("device address set to " +newAddress);
            }else {
                base.ts("invalid Device address!");
                resetAdapterAddressFromBaseclass();
            }
        } catch (NumberFormatException e) {
            base.ts("invalid Device address!");
            resetAdapterAddressFromBaseclass();
        }

    }

    @Override
    public void onBackPressed() {
        this.finish();
    }
}