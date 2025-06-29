package cn.gbtmedia.system.servcie;

import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.gbtmedia.system.dto.QueryParam;
import cn.gbtmedia.system.entity.SysLog;
import cn.gbtmedia.system.repository.SystemLogRepository;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author xqs
 */
@Aspect
@Service
public class SysLogService {

    @Resource
    private SystemLogRepository logRepository;

    public Page<SysLog> page(QueryParam param) {
        Specification<SysLog> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getStartTime()) && ObjectUtil.isNotEmpty(param.getEndTime())){
                predicates.add(criteriaBuilder.between(root.get("createTime"), param.getStartTime(), param.getEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return logRepository.findAll(specification, page);
    }

    @Aspect
    @Component
    private static class RequestLogAspect {

        @Resource
        private SystemLogRepository logRepository;

        @Pointcut("execution(* cn.gbtmedia.*.controller..*.*(..))")
        public void requestLog() {
        }

        @Around("requestLog()")
        public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return joinPoint.proceed();
            }
            HttpServletRequest request = attributes.getRequest();
            String uri = request.getRequestURI();
            if("/backend/system/log/page".equals(uri)){
                return joinPoint.proceed();
            }
            long startTime = System.currentTimeMillis();
            Object result = null;
            Exception exception = null;
            try {
                result = joinPoint.proceed();
                return result;
            } catch (Exception ex) {
                exception = ex;
                throw ex;
            } finally {
                long costTime = System.currentTimeMillis() - startTime;
                String ip = getClientIp(request);
                String method = request.getMethod();
                Object[] args = joinPoint.getArgs();
                SysLog content = new SysLog();
                content.setIp(ip);
                content.setMethod(method);
                content.setUri(uri);
                content.setParams(toJsonStr(Arrays.stream(args).map(this::toJsonStr).toList()));
                //content.setResult(result == null ? "" : toJsonStr(result));
                content.setSuccess(exception == null ? "1" : "0");
                content.setErrorMessage(exception == null ? "" : exception.getMessage());
                content.setCostTime(costTime);
                logRepository.save(content);
            }
        }

        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }

        private String toJsonStr(Object object) {
            try {
                return JSONUtil.toJsonStr(object);
            } catch (Exception ignored) {
            }
            return "";
        }
    }
}
