package cn.gbtmedia.common.util;

import lombok.extern.slf4j.Slf4j;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author xqs
 */
@Slf4j
public class NetUtil extends cn.hutool.core.net.NetUtil {

    /**
     * 返回本机物理ip地址
     */
    public static String getLocalIp() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return InetAddress.getLocalHost().getHostAddress();
            }
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = allNetInterfaces.nextElement();
                String name = networkInterface.getName().toLowerCase();
                // 物理网卡常见前缀 eth, enp, ens, wlp, wlan, en（macOS）, Ethernet（Windows），Wi-Fi（Windows）
                if (name.startsWith("eth") || name.startsWith("en") || name.startsWith("wl") || name.startsWith("wi")) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("getLocalIp ex", ex);
        }
        return "127.0.0.1";
    }
}
