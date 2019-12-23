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
 * Flow controller driver which schedule the transfer of one packet at fixed rate.
 * 
 * @author c.fauch
 *
 */
public final class ScheduleAtFixedRate extends AbsDriver {

    /**
     * Executor service.
     */
    private final ScheduledExecutorService executor;
    
    /**
     * The period between the beginning of each consecutive transfer.
     */
    private final long period;
    
    /**
     * The time unit of the period.
     */
    private final TimeUnit unit;

    /**
     * Constructor.
     * 
     * @param period The period between the beginning of each consecutive transfer.
     * @param unit the time unit of the given period
     */
    public ScheduleAtFixedRate(final long period, final TimeUnit unit) {
        this.executor = Executors.newScheduledThreadPool(1);
        this.period = period;
        this.unit = unit;
    }
    
    /**
     * Schedule a task at fixed rate to ask to the flow controller to process a packet.
     */
    @Override
    void start(final FlowController<?> ctrl) {
        this.executor.scheduleAtFixedRate(() -> ctrl.process(), 0, this.period, this.unit);
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
