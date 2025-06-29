package cn.gbtmedia.common.exception;

import cn.gbtmedia.common.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @author xqs
 */
@Slf4j
@Configuration
@RestControllerAdvice
public class ExceptionAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = AppException.class)
    public Result<?> baseException(AppException e){
        log.error("【全局异常拦截】AppException : 状态码 {}, 错误信息 {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = NoResourceFoundException.class)
    public Result<?> noResourceFoundException(NoResourceFoundException e){
        log.error("【全局异常拦截】NoResourceFoundException :  错误信息 {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestError(AsyncRequestNotUsableException e) {
        log.debug("【全局异常拦截】AsyncRequestNotUsableException :  错误信息 {}", e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public Result<?> exceptionHandler(Exception e){
        log.error("【全局异常拦截】{} :  错误信息 {}", e.getClass().getSimpleName(),e.getMessage(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),"服务器内部错误");
    }
}
