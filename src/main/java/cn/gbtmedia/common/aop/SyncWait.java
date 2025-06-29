package cn.gbtmedia.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xqs
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyncWait {

    String key();

    int maxWaitTime() default 10000 * 9;

    int maxWaitThread() default 9;

}
