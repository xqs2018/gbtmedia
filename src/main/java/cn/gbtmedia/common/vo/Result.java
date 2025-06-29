package cn.gbtmedia.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xqs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = true;

    /**
     * 数据
     */
    private T data;

    /**
     * 错误信息返回
     */
    @Builder.Default
    private int code = 200;

    /**
     * 错误信息返回
     */
    @Builder.Default
    private String message = "操作成功！";

    /**
     * traceId 当前请求追踪id
     */
    private String traceId;

    /**
     * 时间戳
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();


    /**
     * 成功返回
     *
     */
    public static Result<?> success() {
        return new Result<>();
    }

    /**
     * 成功返回
     *
     */
    public static Result<?> success(String message) {
        return Result.builder().message(message).build();
    }

    /**
     * 成功返回
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success(T data) {
        return (Result<T>) Result.builder().data(data).build();
    }


    /**
     * 成功返回
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success(T data, String message) {
        return (Result<T>) Result.builder()
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 失败返回
     */
    public static Result<?> error(String msg) {
        return error(500,msg);
    }

    /**
     * 失败返回
     */
    public static Result<?> error(Integer code,String message) {
        return Result.builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }

}
