package cn.gbtmedia.common.util;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 * @author xqs
 */
@Component
public class SpringUtil extends cn.hutool.extra.spring.SpringUtil {

    /**
     * 解析返回字符串
     */
    public static String spelParse(String str, Method method, Object[] args){
        return spelParse(str,method,args,String.class);
    }

    /**
     * 解析注解上的spel的表达式
     * @param str 原始字符串
     * @param method 注解方法
     * @param args 参数
     * @param returnType 返回类型
     * @return /
     */
    public static <T> T spelParse(String str, Method method, Object[] args, Class<T> returnType){
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        //1.@对象 解析使用spring容器
        context.setBeanResolver(new BeanFactoryResolver(SpringUtil.getApplicationContext()));
        //2.#参数 解析使用方法参数填充
        // https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-6.x#parameter-name-retention
        StandardReflectionParameterNameDiscoverer u = new StandardReflectionParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);
        if(!ObjectUtils.isEmpty(paraNameArr)){
            for(int i=0;i<paraNameArr.length;i++){
                context.setVariable(paraNameArr[i], args[i]);
            }
        }
        return parser.parseExpression(str).getValue(context,returnType);
    }
}
