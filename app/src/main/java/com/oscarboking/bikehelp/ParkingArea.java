package com.oscarboking.bikehelp;

/**
 * Created by boking on 2017-09-10.
 */

public class ParkingArea {
    private String id;
    private double latitude;
    private double longitude;
    private String address;
    private int parkingSpaces;

    public ParkingArea(String id, double latitude, double longitude,int parkingSpaces, String address){
        this.id = id;
        this.latitude=latitude;
        this.longitude=longitude;
        this.parkingSpaces = parkingSpaces;
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getParkingSpaces() {
        return parkingSpaces;
    }

    public void setParkingSpaces(int parkingSpaces) {
        this.parkingSpaces = parkingSpaces;
    }
}
