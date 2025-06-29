package cn.gbtmedia.common.exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.io.IOException;

/**
 * @author xqs
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE+10)
@Configuration
public class ExceptionFilter extends OncePerRequestFilter {

    @Lazy
    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @SuppressWarnings("all")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request,response);
        }catch (Exception ex){
            if(response.isCommitted()){
                log.error("【全局异常Filter拦截（响应已经提交将不做任何处理）】URI {} {}: 错误信息 {}",
                        request.getRequestURI(),
                        ex.getClass().getSimpleName(),ex.getMessage(),ex);
                return;
            }
            log.error("【全局异常Filter拦截】URI {} {}: 错误信息 {}",request.getRequestURI(),
                    ex.getClass().getSimpleName(),ex.getMessage(),ex);
           resolver.resolveException(request,response,null,ex);
        }
    }
}
