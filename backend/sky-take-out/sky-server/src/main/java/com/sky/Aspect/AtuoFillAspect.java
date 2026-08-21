package com.sky.Aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

/**
 * 自定义切面类，来实现公共字段的自动填充逻辑
 */
@Aspect
@Component
@Slf4j
public class AtuoFillAspect {

    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知，用于在方法执行前进行自动填充
     */
    @Before("autoFillPointCut()")
    public void autoFillBefore(JoinPoint joinPoint) {
        log.info("自动填充前置通知");

        //获取当前被拦截方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();// 获取方法签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);// 获取方法上的AutoFill注解
        OperationType operationType = autoFill.value();// 获取注解中的数据库操作类型

        //获取被拦截方法的传入参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.info("自动填充前置通知：没有获取到方法的参数");
            return;
        }
        Object entity = args[0];//参数默认为第一位

        //准备赋值的数据
        Long currentId = BaseContext.getCurrentId();// 获取当前登录员工ID
        LocalDateTime now = LocalDateTime.now();// 获取当前时间

        //根据不同的数据库操作类型，进行不同的字段赋值
        switch (operationType) {
            case INSERT:
                //通过反射为对象属性赋值
                try {
                    entity.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class).invoke(entity, currentId);
                    entity.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                } catch (Exception e) {
                    log.error("自动填充前置通知：反射赋值失败", e);
                    e.printStackTrace();// 打印堆栈信息
                }
                break;
            case UPDATE:
                try {
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
                    entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                } catch (Exception e) {
                    log.error("自动填充前置通知：反射赋值失败", e);
                    e.printStackTrace();// 打印堆栈信息
                }
                break;
            default:
                log.info("自动填充前置通知：没有进行任何操作");
                break;
        }

    }
}
