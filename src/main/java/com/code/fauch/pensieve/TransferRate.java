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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Flow controller driver which transfer the packets according a transfer rate given in bits/s
 * 
 * @author c.fauch
 *
 */
public final class TransferRate extends AbsDriver {

    /**
     * Executor service.
     */
    private final ExecutorService executor;
    
    /**
     * The transfer rate in bits/s.
     */
    private final int rate;

    /**
     * Constructor.
     * 
     * @param rate the transfer rate in bits/s
     */
    public TransferRate(final int rate) {
        this.executor = Executors.newSingleThreadExecutor();
        this.rate = rate;
    }
    
    /**
     * Start a task to process each packets and sleep is needed to ensure the given transfer rate.
     */
    @Override
    void start(FlowController<?> ctrl) {
        this.executor.submit(()-> loop(this.rate, ctrl));
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

    /**
     * Infinite loop that send packets waiting sometime if needed
     * to respect give transfer rate.
     * 
     * @param rate the transfer rate (bits/s)
     * @param bytesFuntion the function to apply on each packet to determine their total number of bytes.
     */
    private void loop(final int rate, final FlowController<?> ctrl) {
        try {
            long start = System.currentTimeMillis();
            int total = 0; // Number of bits
            while(!Thread.currentThread().isInterrupted()) {
                total += ctrl.process() * 8;
                final long delay = System.currentTimeMillis() - start;
                if (rate > 0) {
                    long toWait = computeWait(total, delay, rate);
                    if (toWait > 20) {
                        Thread.sleep(toWait);
                        start = System.currentTimeMillis();
                        total = 0;
                    }
                }
            }
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Compute wait for given rate according to given total bits and delay.
     * 
     * @param total the total bits sent
     * @param delay the delay
     * @param rate the expected rate
     * @return the time to wait
     */
    private long computeWait(final long total, final long delay, final int rate) {
        final double totalExpectedTime = 1000 * total/(double)rate;
        return (long) (totalExpectedTime - delay);
    }
    
}
