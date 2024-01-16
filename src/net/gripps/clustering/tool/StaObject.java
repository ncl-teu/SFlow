package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.BBTask;

/**
 * Created by IntelliJ IDEA.
 * User: kanemih
 * Date: 2009/12/26
 * Time: 22:45:05
 * To change this template use File | Settings | File Templates.
 */
public class StaObject {
    private double makeSpanRate;
    private double levelRate;
    private int clusterNum;

    public StaObject() {
        this.makeSpanRate = 0.0;
        this.levelRate = 0.0;
        this.clusterNum = 0;

    }

    public double getMakeSpanRate() {
        return makeSpanRate;
    }

    public void setMakeSpanRate(double makeSpanRate) {
        this.makeSpanRate = makeSpanRate;
    }

    public void addMakeSpanRate(double value){
        this.makeSpanRate += value;
    }

    public double getLevelRate() {
        return levelRate;
    }

    public void setLevelRate(double levelRate) {
        this.levelRate = levelRate;
    }

    public void addLevelRate(double value){
        this.levelRate += value;
    }

    public int getClusterNum() {
        return clusterNum;
    }

    public void setClusterNum(int clusterNum) {
        this.clusterNum = clusterNum;
    }

    public void addClusterNum(int value){
        this.clusterNum += value;
    }
}
