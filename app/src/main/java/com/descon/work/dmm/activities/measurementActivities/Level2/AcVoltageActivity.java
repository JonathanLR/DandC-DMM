package com.descon.work.dmm.activities.measurementActivities.Level2;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.descon.work.dmm.utilityClasses.BaseApplication;
import com.descon.work.dmm.utilityClasses.MessageCode;
import com.descon.work.dmm.R;
import com.descon.work.dmm.displayAndVisualisationClasses.Speedometer;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AcVoltageActivity extends AppCompatActivity {
    private static BaseApplication base;
    private static LineChart loggingLineChart;
    private Speedometer gauge;
    boolean isLogging = false;
    private int xoffset = 0;
    private float voltage=0;
    private boolean currentVoltageLogged = true;

    private static ArrayList<Entry> entry_list = new ArrayList<Entry>();

    private String[] unitsForRanges = {"V(RMS)","mV(RMS)","mV(RMS)","mV(RMS)"};
    private float[] maxValuesForRanges = {10,1000,100,10};
    private float[] minValuesForRanges = {0,0,0,0};
    private int currentRange = -1;//-1 to ensure range update on first intent
    private float maxVoltage = 10f;
    private float signalFreq= Float.NaN;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == MessageCode.PARSED_AC_VOLTAGE) {
                parseRange(intent.getIntExtra(MessageCode.RANGE,0));
                voltage = intent.getFloatExtra(MessageCode.VALUE,0f);
                float adjustedVoltage = valueToRangeAdjustment(currentRange,voltage);
                int freqInHz = intent.getIntExtra(MessageCode.EXTRA,-1);
                String freqUnit= "";
                //parsing frequency
                if(freqInHz <= 1000){
                    signalFreq = ((float)freqInHz);//Hz
                    freqUnit = "Hz";
                }else if (freqInHz > 1E3 && freqInHz <= 1E6){
                    signalFreq = (float) (((float)freqInHz)/1E3);//kHz
                    freqUnit = "kHz";
                }else if (freqInHz > 1E6 && freqInHz <= 1E9){
                    signalFreq = (float) (((float)freqInHz)/1E6);//MHz
                    freqUnit = "MHz";
                }else{
                    signalFreq = Float.NaN;
                }
                String freqString = String.format(" Frequency:%.2f%s",signalFreq,freqUnit);
                frequency_text.setText(freqString);
                rangeBoundaryProximity(adjustedVoltage,minValuesForRanges[currentRange],maxValuesForRanges[currentRange]);
                gauge.onSpeedChanged(adjustedVoltage);
                if (isLogging) {
                    Entry e =new Entry((float)xoffset,voltage);
                    entry_list.add(e);
                    if(entry_list.size()>0){
                        genChart();
                    }
                    xoffset++;
                }else{
                    currentVoltageLogged = false;
                }
            }else{//inside voltage activity but received wrong packet. send change mode packet
                Intent change_mode = new Intent(MessageCode.DMM_CHANGE_MODE_REQUEST);
                change_mode.putExtra(MessageCode.MODE,MessageCode.AC_VOLTAGE_MODE);
                sendBroadcast(change_mode);
                frequency_text.setText("DMM in incorrect mode...");
            }
        }
    };

    /*take the range value and set the correct units for display in gauge*/
    private void parseRange(int range){
        //check if the range has changed
        if(currentRange != range){
            //assigning new gauge values and units
            gauge.setMin(minValuesForRanges[range]);
            gauge.setMax(maxValuesForRanges[range]);
            gauge.setUnit(unitsForRanges[range]);
            currentRange = range;
        }
    }

    /*converts input voltage (e.g 0.3V) to the current range, so for example 0.3V->300mV*/
    private float valueToRangeAdjustment(int range, float voltage){
        if(voltage<0){
            voltage=0;
        }
        switch(range){
            case 0:
                return voltage*1;//V
            case 1:
                return voltage*1000;//mV
            case 2:
                return voltage*1000;//mV
            case 3:
                return voltage*1000;//mV
        }
        return 0;
    }

    private void rangeBoundaryProximity(float value,float minVal,float maxVal){
        if(value > maxVal || value < minVal){
            base.vibratePulse(500);
        }else if(value >= maxVal*0.9f || value <= minVal*0.9f){
            base.vibratePulse(250);
        }
    }

    protected TextView frequency_text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ac_voltage);
        getSupportActionBar().setTitle("AC Voltage mode");
        base = (BaseApplication)getApplicationContext();
        gauge = (Speedometer) findViewById(R.id.ac_gauge);
        gauge.setMax(10);
        gauge.setMin(0);
        gauge.setCurrentSpeed(0);
        gauge.setUnit("V(RMS)");
        /*Registering all message types so that application can send switch mode packet if the
        * wrong packet type is received*/
        registerReceiver(receiver,base.intentFILTER);

        //Line Chart
        loggingLineChart = (LineChart) findViewById(R.id.ac_loggingLineChart);
        frequency_text = (TextView) findViewById(R.id.ac_freq_text);
        entry_list = new ArrayList<Entry>();

        //Data Logging Toggle
        Switch dataLogTog = (Switch) findViewById(R.id.ac_loggingToggle);
        dataLogTog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAutoLogging();
            }
        });
        //asking dmm to change to Ac mode
        Intent change_mode = new Intent(MessageCode.DMM_CHANGE_MODE_REQUEST);
        change_mode.putExtra(MessageCode.MODE,MessageCode.AC_VOLTAGE_MODE);
        sendBroadcast(change_mode);
    }

    private void toggleAutoLogging(){
        isLogging = !isLogging;
    }

    public void logVoltage(View view){
        if (!currentVoltageLogged) {
            Entry e =new Entry((float)xoffset,voltage);
            entry_list.add(e);
            if(entry_list.size()>0){
                genChart();
            }
            xoffset++;
            currentVoltageLogged = true;
        }else{
            base.ts("No new data packages");
        }
    }

    public void onClickExportData(View view){
        DialogFragment exportDialog = new AcVoltageActivity.exportDialogFragment();
        exportDialog.show(getSupportFragmentManager(),"exportFragmentAcVoltage");
    }

    public void onClickClearLog(View view){
        entry_list = new ArrayList<>();
        loggingLineChart.clear();
        loggingLineChart.invalidate();
    }


    private void genChart() {
        //Generate new dataset
        List<Entry> entries = entry_list;
        //limit the number of entries
        if (entries.size() > base.getMaxDataPointsToKeep()){
            entries.remove(0);
        }
        LineDataSet dataSet = new LineDataSet(entries, "Voltage(RMS)"); // add entries to dataset
        dataSet.setColor(ColorTemplate.COLORFUL_COLORS[1]);
        dataSet.setValueTextColor(Color.BLACK);
        LineData lineData = new LineData(dataSet);
        loggingLineChart.clear();
        Description desc = new Description();
        desc.setText("X: measurement number Y: Voltage(RMS)");
        loggingLineChart.setDescription(desc);
        loggingLineChart.setData(lineData);
        loggingLineChart.invalidate(); // refresh
        loggingLineChart.notifyDataSetChanged();//Causes redraw when we add data. I imagine we'll initiate
    }

    public static class exportDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String[] items = {"Export chart data points and send them via email",
                    "Save chart image to the phone image gallery",
                    "Cancel"};
            builder.setTitle("Chart export options")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                    exportData();
                                    break;
                                case 1:
                                    exportImage();
                                    break;
                                case 2:
                                    base.ts("Export canceled.");
                                    return;
                            }
                        }
                    });
            return builder.create();
        }
    }

    private static void  exportData(){
        if(entry_list.size()<=0){
            base.ts("No chart data to export, Aborting.");
            return;
        }
        File root   = Environment.getExternalStorageDirectory();
        File dir    =   new File (root.getAbsolutePath() + "/DMM_Saved_Logs");
        if(!dir.exists()){
            dir.mkdirs();
        }
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        File file   =   new File(dir, "AcVoltageLog_"+currentDateTimeString+".txt");
        StringBuilder dataString = new StringBuilder("");
        ArrayList<Entry> copyList = new ArrayList<>(entry_list);
        for(int i=0;i<copyList.size();i++){
            String dataPoint = "x:"+copyList.get(i).getX()+" y:"+copyList.get(i).getY()+"\n";
            dataString.append(dataPoint);
        }
        String data = dataString.toString();
        try {
            if (data.length()>0) {
                FileOutputStream out = new FileOutputStream(file);
                out.write(data.getBytes());
                out.close();
                // try to send file via email
                Uri fileLocation  =   Uri.fromFile(file);
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "DMM Voltage data");
                sendIntent.putExtra(Intent.EXTRA_STREAM, fileLocation);
                sendIntent.setType("text/html");
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                base.startActivity(sendIntent);
            }
        } catch (Exception e) {
            base.ts("ERROR encountered when trying to export data.");
            e.printStackTrace();
        }
    }

    private static void exportImage(){
        if(entry_list.size()<= 0){
            base.t("Graph empty, aborting image export");
            return;
        }
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String imageName =   "AcVoltageGraphImage_"+currentDateTimeString;
        loggingLineChart.saveToGallery(imageName,100);
        base.ts("Image saved to Gallery as: "+imageName);
    }

    @Override
    protected void onDestroy() {
        //preventing receiver leaking
        this.unregisterReceiver(this.receiver);
        super.onDestroy();
    }
}
