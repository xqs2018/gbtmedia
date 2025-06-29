package cn.gbtmedia.gbt28181.server.media.stream.muxer;

import lombok.Data;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Data
public class H264Decoder {

    private byte[] sps;

    private byte[] pps;

    private Consumer<byte[]> nlauConsumer;

    private void addH264(byte[] h264){

    }

}
