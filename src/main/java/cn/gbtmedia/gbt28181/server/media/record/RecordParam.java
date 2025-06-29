package cn.gbtmedia.gbt28181.server.media.record;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class RecordParam {

    private String ssrc;

    private String pullUrl;

    private String recordPath;

    private long recordSecond;

    private boolean recordSlice;

}
