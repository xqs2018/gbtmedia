package cn.gbtmedia.gbt28181.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.util.Date;

/**
 * @author xqs
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gbt28181_device",uniqueConstraints={@UniqueConstraint(columnNames={"device_id"})})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 设备Id
     */
    @Column(name = "device_id",nullable = false)
    private String deviceId;

    /**
     * 设备名
     */
    private String name;

    /**
     * 自定义名称
     */
    @Column(name = "custom_name")
    private String customName;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 型号
     */
    private String model;

    /**
     * 固件版本
     */
    private String firmware;

    /**
     * 信令传输协议
     * UDP/TCP
     */
    @Column(name = "sip_transport")
    private String sipTransport;

    /**
     * 编码
     */
    private String charset;

    /**
     * 信令ip
     */
    @Column(name = "sip_ip")
    private String  sipIp;

    /**
     * 信令port
     */
    @Column(name = "sip_port")
    private Integer sipPort;

    /**
     * 注册时间
     */
    @Column(name = "regist_time")
    private Date registTime;

    /**
     * 心跳时间
     */
    @Column(name = "keepalive_time")
    private Date keepaliveTime;

    /**
     * 注册有效期
     */
    private Integer expires;

    /**
     * 在线
     */
    private Integer online;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 流传输协议
     * upd 设备向服务器推流
     * tcpPassive 设备向服务器推流
     * tcpActive 服务器向设备拉流(等设备返回具体的ip和端口，服务器发起请求去拉流)
     */
    @Column(name = "media_transport")
    private String mediaTransport;

    /**
     * 最多几路播放流
     */
    @Column(name = "max_play_stream")
    private Integer maxPlayStream;

    /**
     * 最多几路回放流
     */
    @Column(name = "max_playback_stream")
    private Integer maxPlaybackStream;

    /**
     * 最多几路下载流
     */
    @Column(name = "max_download_stream")
    private Integer maxDownloadStream;
}
