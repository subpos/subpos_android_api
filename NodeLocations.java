package iocomms.subpos;

/* Android API for SubPos (http://www.subpos.org)
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
import org.apache.commons.math3.linear.RealVector;

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
    Position currentNodePosition = null;
    Position currentPosition = null;
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
                    + (currentNodePosition.altitude / 100); //Convert alt to meters
        }
        return build;
    }

    public void calculatePosition() {
        if (locations.size() > 0) {
            if (locations.size() == 1)
            {
                currentPosition = new Position(locations.get(0).lat, locations.get(0).lng,
                        locations.get(0).altitude, locations.get(0).distance);
            }
            else
            {
                currentPosition = trilaterate(locations);
            }
            calculated = true;
        }
    }

    //Cannot debug a single node
    private Position trilaterate(ArrayLocations locations) {
        //Trilateration nonlinear weighted least squares

        //https://github.com/lemmingapex/Trilateration - MIT Licence
        //http://commons.apache.org/proper/commons-math/download_math.cgi


        double[][] positions = new double[locations.size()][3];
        double[] distances   = new double[locations.size()];
        int i = 0;
        while (i < locations.size()) {

            //Map projection is treated as Mercator for calcs
            //Convert lat,lng to meters and then back again
            //Altitude is in cm

            positions[i] = new double[]{WebMercator.latitudeToY(locations.get(i).lat),
                    WebMercator.longitudeToX(locations.get(i).lng),locations.get(i).altitude};

            distances[i] = locations.get(i).distance;
            i++;
        }
        TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());

        LeastSquaresOptimizer.Optimum optimum = solver.solve();
        double[] centroid = optimum.getPoint().toArray();

        double errorRadius = 0;
        boolean errorCalc = false;

        // Error and geometry information
        try {
            //Create new array without the altitude. Including altitude causes a
            //SingularMatrixException as it cannot invert the matrix.
            double[][] err_positions = new double[locations.size()][2];
            i = 0;
            while (i < locations.size()) {

                err_positions[i] = new double[]{positions[i][0],
                        positions[i][1]};
                i++;
            }
            trilaterationFunction = new TrilaterationFunction(err_positions, distances);
            solver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());

            optimum = solver.solve();
            RealVector standardDeviation = optimum.getSigma(0);
            //RealMatrix covarianceMatrix = optimum.getCovariances(0);

            errorRadius = ((standardDeviation.getEntry(0) + standardDeviation.getEntry(1)) / 2) * 100;
            errorCalc = true;

        } catch (Exception ex) {
            errorRadius = 0;
            errorCalc = false;
        }

        return new Position(WebMercator.yToLatitude(optimum.getPoint().toArray()[0]),
                WebMercator.xToLongitude(centroid[1]),centroid[2],
                errorRadius, errorCalc);

    }

}
