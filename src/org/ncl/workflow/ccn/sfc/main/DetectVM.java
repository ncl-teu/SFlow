package org.ncl.workflow.ccn.sfc.main;

import org.ncl.workflow.ccn.util.ResourceMgr;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/04.
 */

public class DetectVM {
    private static final Map<String, String> vmMacAddressOUI = new HashMap();
    private static final String[] vmModelArray;

    public DetectVM() {
    }

    public static void main(String[] args) {
        String vmString = identifyVM();
        if (vmString.isEmpty()) {
            System.out.println("You do not appear to be on a Virtual Machine.");
        } else {
            System.out.println("You appear to be on a VM: " + vmString);
        }

    }

    public static String identifyVM() {
        ResourceMgr.getIns().initResource();

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();
        NetworkIF[] nifs = hw.getNetworkIFs();
        NetworkIF[] var3 = nifs;
        int var4 = nifs.length;

        int var5;
        String vm;
        for(var5 = 0; var5 < var4; ++var5) {
            NetworkIF nif = var3[var5];
            vm = nif.getMacaddr().toUpperCase();
            String oui = findOuiByMacAddressIfPossible(vm);
            if (oui != null && !oui.isEmpty()) {
                return oui;
            }
        }

        String model = hw.getComputerSystem().getModel();
        String[] var10 = vmModelArray;
        var5 = var10.length;

        for(int var12 = 0; var12 < var5; ++var12) {
            vm = var10[var12];
            if (model.contains(vm)) {
                return vm;
            }
        }

        String manufacturer = hw.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) {
            return "Microsoft Hyper-V";
        } else {
            return "";
        }
    }

    public static String findOuiByMacAddressIfPossible(String mac) {
        return (String)vmMacAddressOUI.entrySet().stream().filter((entry) -> {
            return mac.startsWith((String)entry.getKey());
        }).map(Map.Entry::getValue).collect(Collectors.joining());
    }

    static {
        vmMacAddressOUI.put("00:50:56", "VMware ESX 3");
        vmMacAddressOUI.put("00:0C:29", "VMware ESX 3");
        vmMacAddressOUI.put("00:05:69", "VMware ESX 3");
        vmMacAddressOUI.put("00:03:FF", "Microsoft Hyper-V");
        vmMacAddressOUI.put("00:1C:42", "Parallels Desktop");
        vmMacAddressOUI.put("00:0F:4B", "Virtual Iron 4");
        vmMacAddressOUI.put("00:16:3E", "Xen or Oracle VM");
        vmMacAddressOUI.put("08:00:27", "VirtualBox");
        vmMacAddressOUI.put("02:42:AC", "Docker Container");
        vmModelArray = new String[]{"Linux KVM", "Linux lguest", "OpenVZ", "Qemu", "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels", "Linux Containers", "LXC"};
    }
}
