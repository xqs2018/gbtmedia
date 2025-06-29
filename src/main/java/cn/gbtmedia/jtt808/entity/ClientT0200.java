package cn.gbtmedia.jtt808.entity;

import cn.gbtmedia.jtt808.server.cmd.message.T0200;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

/**
 * 位置信息上报
 * @author xqs
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jtt808_client_t0200",indexes = {@Index(columnList = "client_id,create_time")})
public class ClientT0200 {

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
     * T0200位置信息上报
     */
    @Column(name = "t0200_json",length = 2000)
    @JsonIgnore
    private String t0200json;

    /**
     * T0200位置信息上报对象
     */
    private transient T0200 t0200;
}
