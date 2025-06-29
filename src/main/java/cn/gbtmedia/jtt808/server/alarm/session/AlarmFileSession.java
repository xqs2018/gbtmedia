package cn.gbtmedia.jtt808.server.alarm.session;

import io.netty.channel.Channel;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xqs
 */
@Data
public class AlarmFileSession {

    private Channel channel;

    private long createTime;

    private long lastAccessedTime;

    /**
     * 终端手机号
     */
    private String clientId;

    /**
     * 真实版本标记  2019  2013  2011
     */
    private int version;

    /**
     * 递增的序列号
     */
    private final AtomicInteger serialNo = new AtomicInteger(1);

    /**
     * 客户端地址
     */
    private InetSocketAddress socketAddress;

}
