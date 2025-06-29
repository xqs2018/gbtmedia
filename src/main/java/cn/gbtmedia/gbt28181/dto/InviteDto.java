package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

import java.util.List;

/**
 * @author xqs
 */
@Data
public class InviteDto {

    private String inviteType;

    private long createTime;

    private long viewNum;

    private String deviceId;

    private String channelId;

    private String mediaIp;

    private int mediaPort;

    private String ssrc;

    private String callId;

    private String mediaTransport;

    private String httpFlv;

    private String extInfo;

    private String rxRate;

    private String txRate;

    private String platformId;

    // 级联推流
    private List<InviteDto> platformInvites;

}
