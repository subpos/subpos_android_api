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

public class NodePosition {
    double      lat;
    double      lng;
    double      altitude;
    double      distance;

    NodePosition(double x, double y)
    {
        lat = x;
        lng = y;
    }

    NodePosition(double x, double y, double z, double d)
    {
        lat = x;
        lng = y;
        altitude = z;
        distance = d;

    }

    @Override
    public String toString()
    {
        String data = new String();
        data = data + ((double)((long)(this.lat * 10000000)))/10000000 + ", ";
        data = data + ((double)((long)(this.lng * 10000000)))/10000000 + ", ";
        data = data + this.altitude;


        return data;
    }
}
