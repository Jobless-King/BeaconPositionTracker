package com.google.sample.eddystonevalidator;
public class PositionSet{
    private String name;
    private double posX;
    private double posY;
    public PositionSet(String pra_name, double pra_posX, double pra_posY){
        this.name = pra_name;
        this.posX = pra_posX;
        this.posY = pra_posY;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public double getPosX() {
        return posX;
    }
    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }
}
