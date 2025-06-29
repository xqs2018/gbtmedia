package cn.gbtmedia.gbt28181.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(name = "gbt28181_device_channel",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"device_id_channel_id"})},
        indexes = {@Index(columnList = "device_id, channel_id")})
public class DeviceChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id",nullable = false)
    private String deviceId;


    @Column(name = "channel_id",nullable = false)
    private String channelId;

    /**
     * 通道名
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
     * 设备归属
     */
    private String owner;

    /**
     * 行政区域
     */
    @Column(name = "civil_code")
    private String civilCode;

    /**
     * 警区
     */
    private String block;

    /**
     * 安装地址
     */
    private String address;

    /**
     * 是否有子设备 1有, 0没有
     */
    private Integer parental;

    /**
     * 父级id
     */
    @Column(name = "parent_id")
    private String parentId;

    /**
     * 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式
     */
    @Column(name = "safety_way")
    private Integer safetyWay;

    /**
     * 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式
     */
    @Column(name = "register_way")
    private Integer registerWay;

    /**
     * 证书序列号
     */
    @Column(name = "cert_num")
    private String certNum;

    /**
     * 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效
     */
    private Integer certifiable;

    /**
     * 证书无效原因码
     */
    @Column(name = "err_code")
    private Integer errCode;

    /**
     * 证书终止有效期
     */
    @Column(name = "end_time")
    private String endTime;

    /**
     * 保密属性 缺省为0; 0:不涉密, 1:涉密
     */
    private String secrecy;

    /**
     * ip地址
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 密码
     */
    private String password;

    /**
     * 云台类型
     */
    @Column(name = "ptz_type")
    private Integer PTZType;

    /**
     * 云台类型描述字符串
     */
    @Column(name = "ptz_type_text")
    private String PTZTypeText;

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
     * 在线
     */
    private Integer online;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     *  是否含有音频
     */
    @Column(name = "has_audio")
    private int hasAudio;

    /**
     *  是否开启云端录像
     */
    @Column(name = "cloud_record")
    private Integer cloudRecord;

    /**
     *  正在实时播放中的ssrc
     */
    @Transient
    private String playSsrc;

    /**
     *  正在实时播放中的callId
     */
    @Transient
    private String playCallId;
}
