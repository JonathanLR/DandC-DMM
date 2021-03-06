package com.descon.work.dmm.activities.measurementActivities.Level1;

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

public class ResistanceActivity extends AppCompatActivity {
    private static BaseApplication base;
    private static LineChart loggingLineChart;
    private Speedometer gauge;
    boolean isLogging = false;
    private int xoffset = 0;
    private float resistance =0;
    private boolean currentVoltageLogged = false;

    private static ArrayList<Entry> entry_list = new ArrayList<Entry>();

    private String[] unitsForRanges = {"Ω","kΩ","kΩ","kΩ"};
    private float[] maxValuesForRanges = {1000,10,100,1000};
    private float[] minValuesForRanges = {0,0,0,0};
    private int currentRange = -1;//-1 to ensure range update on first intent

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == MessageCode.PARSED_DATA_RESISTANCE) {
                resistance = intent.getFloatExtra(MessageCode.VALUE,0f);
                float adjustedResistance = valueToRangeAdjustment(resistance);
                rangeBoundaryProximity(adjustedResistance,minValuesForRanges[currentRange],maxValuesForRanges[currentRange]);
                gauge.onSpeedChanged(adjustedResistance);
                if (isLogging) {
                    Entry e =new Entry((float)xoffset, resistance);
                    entry_list.add(e);
                    if(entry_list.size()>0){
                        genChart();
                    }
                    xoffset++;
                }else{
                    currentVoltageLogged = false;
                }
            }else{//inside resistance activity but received wrong packet. send change mode packet
                Intent change_mode = new Intent(MessageCode.DMM_CHANGE_MODE_REQUEST);
                change_mode.putExtra(MessageCode.MODE,MessageCode.RESISTANCE_MODE);
                sendBroadcast(change_mode);
            }
        }
    };

    private void rangeBoundaryProximity(float value,float minVal,float maxVal){
        if(value > maxVal || value < minVal){
            base.vibratePulse(500);
        }else if(value >= maxVal*0.9f || value <= minVal*1.1f){
            base.vibratePulse(250);
        }
    }

    /*converts input resistance (e.g 0.3V) to the current range, so for example 0.3V->300mV*/
    private float valueToRangeAdjustment(float resistance){
        currentRange = 0;
        //preventing extremes
        if(resistance<0){
            resistance = 0;
        }else if(resistance > 1E6){
            resistance = 1E6f;
        }

        //gauge limits and units
        gauge.setMin(0);// min is always 0
        if(resistance<1E3){
            resistance /= 1;
        }else if(resistance >= 1E3 && resistance < 1E4){
            currentRange = 1;
            resistance /= 1000;
        }else if(resistance >= 1E4 && resistance < 1E5){
            currentRange = 2;
            resistance /= 1000;
        }else if(resistance >= 1E5 && resistance <= 1E6){
            currentRange = 3;
            resistance /= 1000;
        }
        gauge.setUnit(unitsForRanges[currentRange]);
        gauge.setMin(minValuesForRanges[currentRange]);
        gauge.setMax(maxValuesForRanges[currentRange]);
        return resistance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resistance);
        getSupportActionBar().setTitle("Resistance mode");
        base = (BaseApplication)getApplicationContext();
        gauge = (Speedometer) findViewById(R.id.ResistanceGauge);
        gauge.setMax(1000);
        gauge.setMin(0);
        gauge.setCurrentSpeed(0);
        gauge.setUnit("Ω");
        /*Registering all message types so that application can send switch mode packet if the
        * wrong packet type is received*/
        registerReceiver(receiver,base.intentFILTER);

        //Line Chart
        loggingLineChart = (LineChart) findViewById(R.id.loggingLineChart_Resistance);
        entry_list = new ArrayList<Entry>();

        //Data Logging Toggle
        Switch dataLogTog = (Switch) findViewById(R.id.autoLogResistance_swt);
        dataLogTog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAutoLogging();
            }
        });
        //ask DMM to switch to resistance mode
        Intent change_mode = new Intent(MessageCode.DMM_CHANGE_MODE_REQUEST);
        change_mode.putExtra(MessageCode.MODE,MessageCode.RESISTANCE_MODE);
        sendBroadcast(change_mode);
    }

    private void toggleAutoLogging(){
        isLogging = !isLogging;
    }

    public void logResistance(View view){
        if (!currentVoltageLogged) {
            Entry e =new Entry((float)xoffset, resistance);
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
        DialogFragment exportDialog = new exportDialogFragment();
        exportDialog.show(getSupportFragmentManager(),"exportFragmentResistance");
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
        LineDataSet dataSet = new LineDataSet(entries, "Resistance"); // add entries to dataset
        dataSet.setColor(ColorTemplate.COLORFUL_COLORS[1]);
        dataSet.setValueTextColor(Color.BLACK);
        LineData lineData = new LineData(dataSet);
        loggingLineChart.clear();
        Description desc = new Description();
        desc.setText("X: measurement number Y: Resistance");
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
        File file   =   new File(dir, "ResistanceLog_"+currentDateTimeString+".txt");
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
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "DMM Resistance data");
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
        String imageName =   "ResistanceGraphImage_"+currentDateTimeString;
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
