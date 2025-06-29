package cn.gbtmedia.jtt808.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class InfoDto {

    private Jtt808 jtt808;

    @Data
    public static class Jtt808{
        private String accessIp;   // 接入ip
        private String cmdPort;    // 接入端口
    }
}
