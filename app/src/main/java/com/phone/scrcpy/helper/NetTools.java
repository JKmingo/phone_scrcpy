package com.phone.scrcpy.helper;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetTools {

    public static String getLocalIpAddress() {
        //https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
        try {
            InetAddress ipv4 = null;
            InetAddress ipv6 = null;

            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet6Address) {
                        ipv6 = inetAddress;
                        continue;
                    }
                    if (inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ipv4 = inetAddress;
                        continue;
                    }
                    return inetAddress.getHostAddress();
                }
            }
            if (ipv6 != null) {
                return ipv6.getHostAddress();
            }
            if (ipv4 != null) {
                return ipv4.getHostAddress();
            }
            return null;

        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
