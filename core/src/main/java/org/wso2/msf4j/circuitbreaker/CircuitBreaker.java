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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker class which trips the switch and move to open state, when the threshold value is exceeded. After the
 * timeout value reached the state will be moved to halfopen state, If the very next request is a success then the
 * circuit state move to a closed state, and will move to open state if otherwise.
 */
public class CircuitBreaker {

    private AtomicLong failureCounter;     //Failure counter list to hold the list of failures with their timestamp.
    private long timeout;                  //Timeout value in milliseconds
    private long threshold;                //Max number of failures
    private Timer timeoutTimer;
    private CircuitStatus status = CircuitStatus.CLOSED;

    /**
     * @param threshold max number of failures
     * @param timeout   timeout values in milliseconds
     */
    public CircuitBreaker(long threshold, long timeout) {
        failureCounter = new AtomicLong(0);
        timeoutTimer = new Timer();
        this.timeout = timeout;
        this.threshold = threshold;
    }


    public synchronized void update() {
        //if the status is closed and threshold exceed set the circuit open and start the timeout timer.
        if (isClosed() && (failureCounter.get() >= threshold)) {
            this.status = CircuitStatus.OPEN;
            startTimeoutTask();
        } else if (isHalfOpen()) {  // if the status is halfopen, then move the circuit to open state and start the time out timer.
            this.status = CircuitStatus.OPEN;
            startTimeoutTask();
        }
    }

    public boolean isClosed() {
        return CircuitStatus.CLOSED.equals(status);
    }

    public boolean isOpen() {
        return CircuitStatus.OPEN.equals(status);
    }

    public boolean isHalfOpen() {
        return CircuitStatus.HALF_OPEN.equals(status);
    }

    public synchronized void reset() {
        failureCounter.set(0);
        status = CircuitStatus.CLOSED;
    }

    private void startTimeoutTask() {
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                status = CircuitStatus.HALF_OPEN;
            }
        }, timeout);
    }
}

