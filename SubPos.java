package iocomms.subpos;

/* SSID Android API for SubPos (http://www.subpos.org)
 * Copyright (C) 2015 Blair Wyatt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import java.util.List;

public class SubPos {

    private WifiManager mainWifiObj;
    private WifiScanReceiver wifiReceiver;
    private NodeLocations SPSNodes = new NodeLocations();
    private Context mContext;
    private boolean three_d_map;
    private boolean offset_mapping;
    private long application_id;
    private int rolling_average_length;
    private int age; //Default age of 30 seconds; remove anything over 30 seconds old

    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
            if (wifiScanList.size() != 0) {
                SPSNodes.killAgedNodes(age);
                for (int i = 0; i < wifiScanList.size(); i++) {
                    if (wifiScanList.get(i).SSID.startsWith("SPS")) {
                        SPSNodes.addLocation(wifiScanList.get(i).SSID.toCharArray(),
                                wifiScanList.get(i).level,
                                wifiScanList.get(i).BSSID,
                                wifiScanList.get(i).frequency,
                                application_id, offset_mapping, three_d_map);
                    }
                }
                if (SPSNodes.size() > 0) {
                    SPSNodes.calculatePosition();
                }
            }
        }
    }
    //Create a SubPos positioning object that will obtain position from nodes that belong to the
    //appropriate application ID and positioning type (3d,offset). If you don't have a specific
    //application ID for the positioning project, just use 0.
    public SubPos(Context mContext, int nodeAge, int rolling_average_length, boolean offset_mapping,
                  boolean three_d_mapping, long application_id) {
        this.three_d_map = three_d_mapping;
        this.offset_mapping = offset_mapping;
        this.application_id = application_id;
        this.rolling_average_length = rolling_average_length;
        this.age = nodeAge;

        SPSNodes.setRollingAverage(this.rolling_average_length);

        this.mContext = mContext;
        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();

        mainWifiObj.startScan();
    }

    public SubPos(Context mContext, boolean offset_mapping,
                  boolean three_d_mapping, long application_id) {
        this.three_d_map = three_d_mapping;
        this.offset_mapping = offset_mapping;
        this.application_id = application_id;
        this.rolling_average_length = 10;
        this.age = 30;

        SPSNodes.setRollingAverage(this.rolling_average_length);

        this.mContext = mContext;
        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();

        mainWifiObj.startScan();
    }

    public SubPos(Context mContext) {
        this.three_d_map = false;
        this.offset_mapping = false;
        this.application_id = 0;
        this.rolling_average_length = 10;
        this.age = 30;

        SPSNodes.setRollingAverage(this.rolling_average_length);

        this.mContext = mContext;
        mainWifiObj = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();

        mainWifiObj.startScan();
    }

    //Call this on the object when the application is paused.
    public void pause() {
        mContext.unregisterReceiver(wifiReceiver);
    }

    //Call this on the object when the application resumes.
    public void resume() {
        mContext.registerReceiver(wifiReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    //Get the current position as calculated from the visible nodes.
    //Return null if nothing is calculated.
    public SubPosPosition getPosition()
    {
        if (SPSNodes.isCalculated()) {
            return new SubPosPosition(SPSNodes.currentNodePosition.lat,
                    SPSNodes.currentNodePosition.lng, SPSNodes.currentNodePosition.altitude);
        } else {
            return null;
        }
    }
}
