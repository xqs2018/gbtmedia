package cn.gbtmedia.gbt28181.server.sip.session;

import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import lombok.Data;

/**
 * 客户端向服务端下发Invite
 * @author xqs
 */
@Data
public class ClientInvite {

    private MediaType mediaType;

    private long createTime = System.currentTimeMillis();

    private String platformId;

    private String deviceId;

    private String channelId;

    private String mediaIp;

    private int mediaPort;

    private String httpFlv;

    /**
     * 流传输协议
     * upd 服务端向客户端推流
     * tcpPassive 服务端向客户端推流
     * tcpActive 客户端向服务器拉流
     */
    private String mediaTransport;

    private String ssrc;

    private boolean checkSsrc;

    private boolean inviteAck;

    private String callId;

    private String fromTag;

    private String toTag;

    private String viaBranch;

    private boolean sendBye;

    private MediaClient mediaClient;

    private ServerInvite serverInvite;
}
