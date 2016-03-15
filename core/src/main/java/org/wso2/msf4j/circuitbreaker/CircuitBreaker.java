package org.wso2.msf4j.circuitbreaker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Monitor Circuit Breaker Status.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface CircuitBreaker {

    /**
     * TimeFrame Enum values.
     */
    public enum RateType {
        MEAN, ONE_MINUTE, FIVE_MINUTES, FIFTEEN_MINUTES
    }

    RateType ratetype() default RateType.ONE_MINUTE;

    double threshold() default 0D;
}
