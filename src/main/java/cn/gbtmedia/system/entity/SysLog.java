package cn.gbtmedia.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
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
@Table(name = "system_log",indexes = {@Index(columnList = "create_time")})
public class SysLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 请求ip
     */
    private String ip;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求地址
     */
    private String uri;

    /**
     * 请求参数
     */
    @Column(columnDefinition="text")
    private String params;

    /**
     * 处理结果
     */
    @Column(columnDefinition="text")
    private String result;

    /**
     * 是否成功
     */
    private String success;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition="text")
    private String errorMessage;

    /**
     * 耗时
     */
    private long costTime;

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
