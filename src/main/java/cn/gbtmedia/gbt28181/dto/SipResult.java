package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class SipResult<T> {

    private boolean success;

    private T data;

    private String message;

    public static <T> SipResult<T> success() {
        SipResult<T> result = new SipResult<>();
        result.setSuccess(true);
        return result;
    }

    public static <T> SipResult<T> success(T data) {
        SipResult<T> result = new SipResult<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static <T> SipResult<T> error(String message) {
        SipResult<T> result = new SipResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
