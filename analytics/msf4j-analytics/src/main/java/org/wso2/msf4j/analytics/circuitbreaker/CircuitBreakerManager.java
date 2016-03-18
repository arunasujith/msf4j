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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Manager class which holds and manage resources with their circuits. A resource is represented as the
 * method, and there is a circuit per method.
 */
public class CircuitBreakerManager {

    private Map<Method, Circuit> methodCircuitMap;

    /**
     * Initialize the circuit manager.
     */
    public CircuitBreakerManager() {
        methodCircuitMap = new ConcurrentHashMap<>();
    }

    public synchronized void markFailure(ResourceInfo resourceInfo) {
        Circuit circuit;
        if (methodCircuitMap.containsKey(resourceInfo.getMethod())) {
            circuit = methodCircuitMap.get(resourceInfo.getMethod());
        } else {
            circuit = new Circuit(resourceInfo);
            methodCircuitMap.put(resourceInfo.getMethod(), circuit);
        }
        circuit.update();
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
