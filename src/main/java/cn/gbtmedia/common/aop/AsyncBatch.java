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
public @interface AsyncBatch {

    int maxThread() default 2;

    int maxElements() default 1000;

    int maxWaitTime() default 1000;

}
