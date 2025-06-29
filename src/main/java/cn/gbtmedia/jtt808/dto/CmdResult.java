package cn.gbtmedia.jtt808.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class CmdResult<T> {

    private boolean success;

    private T data;

    private String message;

    public static <T> CmdResult<T> success() {
        CmdResult<T> result = new CmdResult<>();
        result.setSuccess(true);
        return result;
    }

    public static <T> CmdResult<T> success(T data) {
        CmdResult<T> result = new CmdResult<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static <T> CmdResult<T> error(String message) {
        CmdResult<T> result = new CmdResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
