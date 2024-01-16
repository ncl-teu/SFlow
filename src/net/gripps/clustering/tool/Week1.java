package net.gripps.clustering.tool;

/**
 * Author: H. Kanemitsu
 * Date: 2009/04/17
 */
class Week1{
	public static void main(String[] args){
		if(args == null){
			System.out.println("unko");
		}else{
			System.out.println("OK");
		}
       



        int len = args.length;
        for(int i=0;i<len;i++){
            System.out.println(args[i]);
        }

    }
}

