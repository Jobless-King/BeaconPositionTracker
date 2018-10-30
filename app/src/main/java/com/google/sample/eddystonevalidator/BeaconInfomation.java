package com.google.sample.eddystonevalidator;

import java.util.ArrayList;

/**
 * Created by ymc12 on 2018-09-19.
 */

public class BeaconInfomation {
    private String address;
    private double txpower;
    private double rssi;
    private double distance;
    private double weight;
    private KalmanFilter filter;
    private ArrayList<Double> rawrssi;

    public BeaconInfomation(String address){
        this.address = address;
        filter = new KalmanFilter(0.0f);
        rawrssi = new ArrayList<>();
        distance = 0.0;
        weight = 1;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public KalmanFilter getFilter() {
        return filter;
    }
    public void setFilter(KalmanFilter filter) {
        this.filter = filter;
    }
    public double getTxpower() {
        return txpower;
    }
    public void setTxpower(double txpower) {
        this.txpower = txpower;
    }
    public double getRssi() {
        return rssi;
    }
    public void setRssi(double rssi) {
        this.rssi = rssi;
    }
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public ArrayList<Double> getRawrssi() {
        return rawrssi;
    }
}
