package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动填充注解，用来标识哪些字段需要自动填充
 */
//指定注解只能使用在方法上，表示该方法上的参数需要自动填充
@Target(ElementType.METHOD)
//指定注解的保留策略为运行时，表示该注解在运行时仍然有效
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //指定注解的属性：UPDATE,INSERT
    OperationType value();
}
