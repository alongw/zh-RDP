package com.microsoft.a3rdc.logging;

import java.lang.reflect.Method;

import org.aspectj.lang.reflect.MethodSignature;

import android.util.Log;

import com.squareup.otto.Bus;

public aspect LoggingAspect {

    /**
     * Log Annotation
     * 
     * Declares a pointcut to capture all methods that are annotated with @LogDebug.
     * Logs method enter and exit, and optionally the parameters.
     * 
     * Put {@link LogDebug} annotation on method calls that you would like to get logged
     * in developer builds.
     */

    pointcut logDebugAnnotation() : execution(@LogDebug * *(..));

    Object around() : logDebugAnnotation() {
        final String tag = thisJoinPoint.getTarget().getClass().getSimpleName();
        final MethodSignature signature = (MethodSignature) thisJoinPoint.getSignature();
        final Method method = signature.getMethod();
        final String methodName = method.getName();
        final LogDebug annotation = method.getAnnotation(LogDebug.class);

        Log.d(tag, "Enter " + methodName);

        if (annotation.args()) {
            for (int i = 0; i < thisJoinPoint.getArgs().length; ++i) {
                Log.d(tag, String.format("    arg%d: %s", i, thisJoinPoint.getArgs()[i]));
            }
        }

        final Object result = proceed();

        Log.d(tag, "Exit " + methodName);

        if (annotation.args() && !method.getReturnType().equals(void.class)) {
            Log.d(tag, "    result: " + result);
        }

        return result;
    }

    /**
     * Log all events on the event bus.
     */

    pointcut events() : execution(void Bus.post(Object));

    before() : events() {
        Log.d("Event", "post " + thisJoinPoint.getArgs()[0].getClass().getSimpleName());
    }
}
