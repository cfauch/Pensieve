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

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Flow controller.
 * <p>
 * This object is the main entry point to consumer packets by controlling flow rate.
 * </p>
 * <p>
 * It is made of:
 * <ul>
 * <li> a transfer queue: in which the packets to be transferred are stacked</li>
 * <li> a consumer: to handle the unstacking packet.</li>
 * <li> a driver: to drive packets unstacking using different modes</li>
 * </ul>
 * </p>
 * @author c.fauch
 *
 */
public final class FlowController<T> implements AutoCloseable {

    /**
     * Indicates whether this flow controller is open or not.
     */
    private boolean isOpen;
        
    /**
     * The consumer used to process each available packets.
     */
    private final Consumer<T> consumer;
    
    /**
     * The transfer queue used to manage streaming
     */
    private final TransferQueue<T> transferQueue;
    
    /**
     * The function to apply on packet to know its size in bytes.
     */
    private final ToIntFunction<T> bytesFunction;
    
    /**
     * Flow controller driver.
     * It is used to drive packets unstacking.
     */
    private final AbsDriver driver;

    /**
     * Constructor.
     * 
     * @param driver the driver to use drive packets unstacking (not null)
     * @param consumer the consumer to process each packet (not null)
     * @param bytesFunction the function to compute the size in bytes of a packet (not null)
     */
    FlowController(final AbsDriver driver, final Consumer<T> consumer, final ToIntFunction<T> bytesFunction) {
        this.consumer = Objects.requireNonNull(consumer, "consumer is missing");
        this.transferQueue = new LinkedTransferQueue<T>();
        this.isOpen = false;
        this.bytesFunction = Objects.requireNonNull(bytesFunction, "bytesFunction is missing");
        this.driver = driver;
    }
    
    /**
     * Open the flow controller if it is not already open.
     * 
     * @throws IllegalStateException if the flow controller was already open
     */
    public void open() {
        if (this.isOpen) {
            throw new IllegalStateException("Streaming already open");
        }
        this.driver.start(this);
        this.isOpen = true;
    }
    
    @Override
    public void close() {
        this.driver.stop(this);
        this.isOpen = false;
    }
    
    /**
     * Transfer a packet.
     * If this flow controller is not open, blocks until it is open.
     * 
     * @param packet the packet to transfer
     * @throws InterruptedException
     */
    public void transfer(final T packet) throws InterruptedException {
        try {
            this.transferQueue.transfer(packet);
        } catch (InterruptedException err) {
            this.transferQueue.put(packet);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Returns whether or not this flow controller is open.
     * 
     * @return the isOpen
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Take and remove a packet from the transfer queue, 
     * waiting if necessary until a packet becomes available and call the consumer to process this packet.
     * 
     * @return the size in bytes of the transfered packet or null if interrupted while waiting for a packet
     * to transfer 
     */
    int process() {
        try {
            final T packet = this.transferQueue.take();
            this.consumer.accept(packet);
            return bytesFunction.applyAsInt(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    /**
     * Takes and removes all the packets from the transfer queue and call the consumer
     * to process all of these packets. 
     */
    void drain() {
        final ArrayList<T> packets = new ArrayList<>();
        transferQueue.drainTo(packets);
        for (T p : packets) {
            this.consumer.accept(p);
        }
    }
    
}
