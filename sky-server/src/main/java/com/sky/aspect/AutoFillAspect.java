package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
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
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static com.sky.constant.AutoFillConstant.*;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段自动填充...");

        // 获取方法上的autofill注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        // 获取注解value
        OperationType operationType = autoFill.value();
        
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || args == null){
            return;
        }
    
        // 获取参数
        Object arg = args[0];



        if (operationType == OperationType.INSERT){
            try {
                // 反射获取方法
                Method setCreateTime = arg.getClass().getDeclaredMethod(SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(SET_CREATE_USER, Long.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(arg, LocalDateTime.now());
                setCreateUser.invoke(arg, BaseContext.getCurrentId());
                setUpdateTime.invoke(arg, LocalDateTime.now());
                setUpdateUser.invoke(arg, BaseContext.getCurrentId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = arg.getClass().getDeclaredMethod(SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(arg, LocalDateTime.now());
                setUpdateUser.invoke(arg, BaseContext.getCurrentId());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            
        }
    }
}
