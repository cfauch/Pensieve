/*
 * Copyright 2019 Claire Fauch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.code.fauch.pensieve;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Flow controller driver which schedule the transfer of one packet with a fixed
 * delay between the end of the first transfer and the beginning of the second one.
 * 
 * @author c.fauch
 *
 */
public final class ScheduleWithFixedDelay extends AbsDriver {

    /**
     * Executor service.
     */
    private final ScheduledExecutorService executor;
    
    /**
     * The delay between the end of one transfer and the start of the second one.
     */
    private final long delay;
    
    /**
     * The time unit of the delay.
     */
    private final TimeUnit unit;
    
    /**
     * Constructor.
     * 
     * @param delay The delay between the end of one transfer and the start of the second one.
     * @param unit The time unit of the given delay
     */
    ScheduleWithFixedDelay(final long delay, final TimeUnit unit) {
        this.executor = Executors.newScheduledThreadPool(1);
        this.delay = delay;
        this.unit = unit;
    }
    
    /**
     * Schedule a task to ask to the flow controller to process a packet with a fixed delay between the end
     * of the first execution and the start of the second one.
     */
    @Override
    void start(final FlowController<?> ctrl) {
        this.executor.scheduleWithFixedDelay(() -> ctrl.process(), 0, this.delay, this.unit);
    }

    /**
     * Cancel the task and ask to the flow controller to drain and process all remaining packets.
     */
    @Override
    void stop(final FlowController<?> ctrl) {
        try {
            this.executor.shutdownNow();
            ctrl.drain();
            this.executor.awaitTermination(10,TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
