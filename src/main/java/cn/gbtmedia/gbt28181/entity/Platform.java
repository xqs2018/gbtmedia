package cn.gbtmedia.gbt28181.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.List;

/**
 * @author xqs
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gbt28181_platform",uniqueConstraints={@UniqueConstraint(columnNames={"platform_id"})})
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 上级平台国标id
     */
    @Column(name = "platform_id",nullable = false)
    private String platformId;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "sip_domain",nullable = false)
    private String sipDomain;

    /**
     * 信令ip
     */
    @Column(name = "sip_ip",nullable = false)
    private String  sipIp;

    /**
     * 信令port
     */
    @Column(name = "sip_port",nullable = false)
    private Integer sipPort;

    /**
     * 信令传输协议
     * UDP/TCP
     */
    @Column(name = "sip_transport",nullable = false)
    private String sipTransport;


    private String password;

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
     * 编码
     */
    @Column(name = "charset",nullable = false)
    private String charset;

    /**
     * 注册周期 (秒)
     */
    @Column(name = "expires",nullable = false)
    private Integer expires;

    /**
     * 心跳周期(秒)
     */
    @Column(name = "keep_timeout",nullable = false)
    private Integer keepTimeout;

    /**
     * 心跳次数
     */
    @Column(name = "keep_count",nullable = false)
    private Integer keepCount;

    /**
     * 在线
     */
    private Integer online;

    /**
     * 是否启用
     */
    private Integer enable;

    /**
     * 订阅信息
     */
    @Column(name = "subscribe_catalog_info")
    private String subscribeCatalogInfo;

    @Transient
    private List<PlatformChannel> platformChannelList;
}
