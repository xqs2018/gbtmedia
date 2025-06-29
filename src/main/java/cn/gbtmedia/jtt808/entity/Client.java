package cn.gbtmedia.jtt808.entity;

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
@Table(name = "jtt808_client",uniqueConstraints={@UniqueConstraint(columnNames={"client_id"})})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 终端手机号
     */
    @Column(name = "client_id",nullable = false)
    private String clientId;

    /**
     * 终端版本
     */
    private String version;

    /**
     * 省域ID
     */
    @Column(name = "province_id")
    private String provinceId;

    /**
     * 市县域ID
     */
    @Column(name = "city_id")
    private String cityId;

    /**
     * 制造商ID
     */
    @Column(name = "maker_id")
    private String makerId;

    /**
     * 终端型号
     */
    @Column(name = "device_model")
    private String deviceModel;

    /**
     * 终端ID
     */
    @Column(name = "device_id")
    private String deviceId;

    /**
     * 车牌颜色
     */
    @Column(name = "plate_color")
    private String plateColor;

    /**
     * 车辆标识
     */
    @Column(name = "plate_no")
    private String plateNo;

    /**
     * 终端ip
     */
    @Column(name = "client_ip")
    private String  clientIp;

    /**
     * 终端port
     */
    @Column(name = "client_port")
    private Integer clientPort;

    /**
     * 终端支持的最大音频物理通道
     */
    @Column(name = "max_audio_channels")
    private Integer maxAudioChannels;

    /**
     * 终端支持的最大视频物理通道
     */
    @Column(name = "max_video_channels")
    private Integer maxVideoChannels;

    /**
     * 音视频通道总数
     */
    @Column(name = "audioVideoChannels")
    private Integer audioVideoChannels;

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
     * 在线
     */
    private Integer online;

    /**
     * 鉴权码
     */
    private String token;

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
}
