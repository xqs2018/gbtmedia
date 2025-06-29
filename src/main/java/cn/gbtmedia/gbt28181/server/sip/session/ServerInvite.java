package cn.gbtmedia.gbt28181.server.sip.session;

import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端向客户端下发Invite
 * @author xqs
 */
@Data
public class ServerInvite {

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
     * upd 客户端向服务器推流
     * tcpPassive 客户端向服务器推流
     * tcpActive 服务器向客户端拉流(等客户端返回具体的ip和端口，服务器发起请求去拉流)
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

    private Date startTime;

    private Date endTime;

    private Integer downloadSpeed;

    private MediaServer mediaServer;

    /**
     * 级联关联的Invite
     */
    private List<ClientInvite> clientInvites = new CopyOnWriteArrayList<>();
}
