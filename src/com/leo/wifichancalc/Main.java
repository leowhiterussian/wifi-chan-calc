package com.leo.wifichancalc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.leoliberman.wifichancalc.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements OnClickListener {
    WifiManager wifi;
    Button buttonScan;
    TextView bestChannel;
    List<ScanResult> results;

    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bestChannel = (TextView) findViewById(R.id.bestChannel);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(this);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Wi-Fi is disabled... Enabling to enable scan", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }   
        
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
               results = wifi.getScanResults();
               findBestChannel();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));                    
    }

    protected void findBestChannel() {
        if (results == null || results.size() == 0) {
            bestChannel.setText("No networks found, lets go with 6");
        }
        SparseIntArray channelsSeen = new SparseIntArray(14);
        
        // iterate through all the found networks, incrementing the count
        for (ScanResult sr : results) {
            int channel = getChannelFromFrequency(sr.frequency);
            channelsSeen.put(channel, channelsSeen.get(channel) + 1);
        }
        
        /*  now we assign a score to each channel
         *  we are going to do this based on how many networks can be found within 2 channels in each direction
         */
        SparseIntArray channelScore = new SparseIntArray(14);
        for (int i = 1 ; i < 15 ; i++) {
            int score = calculateScore(i, channelsSeen);
            channelScore.put(i, score);
        }
        
        // now we go through all the scores we've figured out, and find the best channel
        int minScore = 999999;
        int bestC = 0;
        for(int i = 0; i < channelScore.size(); i++) {
            int key = channelScore.keyAt(i);
            int value = channelScore.get(key);
            if (value < minScore) {
                minScore = value;
                bestC = key;
            }
        }
        bestChannel.setText("Channel " + bestC);
    }
    
    private int calculateScore(int channel, SparseIntArray channelsSeen) {
        int score = 0;
        for (int i = Math.max(1, channel - 2) ; i < Math.min(14, channel + 2) ; i++) {
            int distance = Math.abs(channel - i);
            score += Math.floor(((float) channelsSeen.get(i) * 100)  / ((float) (distance + 1)));
        }
        return score;
    }

    private final static ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
            Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
                    2452, 2457, 2462, 2467, 2472, 2484));

    public static Integer getFrequencyFromChannel(int channel) {
        return channelsFrequency.get(channel);
    }

    public static int getChannelFromFrequency(int frequency) {
        return channelsFrequency.indexOf(Integer.valueOf(frequency));
    }
    
    public void onClick(View view) {
        wifi.startScan();

        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
    }
}