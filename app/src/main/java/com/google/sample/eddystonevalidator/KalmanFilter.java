package com.google.sample.eddystonevalidator;

public class KalmanFilter {
    private double Q = 0.00001;
    private double R = 0.001;
    private double P = 1;
    private double X = 0;
    private double K;

    KalmanFilter(double initValue){
        X = initValue;
    }
    private void measurementUpdate(){
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (P + Q + R);
    }
    public double update(double measurement){
        measurementUpdate();
        X = X + (measurement - X) * K;
        return X;
    }

}
