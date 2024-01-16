package net.gripps.GJ;

/**
 * Created by kanemih on 2015/11/23.
 */
public class Gauss1 {
    public static void main(String[] args) {
        double [][]a={{1,2,1,2,1,7},{2,3,2,3,2,7},{1,2,3,4,5,7},{4,3,8,1,2,7},{8,2,4,1,9,7}};//拡大行列
        int n=a.length;
        double []b=new double[n];//ベクトルb
        double []x=new double[n];//解ベクトル
        double [][]c=new double[n][n];//行列A
        //ベクトルb、行列Aの生成
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                c[i][j]=a[i][j];
            }
            b[i]=a[i][n];
        }
		/*前進消去過程*/
        for(int k=0;k<n-1;k++){
            for(int i=k+1;i<n;i++){
                double alpha=a[i][k]/a[k][k];
                for(int j=k+1;j<n;j++){
                    a[i][j]=a[i][j]-alpha*a[k][j];
                }
                a[i][n]=a[i][n]-alpha*a[k][n];
            }
        }
		/*後退代入過程*/
        for(int k=n-1;k>=0;k--){
            double sum = 0;
            for(int j=k;j<n-1;j++){
                sum += a[k][j+1]*a[j+1][n];
            }
            a[k][n]=(a[k][n]-sum)/a[k][k];
        }
        //解ベクトルの生成
        for(int i=0;i<n;i++){
            x[i]=a[i][n];
        }
        MatPrint(a);//拡大行列の表示
        System.out.println("残差は"+normInf(VecSub(b,MatVec(c,x))));//残差（∞ノルム）の表示
    }
    static double[] MatVec(double[][] a,double[] b){
        int k=a.length;
        int t=b.length;
        double []e=new double[k];
        for(int i=0;i<k;i++){
            for(int j=0;j<t;j++){
                e[i] += a[i][j]*b[j];
            }
        }
        return e;
    }
    static double[] VecSub(double[] b,double[] d){
        int k=b.length;
        double []e=new double[k];
        for(int i=0;i<k;i++){
            e[i]=b[i]-d[i];
        }
        return e;
    }
    static double normInf(double[] b){
        double x_infty=Math.abs(b[0]);
        for(int i=0;i<b.length;i++){
            if(x_infty<Math.abs(b[i])){
                x_infty=Math.abs(b[i]);
            }
        }
        return x_infty;
    }
    static void MatPrint(double[][] a){
        int m=a.length;
        int n=a[0].length;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                System.out.print(a[i][j]+" ");
            }
            System.out.println();
        }
    }
}
