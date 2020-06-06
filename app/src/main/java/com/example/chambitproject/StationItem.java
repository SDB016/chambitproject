package com.example.chambitproject;

public class StationItem {
    private int distance;
    private String name;
    private double x;
    private double y;

    public String getName() {
        return this.name;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public int getDistance() {
        return this.distance;
    }

    public void setName(String name2) {
        this.name = name2;
    }

    public void setDistance(int distance2) {
        this.distance = distance2;
    }

    public void setX(double x2) {
        this.x = x2;
    }

    public void setY(double y2) {
        this.y = y2;
    }
}