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

package org.wso2.msf4j.circuitbreaker;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Manager class which holds and manage resources with their circuits. A resource is represented as the
 * method, and there is a circuit per method.
 */
public class CircuitBreakerManager {

    private Map<Method, CircuitBreaker> methodCircuitMap;
    private long maxFailures;
    private long timeout;

    /**
     * Initialize the circuit manager with max failure count and the timeout values.
     *
     * @param maxFailures the number of maximum failures in a given timeout window.
     * @param timeout     the timeout value to the circuit to be stay closed in milliseconds.
     */
    public CircuitBreakerManager(long maxFailures, long timeout) {
        methodCircuitMap = new ConcurrentHashMap<>();
        this.maxFailures = maxFailures;
        this.timeout = timeout;
    }

    public synchronized void addResourceFailure(Method method) {
        CircuitBreaker circuitBreaker;
        if (methodCircuitMap.containsKey(method)) {
            circuitBreaker = methodCircuitMap.get(method);
        } else {
            circuitBreaker = new CircuitBreaker(maxFailures, timeout);
            methodCircuitMap.put(method, circuitBreaker);
        }
        circuitBreaker.update();
    }

    public synchronized boolean isCircuitOpen(Method method) {
        if (methodCircuitMap.containsKey(method)) {
            return methodCircuitMap.get(method).isOpen();
        }
        return false;
    }

    public synchronized boolean isCircuitHalfOpen(Method method) {
        if (methodCircuitMap.containsKey(method)) {
            return methodCircuitMap.get(method).isHalfOpen();
        }
        return false;
    }

    public synchronized void resetCircuit(Method method) {
        if (methodCircuitMap.containsKey(method)) {
            methodCircuitMap.get(method).reset();
        }
    }

}
