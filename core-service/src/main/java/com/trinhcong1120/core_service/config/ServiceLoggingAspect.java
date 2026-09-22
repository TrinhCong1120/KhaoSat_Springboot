package com.trinhcong1120.core_service.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class ServiceLoggingAspect implements BeanPostProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(ServiceLoggingAspect.class);

    private static final String SERVICE_PACKAGE =
            "com.trinhcong1120.core_service.service";

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) throws BeansException {

        Class<?> beanClass = bean.getClass();

        if (!beanClass.getName().startsWith(SERVICE_PACKAGE)) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(
                (MethodInterceptor) invocation -> {

                    String serviceName =
                            invocation.getMethod()
                                    .getDeclaringClass()
                                    .getSimpleName();

                    String methodName =
                            invocation.getMethod()
                                    .getName();

                    StopWatch stopWatch = new StopWatch();

                    log.info(
                            "Starting service activity: {}.{}",
                            serviceName,
                            methodName
                    );

                    try {
                        stopWatch.start();

                        Object result =
                                invocation.proceed();

                        stopWatch.stop();

                        log.info(
                                "Completed service activity: {}.{} in {} ms",
                                serviceName,
                                methodName,
                                stopWatch.getTotalTimeMillis()
                        );

                        return result;

                    } catch (Throwable ex) {

                        if (stopWatch.isRunning()) {
                            stopWatch.stop();
                        }

                        log.error(
                                "Service activity failed: {}.{} after {} ms - {}",
                                serviceName,
                                methodName,
                                stopWatch.getTotalTimeMillis(),
                                ex.getMessage(),
                                ex
                        );

                        throw ex;
                    }
                }
        );

        return proxyFactory.getProxy();
    }
}
