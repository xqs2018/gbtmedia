package cn.gbtmedia.gbt28181.dto;

import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * @author xqs
 */
@Data
public class CatalogDto {

    private String deviceId;

    private String sn;

    private Integer sumNum;

    private Integer nowNum;

    private Integer saveNum;

    @JsonIgnore
    private List<DeviceChannel> deviceChannelList;

}
