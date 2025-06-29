package cn.gbtmedia.system.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xqs
 */
@Data
public class QueryParam {

    private int pageNo = 0;

    private int pageSize = 10;

    private Date startTime;

    private Date endTime;
}
