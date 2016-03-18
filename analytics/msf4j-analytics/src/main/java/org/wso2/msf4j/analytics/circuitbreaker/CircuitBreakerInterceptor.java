/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.msf4j.Interceptor;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.ServiceMethodInfo;
import org.wso2.msf4j.analytics.metrics.MetricReporter;
import org.wso2.msf4j.analytics.metrics.Metrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker Interceptor is to detect failures of a particular resource and prevent applications from executing
 * the particular resource until the server is recovered. If a Particular resource is returning 5XX status codes, which
 * the circuit breaker interceptor will monitor the number of failures in a given time-frame and refraining the requests
 * are further processed and the particular client will receive a HTTP 503 status code.
 */
public class CircuitBreakerInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerInterceptor.class);
    private CircuitBreakerManager circuitBreakerManager;
    private Map<Method, ResourceInfo> methodResourceMap = new ConcurrentHashMap<>();

    public CircuitBreakerInterceptor() {
        circuitBreakerManager = new CircuitBreakerManager();
    }

    public CircuitBreakerInterceptor init(MetricReporter... metricReporters) {
        org.wso2.msf4j.analytics.metrics.Metrics.init(metricReporters);
        // Destroy the Metric Service at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Metrics.destroy();
            }
        });
        return this;
    }

    @Override
    public boolean preCall(Request request, Response responder, ServiceMethodInfo serviceMethodInfo) throws Exception {
        Method method = serviceMethodInfo.getMethod();
        //check annotation is processed per method if not process annotations.
        if (methodResourceMap.get(method) == null) {
            processAnnotations(method);
        }
        //check if the circuit is open, and if so stop the request from processing further.
        if (circuitBreakerManager.isCircuitOpen(method)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Circuit Breaker is in open state for resource " + method.getDeclaringClass().getName() +
                             "#" + method.getName());
            }
            responder.setStatus(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            responder.setEntity("503 Service Unavailable");
            responder.send();
            return false;
        }
        return true;
    }

    @Override
    public void postCall(Request request, int status, ServiceMethodInfo serviceMethodInfo) throws Exception {
        Method method = serviceMethodInfo.getMethod();
        //check for failures with status code 5XX.
        if (status >= 500 && status < 600) {
            if (logger.isDebugEnabled()) {
                logger.debug("Circuit Breaker marked the resource failure " + method.getDeclaringClass().getName() +
                             "#" + method.getName());
            }
            circuitBreakerManager.markFailure(methodResourceMap.get(method));
        } else {
            //If the circuit is in HalfOpen state, close the circuit.
            if (circuitBreakerManager.isCircuitHalfOpen(method)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Circuit Breaker moved from Half-Open to Closed state " +
                                 method.getDeclaringClass().getName() + "#" + method.getName());
                }
                circuitBreakerManager.resetCircuit(method);
            }
        }
    }

    /**
     * Process the annotations only once when the first request is made. Priority is given for the method level circuit
     * breaker annotation.
     *
     * @param method method value
     */
    private void processAnnotations(Method method) {
        //check the method level annotation is present else add the class level annotations
        if (method.isAnnotationPresent(CircuitBreaker.class)) {
            CircuitBreaker annotation = method.getAnnotation(CircuitBreaker.class);
            ResourceInfo resourceInfo = new ResourceInfo(method, annotation);
            methodResourceMap.put(method, resourceInfo);
        } else if (method.getDeclaringClass().isAnnotationPresent(CircuitBreaker.class)) {
            CircuitBreaker annotation = method.getDeclaringClass().getAnnotation(CircuitBreaker.class);
            ResourceInfo resourceInfo = new ResourceInfo(method, annotation);
            methodResourceMap.put(method, resourceInfo);
        }
    }
}
