package org.ncl.workflow.ccn.util;
import java.net.*;
import java.util.*;

/**
 * Created by Hidehiro Kanemitsu on 2019/12/06
 */

public class NetInfo {//ネットワークインターフェイスカードのインターフェイス情報取得
    //すべてのネットワークインターフェイスの名前を列挙表示
    public static void printAllNetworkInterface() throws Exception {
        Enumeration <NetworkInterface> netSet;//集合内の列挙操作に使う古い型
        netSet = NetworkInterface.getNetworkInterfaces();
        while(netSet.hasMoreElements()){//すべてのインターフェイスを走査
            NetworkInterface nInterface = (NetworkInterface) netSet.nextElement();
            System.out.print("  " + nInterface .getName() );//ネットワーク識別名
        }
    }
    // 引数のインターフェイスのIPアドレスをすべて表示
    public static InetAddress[]  printInterfaceAddress(String interfaceName) throws Exception {
        NetworkInterface wlan = NetworkInterface.getByName(interfaceName);
        List<InterfaceAddress>list = wlan.getInterfaceAddresses();
        InetAddress[] addrs = new InetAddress[list.size()];
        int i=0;
        for (InterfaceAddress interfaceAdr : list){//インターフェイス内のアドレス走査
            InetAddress inet2 = interfaceAdr.getAddress();
            addrs[i] = inet2;
            //IP.print(inet2);
            i++;
        }
        return addrs;
    }
    // 取得できるすべてのIPアドレスをすべて表示
    public static LinkedList<InetAddress>  getAllIP() throws Exception {
        Enumeration <NetworkInterface> netSet;//集合内の列挙操作に使う古い型
        netSet = NetworkInterface.getNetworkInterfaces();
        LinkedList<InetAddress> addList = new LinkedList<InetAddress>();

        while(netSet.hasMoreElements()){//すべてのインターフェイスを走査
            NetworkInterface nInterface = (NetworkInterface) netSet.nextElement();
            List<InterfaceAddress>list = nInterface.getInterfaceAddresses();

            if( list.size() == 0 ) continue;
            //System.out.println(nInterface .getName() );//ネットワーク識別名
            for (InterfaceAddress interfaceAdr : list){
                InetAddress inet = interfaceAdr.getAddress();
                addList.add(inet);
               // IP.print(inet);//IPアドレスの表示
            }
        }
        return addList;
    }
    /*
    public static void main(String[] args) throws Exception {
        printAllNetworkInterface();//すべてのネットワークインターフェイスの名前を列挙
        System.out.println("\n以上--------次に「wlan0」インターフェイスが持つIPを列挙");
        printInterfaceAddress("wlan0");
        System.out.println("\n取得できるすべてのIPアドレスをすべて表示");
        printAllIP();
        InetAddress inet = NetInfo.getInetAddress4();
        System.out.println(inet.getHostAddress());
    }
　	// 最初に取得できたループバック以外のPv4のアドレスを返す。
    public static InetAddress getInetAddress4() throws Exception {
        InetAddress rtnInet = null;
        Enumeration <NetworkInterface> netSet;//集合内の列挙操作用
        netSet = NetworkInterface.getNetworkInterfaces();
        while(netSet.hasMoreElements()){//すべてのインターフェイスを走査
            NetworkInterface nInterface = (NetworkInterface) netSet.nextElement();
            List<InterfaceAddress>list = nInterface.getInterfaceAddresses();
            if( list.size() == 0 ) continue;
            for (InterfaceAddress interfaceAdr : list){
                InetAddress inet = interfaceAdr.getAddress();
                if(inet.isLoopbackAddress() ) continue;
                if(inet.getClass() == Inet4Address.class) {
                    rtnInet = inet;
                }
            }
        }
        return rtnInet;
    }

     */
}