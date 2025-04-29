package com.example.demo.aspect;

import com.example.demo.config.DataSourceContextHolder;
import com.example.demo.config.DataSourceType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据源切面，用于根据方法类型自动切换数据源
 *
 * 1、统一使用 @Around 切面：将数据源设置和清除逻辑封装在同一个切面中，避免多次调用可能带来的问题。
 * 2、增强日志记录：在切换数据源时记录相关信息，便于排查问题。
 * 3、动态判断方法类型：使用 isReadOperation 方法动态判断方法是否为读操作，支持更灵活的扩展。
 * 4、线程安全保证：假设 DataSourceContextHolder 已经使用 ThreadLocal 实现线程隔离。
 */
@Aspect
@Component
public class DataSourceAspect {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);
    /**
     * 事务操作的切点
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalOperation() {}

    @Pointcut("execution(* com.example.demo.service..*.*(..))")
    public void allServiceMethods() {}

    @Around("allServiceMethods()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        boolean isTransactional = joinPoint.getSignature().getDeclaringType()
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class);

        try {
            if (isTransactional) {
                // 事务方法优先使用主库
                DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
                logger.debug("Switching to MASTER for transactional method: {}", methodName);
            } else if (isReadOperation(methodName)) {
                // 非事务的读操作使用从库
                DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
                logger.debug("Switching to SLAVE for read operation: {}", methodName);
            } else {
                // 默认使用主库
                DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
                logger.debug("Switching to MASTER for write operation: {}", methodName);
            }

            return joinPoint.proceed();
        } finally {
            // 清除数据源类型
            DataSourceContextHolder.clearDataSourceType();
            logger.debug("Cleared data source type after method: {}", methodName);
        }
    }

    private boolean isReadOperation(String methodName) {
        return methodName.startsWith("select") ||
                methodName.startsWith("get") ||
                methodName.startsWith("find") ||
                methodName.startsWith("query") ||
                methodName.startsWith("list") ||
                methodName.startsWith("count");
    }
}
