package cn.gbtmedia.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xqs
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppException extends RuntimeException{

    private Integer code;

    private String message;

    public AppException(String message) {
        super(message);
        this.code = 400;
        this.message = message;
    }

    public AppException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public AppException(int code, String message, Throwable tr) {
        super(message,tr);
        this.code = code;
        this.message = message;
    }

    public AppException(String message, Throwable tr) {
        super(message,tr);
        this.code = 500;
        this.message = message;
    }

    public AppException(Throwable tr) {
        super(tr);
        this.code = 500;
        this.message = tr.getMessage();
    }

}
