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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * @author xqs
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gbt28181_platform_channel",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"platform_id", "channel_id"})},
        indexes = {@Index(columnList = "platform_id, device_id, channel_id")})
public class PlatformChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 上级平台国标id
     */
    @Column(name = "platform_id",nullable = false)
    private String platformId;

    /**
     * 设备id
     */
    @Column(name = "device_id",nullable = false)
    private String deviceId;

    /**
     * 通道id
     */
    @Column(name = "channel_id",nullable = false)
    private String channelId;

    @Transient
    private DeviceChannel deviceChannel;

}
