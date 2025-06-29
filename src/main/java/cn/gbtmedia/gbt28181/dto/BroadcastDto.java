package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class BroadcastDto {

    private String callId;

    private String ssrc;

    private String httpWs;

    private String httpFlv;
}
