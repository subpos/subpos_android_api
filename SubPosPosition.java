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

public class SubPosPosition {
    private double lat;
    private double lng;
    private double altitude;

    SubPosPosition(Double lat, Double lng, Double altitude) {
        this.lat = lat;
        this.lng = lng;
        this.altitude = altitude;
    }

    public double getLat()
    {
        return this.lat;
    }
    public double getLong()
    {
        return this.lng;
    }
    public double getAltitude()
    {
        return this.altitude;
    }
}
