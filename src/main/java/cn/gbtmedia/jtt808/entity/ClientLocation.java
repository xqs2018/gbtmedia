package cn.gbtmedia.jtt808.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.Date;


/**
 * @author xqs
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jtt808_client_location",indexes = {@Index(columnList = "client_id,create_time"),
        @Index(columnList = "platform_alarm_id")})
public class ClientLocation {

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
     * 车辆标识
     */
    @Column(name = "plate_no")
    private String plateNo;

    /**
     * 报警标志
     */
    @Column(name = "warn_bit")
    private Integer warnBit;

    /**
     * 报警标志详情
     */
    @Column(name = "warn_bit_name")
    private String warnBitName;

    /**
     * 状态标志
     */
    @Column(name = "status_bit")
    private Integer statusBit;

    /**
     * 状态标志详情
     */
    @Column(name = "status_bit_name")
    private String statusBitName;

    /**
     * 纬度
     */
    @Column(name = "latitude")
    private Integer latitude;

    /**
     * 经度
     */
    @Column(name = "longitude")
    private Integer longitude;

    /**
     * 高程
     */
    @Column(name = "altitude")
    private Integer altitude;

    /**
     * 速度
     */
    @Column(name = "speed")
    private Integer speed;

    /**
     * 方向
     */
    @Column(name = "direction")
    private Integer direction;

    /**
     * 累计行驶里程
     */
    @Column(name = "total_mileage")
    private Long totalMileage;

    /**
     * 油量
     */
    @Column(name = "oil")
    private Integer oil;

    /**
     * 时间
     */
    @Column(name = "device_time")
    private String deviceTime;

    /**
     * 平台报警id
     */
    @Column(name = "platform_alarm_id")
    private String platformAlarmId;

    /**
     * 报警名称
     */
    @Column(name = "alarm_name")
    private String alarmName;

    /**
     * 报警文件名称
     */
    @Column(name = "alarm_file_name")
    private String alarmFileName;

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
     * 状态 0 离线 1 在线 2 报警
     */
    @Transient
    private Integer clientStatus;

}
