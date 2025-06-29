package cn.gbtmedia.jtt808.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class ClientMediaDto {

    private String type;

    private long createTime;

    private long viewNum;

    private String mediaIp;

    private int mediaPort;

    private String clientId;

    private Integer channelNo;

    private String mediaKey;

    private String httpFlv;

    private String extInfo;

    private String rxRate;

    private String txRate;
}
