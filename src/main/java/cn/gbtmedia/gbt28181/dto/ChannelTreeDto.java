package cn.gbtmedia.gbt28181.dto;

import lombok.Data;
import java.util.List;

/**
 * @author xqs
 */
@Data
public class ChannelTreeDto {

    private String gbId;

    private String customName;

    private Integer online;

    private List<ChannelTreeDto> children;
}
