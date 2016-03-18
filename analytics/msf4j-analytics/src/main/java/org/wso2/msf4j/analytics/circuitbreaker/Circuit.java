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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.metrics.manager.Level;
import org.wso2.carbon.metrics.manager.Meter;
import org.wso2.carbon.metrics.manager.MetricManager;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Circuit Breaker class which trips the switch and move to open state, when the threshold value is exceeded. After the
 * timeout value reached the state will be moved to halfopen state, If the very next request is a success then the
 * circuit state move to a closed state, and will move to open state if otherwise.
 */
public class Circuit {

    private static final Logger logger = LoggerFactory.getLogger(Circuit.class);
    private Meter meter;
    private ResourceInfo resourceInfo;
    private Timer timeoutTimer;
    private CircuitStatus status = CircuitStatus.CLOSED;

    public Circuit(ResourceInfo resourceInfo) {
        this.timeoutTimer = new Timer();
        this.resourceInfo = resourceInfo;
        String meterName = buildName(resourceInfo.getName(), resourceInfo.getMethod());
        this.meter = MetricManager.meter(meterName, Level.INFO);
    }

    /**
     * After an error triggered update the meter values, and the state of the circuit according to the circuit threshold
     * and timeout values. If the circuit is in Closed and the threshold limit reached, move the circuit to Open State
     * and start the timer. If the circuit is in half-open state, move the circuit to open state and start the timer.
     */
    public synchronized void update() {
        if (logger.isDebugEnabled()) {
            logger.debug("Updating failure event " + this.resourceInfo.getMethod().getDeclaringClass().getName() +
                         "#" + this.resourceInfo.getMethod().getName());
        }
        //mark the failure
        meter.mark();
        if (isClosed() && isLimitExceeded()) {
            this.status = CircuitStatus.OPEN;
            startTimeoutTask();
        } else if (isHalfOpen()) {  // if the status is halfopen, then move the circuit to open state and start timer.
            this.status = CircuitStatus.OPEN;
            startTimeoutTask();
        }
    }

    /**
     * Check the circuit is in Closed state.
     *
     * @return {@code true} if the state is closed.
     */
    public boolean isClosed() {
        return CircuitStatus.CLOSED.equals(status);
    }

    /**
     * Check the circuit is in Open state.
     *
     * @return {@code true} if the state is open.
     */
    public boolean isOpen() {
        return CircuitStatus.OPEN.equals(status);
    }

    /**
     * Check the circuit is in HalfOpen state.
     *
     * @return {@code true} if the state is half-open.
     */
    public boolean isHalfOpen() {
        return CircuitStatus.HALF_OPEN.equals(status);
    }

    /**
     * Reset the circuit and set the circuit state to {@code CircuitStatus.CLOSED} state.
     */
    public synchronized void reset() {
        status = CircuitStatus.CLOSED;
    }

    /**
     * Starts a timer task and change the circuit status to {@code CircuitStatus.HALF_OPEN} after the give timeout
     * value.
     */
    private void startTimeoutTask() {
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                status = CircuitStatus.HALF_OPEN;
            }
        }, resourceInfo.getTimeout());
    }

    /**
     * A helper method to create a meter name according to the given name.
     *
     * @param explicitName user given name for the meter.
     * @param method       resource method.
     * @return name of the meter.
     */
    private String buildName(String explicitName, Method method) {
        if (explicitName != null && !explicitName.isEmpty()) {
            return MetricManager.name(method.getDeclaringClass().getName(), method.getName(), explicitName);
        }
        return MetricManager.name(method.getDeclaringClass().getName(), method.getName());
    }

    /**
     * Check the error rate has exceeded the threshold value.
     *
     * @return {@code true} if the error rate has exceeded the threshold.
     */
    private boolean isLimitExceeded() {
        switch (this.resourceInfo.getRateType()) {
            case MEAN: {
                return meter.getMeanRate() >= this.resourceInfo.getThreshold();
            }
            case ONE_MINUTE: {
                return meter.getOneMinuteRate() >= this.resourceInfo.getThreshold();
            }
            case FIVE_MINUTES:
                return meter.getFiveMinuteRate() >= this.resourceInfo.getThreshold();
            case FIFTEEN_MINUTES: {
                return meter.getFifteenMinuteRate() >= this.resourceInfo.getThreshold();
            }
            default:
                return false;
        }
    }
}

