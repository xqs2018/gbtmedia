package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xqs
 */
@Data
public class QueryParam {

    private int pageNo = 0;

    private int pageSize = 10;

    private String deviceId;

    private String channelId;

    private Integer online;

    private String ssrc;

    private String callId;

    private Date startTime;

    private Date endTime;

    private int downloadSpeed = 4;

    private String platformId;

    // 0:停止 1:左移 2:右移
    private int leftRight;

    // 0:停止 1:上移 2:下移
    private int upDown;

    // 镜头放大缩小 0:停止 1:缩小 2:放大
    private int inOut;

    // 镜头移动速度
    private int moveSpeed = 255;

    // 镜头缩放速度
    private int zoomSpeed = 1;

    private double speed;

    private String sipIp;
}
