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

import java.util.Date;
import java.util.ArrayList;


public class SPSData implements Comparable<SPSData>{
    long      dev_id;
    double    lat;
    double    lng;
    double    altitude;
    double    tx_pwr;
    boolean   off_map;
    boolean   three_d_map;
    long      res;
    long      app_id;
    int       environment;
    Date      seen;
    double    rx_pwr;      //RSSI
    String    bssid;
    int       freq;
    double    distance;
    ArrayList<Double> distances = new ArrayList<Double>();
    int num_of_averages;


    SPSData(int averages) {
        this.num_of_averages = averages;
    }
    SPSData(long lat,long lng,int tx_pwr,int rx_pwr,long altitude, int averages)
    {
        this.lat = lat;
        this.lng = lng;
        this.tx_pwr = tx_pwr;
        this.rx_pwr = rx_pwr;
        this.altitude = altitude;
        this.num_of_averages = averages;

    }

    @Override
    public int compareTo(SPSData otherData) {
        if (this.bssid.toString().equals(otherData.bssid.toString()) && this.dev_id == otherData.dev_id)
        {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString()
    {
        String data = new String();
        data = data + this.lat + ", ";
        data = data + this.lng + ", ";
        data = data + (this.altitude / 100) + ", ";
        data = data + ((double)((long)(this.distance * 10000000)))/100 + "m";

        return data;
    }


    void decodeSSID(char[] strDecode, double rx_pwr, int freq, String bssid)
    {
        try {
            byte ssid[] = new byte[31];

            int i = 0;
            for (char c : strDecode) {
                if (i < 31) {
                    ssid[i] = (byte)c;
                }
                i++;
            }

            //Check coding bits and reconstruct data
            //we don't have to extract and check the coding mask bits
            //if we work from the right

            int x;
            int y = 0;

            for (x = 30; x >= 24; x--) {
                if (((ssid[30] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] - 1);
                y++;
            }

            y = 0;
            for (x = 23; x >= 17; x--) {
                if (((ssid[29] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] - 1);
                y++;
            }

            y = 0;
            for (x = 16; x >= 10; x--) {
                if (((ssid[28] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] - 1);
                y++;
            }

            y = 0;
            for (x = 9; x >= 3; x--) {
                if (((ssid[27] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] - 1);
                y++;
            }

            //Now pull out the "ASCII" mask
            y = 0;
            for (x = 23; x >= 17; x--) {
                if (((ssid[26] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] | 0x80);
                y++;
            }

            y = 0;
            for (x = 16; x >= 10; x--) {
                if (((ssid[25] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] | 0x80);
                y++;
            }

            y = 0;
            for (x = 9; x >= 3; x--) {
                if (((ssid[24] >> y) & 0x1) == 1)
                    ssid[x] = (byte) (ssid[x] | 0x80);
                y++;
            }

            //Extra masking needed in java to make it react properly to "negative" octets (chars and
            //bytes in java seem to be handled as signed when shifting).
            //Any of the upper bits shouldn't be masked on signed values.

            this.dev_id = ssid[3] << 16 | (ssid[4] & 0xFF) << 8 | (ssid[5] & 0xFF);
            this.lat = ((double) (ssid[6] << 24 | (ssid[7] & 0xFF) << 16 | (ssid[8] & 0xFF) << 8 |
                    (ssid[9] & 0xFF))) / 10000000;
            this.lng = ((double) (ssid[10] << 24 | (ssid[11] & 0xFF) << 16 | (ssid[12] & 0xFF) << 8 |
                    (ssid[13] & 0xFF))) / 10000000;
            this.app_id = ssid[14] << 16 | ssid[15] << 8 | ssid[16];

            this.altitude = ((double) (ssid[17] << 18 | ssid[18] << 10 | ssid[19] << 2 |
                    ((ssid[20] >> 6) & 0x03)));
            if (((ssid[20] & 0x20) >> 5) != 0) {
                this.altitude = (this.altitude * -1);
            }
            this.off_map = ((ssid[20] & 0x10) >> 4) != 0;
            this.three_d_map = ((ssid[20] & 0x08) >> 3) != 0;
            this.tx_pwr = ((ssid[20] & 0x07) << 8) | ssid[21];
            this.tx_pwr = (this.tx_pwr - 1000) / 10;

            this.environment = (ssid[22] & 0xE0) >> 5;
            this.res = (ssid[22] & 0x1F << 8) | ssid[23];

            this.seen = new Date();
            this.rx_pwr = rx_pwr;
            this.bssid = bssid;
            this.freq = freq;

            distanceCalc(this.tx_pwr, this.rx_pwr, this.freq, this.environment);
            this.distances.add(this.distance);

        } catch (Exception ex) {}

    }

    public void updateAverage(double distance, double rx_pwr){
        this.distances.add(distance);
        this.seen           = new Date();
        this.rx_pwr         = rx_pwr;
        if (this.num_of_averages > 1) {
            if (this.distances.size() > this.num_of_averages) {
                this.distances.remove(0);
            }
        }

        this.distance = average(distances);
    }

    //Rolling average with median filter
    private double average(ArrayList<Double> toAverage)
    {
        ArrayList<Double> tempAverage = new ArrayList<Double>(toAverage);
        double sum = 0;
        int remove;

        Collections.sort(tempAverage);
        remove = (int)(tempAverage.size()/4); //median removal size

        //Get the Average of the median values
        for ( int x = remove; x < (tempAverage.size() - 1) - remove; x++)
        {
            sum = sum + tempAverage.get(x).doubleValue();
        }
        return sum / (tempAverage.size() - (2 * remove));
    }

    public double distanceCalc(double tx_pwr, double rx_pwr, int frequency, int coefficient)    {
        //Adapted from "Robust Indoor Wi-Fi Positioning System for Android-based Smartphone"
        //http://www.academia.edu/5075508/Robust_Indoor_Wi-Fi_Positioning_System_for_
        //Android-_based_Smartphone

        //Pr = Pt - Lu
        //Lu = 10log10(4*pi/lambda) + 20log10(d)*mu
        //lambda = c/f

        //20*mu*log10(d) = Lu - 10log10(4*pi/lambda)
        //log10(d)       = (Lu - 10log10(4*pi/lambda))/(20*mu)
        //d = 10^(Lu - 10log10(4*pi/lambda))/(20*mu)
        //d = (Pt - Pr - 10log10(4*pi/lambda))/(20*mu)

        //Mu
        // 2   - Outdoors with clear conditions
        // 2.5 - Indoors with clear conditions
        // 3.0 - Indoors with moderately clear conditions
        // 3.5 - Indoors with moderately bad conditions
        // 4.0 - Indoors with bad conditions
        // 4.5 - Indoors with terrible conditions
        // 5.0 - Indoors with horrendous conditions


        double c = 299.792458; //We will use MHz for freq.

        double mu;
        switch (coefficient) {
            case 0:
                mu = 1;
                break;
            case 1:
                mu = 2.0;
                break;
            case 2:
                mu = 2.5;
                break;
            case 3:
                mu = 3.0;
                break;
            case 4:
                mu = 3.5;
                break;
            case 5:
                mu = 4.0;
                break;
            case 6:
                mu = 4.5;
                break;
            case 7:
                mu = 5.0;
                break;
            default:
                mu = 1;
                break;
        }


        double exp = (((double)(tx_pwr/10)) - rx_pwr -
                10*Math.log10(4*Math.PI/(c/frequency)))/(20*mu);
        //Need to change to GPS precision; divide meters by /100000
        this.distance = Math.pow(10.0, exp) / 100000;
        //Return meters
        return Math.pow(10.0, exp);
    }
}
