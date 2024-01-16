package net.gripps.environment;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/15
 */
public class TmpMachine {

    double evalValue;
    CPU CPU;

    public TmpMachine(double evalValue, CPU CPU) {
        this.evalValue = evalValue;
        this.CPU = CPU;
    }

    public double getEvalValue() {
        return evalValue;
    }

    public void setEvalValue(double evalValue) {
        this.evalValue = evalValue;
    }

    public CPU getCPU() {
        return CPU;
    }

    public void setCPU(CPU CPU) {
        this.CPU = CPU;
    }
}
