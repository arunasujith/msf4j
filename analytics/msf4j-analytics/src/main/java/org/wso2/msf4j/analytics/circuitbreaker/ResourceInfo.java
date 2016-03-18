/*
 * Copyright 2016 WSO2, Inc. http://www.wso2.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.msf4j.analytics.circuitbreaker;

import java.lang.reflect.Method;

/**
 * ResourceInfo class holds the circuit break resource specific details.
 */
public class ResourceInfo {
    
    private Method method;
    private String name;
    private CircuitBreaker.RateType rateType;
    private long threshold;
    private long timeout;

    public ResourceInfo(Method method , CircuitBreaker circuitBreaker) {
        this.method = method;
        this.name = circuitBreaker.name();
        this.rateType = circuitBreaker.ratetype();
        this.threshold = circuitBreaker.threshold();
        this.timeout = circuitBreaker.timeout();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public CircuitBreaker.RateType getRateType() {
        return rateType;
    }

    public void setRateType(CircuitBreaker.RateType rateType) {
        this.rateType = rateType;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }
}
