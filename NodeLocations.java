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

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Date;


class ArrayLocations extends ArrayList<SPSData>
{
    ArrayList<SPSData> locations = new ArrayList<SPSData>();
    public ArrayLocations()
    {
        super();
    }

    @Override
    public int size()
    {
        return locations.size();
    }

    @Override
    public SPSData get(int index)
    {
        return locations.get(index);
    }

    @Override
    public boolean add(SPSData x)
    {
        SPSData temp;
        boolean averaged = false;
        //Find node that already exists
        for(int i=0; i<this.locations.size(); i++) {
            if (x.compareTo(this.locations.get(i)) == 1) {
                temp = this.locations.get(i);
                if (temp.num_of_averages > 1) {
                    temp.updateAverage(x.distance, x.rx_pwr);
                    averaged = true;
                } else {
                    this.locations.remove(x);
                }
                break;
            }
        }
        if (!averaged) {
            this.locations.add(x);
        }

        return true;
    }

}
public class NodeLocations {

    private  int averages = 0; //number of averages for node distance calcs.

    private ArrayLocations locations = new ArrayLocations();
    NodePosition currentNodePosition = null;
    private boolean calculated = false;

    public ArrayList<SPSData> getLocations()
    {
        return locations.locations;
    }

    public void addLocation(char[] ssid, int rx_pwr, String bssid, int freq, long application_id,
                            boolean offset_mapping, boolean three_d_mapping)
    {
        SPSData location = new SPSData(averages);
        location.decodeSSID(ssid, rx_pwr, freq, bssid);
        if (location.app_id == application_id && location.off_map == offset_mapping &&
                location.three_d_map == three_d_mapping) {
            locations.add(location);
            calculated = false;
        }
    }
    public void setRollingAverage(int averages)
    {
        this.averages = averages;
    }

    public int size()
    {
        return locations.size();
    }

    public boolean isCalculated()
    {
        return calculated;
    }

    public void killAgedNodes(int secondsAge)
    {
        int i = 0;

        while (i < this.locations.size()) {
            if (((new Date()).getTime() - this.locations.get(i).seen.getTime())/1000 > secondsAge ) {
                this.locations.locations.remove(i);
            } else {
                i++;
            }
        }
    }


    @Override
    public String toString()
    {
        String build = new String();
        if (currentNodePosition != null)
        {
            build = currentNodePosition.lat + ", " + currentNodePosition.lng + ", "
                    + currentNodePosition.altitude;
        }
        return build;
    }

    public void calculatePosition() {
        if (locations.size() > 0) {
            if (locations.size() == 1)
            {
                currentNodePosition = new NodePosition(locations.get(0).lat, locations.get(0).lng,
                        locations.get(0).altitude, locations.get(0).distance);
            }
            else
            {
                currentNodePosition = trilaterate(locations);
            }
            calculated = true;
        }
    }

    private NodePosition trilaterate(ArrayLocations locations) {
        //Now using https://github.com/lemmingapex/Trilateration

        double[][] positions = new double[locations.size()][3];
        double[] distances   = new double[locations.size()];
        int i = 0;
        while (i < locations.size()) {
            positions[i] = new double[]{locations.get(i).lat,locations.get(i).lng,locations.get(i).altitude};
            distances[i] = locations.get(i).distance;
            i++;
        }
        TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());

        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        return new NodePosition(optimum.getPoint().toArray()[0],optimum.getPoint().toArray()[1]);
    }

}
