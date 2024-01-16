package net.gripps.cloud.nfv.main;

public class Sample extends Object{

    private int num;

    public Sample(int num){
        this.num = num;
    }

    public boolean equals(Sample obj){
        if (obj == null){
            return false;
        }

        return this.num == obj.num;
    }

    public void test(){

    }
}
