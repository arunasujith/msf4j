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

package org.wso2.msf4j.circuitbreaker;

import org.wso2.msf4j.Interceptor;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.ServiceMethodInfo;

import java.lang.reflect.Method;

/**
 * Circuit Breaker Interceptor is to detect failures of a particular resource and prevent applications from executing
 * the particular resource until the server is recovered. If a Particular resource is returning 5XX status codes, which
 * the circuit breaker interceptor will monitor the number of failures in a given time-frame and refraining the requests
 * are further processed and the particular client will receive a HTTP 503 status code.
 */
public class CircuitBreakerInterceptor implements Interceptor {

    private CircuitBreakerManager circuitBreakerManager;

    public CircuitBreakerInterceptor() {
        circuitBreakerManager = new CircuitBreakerManager(5, 5000);
    }

    @Override
    public boolean preCall(Request request, Response responder, ServiceMethodInfo serviceMethodInfo) throws Exception {
        //check if the circuit is open, and if so stop the request from processing further.
        Method method = serviceMethodInfo.getMethod();
        if (circuitBreakerManager.isCircuitOpen(method)) {
            responder.setStatus(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            responder.setEntity("Service Unavailable");
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
            circuitBreakerManager.addResourceFailure(method);
        } else {
            //If the circuit is in HalfOpen state, close the circuit.
            if (circuitBreakerManager.isCircuitHalfOpen(method)) {
                circuitBreakerManager.resetCircuit(method);
            }
        }
    }
}
