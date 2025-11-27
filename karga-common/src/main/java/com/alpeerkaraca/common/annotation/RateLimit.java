package com.alpeerkaraca.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;

    int duration() default 60;

    String key() default ""; // How to specify limit (login, register?)

    TimeUnit unit() default TimeUnit.SECONDS;
}
