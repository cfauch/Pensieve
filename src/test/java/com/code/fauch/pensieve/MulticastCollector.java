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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author c.fauch
 *
 */
public final class MulticastCollector {

    /**
     * Where to submit collector task
     */
    private final ExecutorService executor;
    
    /**
     * The receiver task
     */
    private final ReceivePacketsTask receiver;
    
    /**
     * Where are stored received chunks.
     */
    private final BlockingQueue<Chunk> queue;
        
    /**
     * Timestamp statistics chunk collector.
     *
     * @author cfauch
     */
    public static final class Statistics extends AbsCollector {
        
        /**
         * Current timetamp.
         */
        private Long current;
        
        /**
         * Current delay min (ms)
         */
        private Long delayMin;
        
        /**
         * Current delay max (ms)
         */
        private Long delayMax;

        /**
         * Current chunk size min (bytes)
         */
        private Integer chunkSizeMin;
        
        /**
         * Current chunk size max (bytes)
         */
        private Integer chunkSizeMax;
        
        /**
         * Current sum of each delay (ms)
         */
        private long delaySum;
        
        /**
         * Number of chunk taken in account into delay sum
         */
        private int chunkCount;
        
        /**
         * Constructor.
         *
         */
        private Statistics() {
            this.current = null;
            this.delayMin = null;
            this.delayMax = null;
            this.chunkSizeMax = null;
            this.chunkSizeMin = null;
            this.delaySum = 0;
            this.chunkCount = 0;
        }
        
        /**
         * Update statistics with given chunk
         */
        protected void collect(final Chunk chunk) {
            final int size = chunk.data.length;
            if (this.current == null) {
                this.current = chunk.timestamp;
            } else {
                final long delta = chunk.timestamp - this.current;
                if (this.delayMin == null || delta < this.delayMin) {
                    this.delayMin = delta;
                }
                if (this.delayMax == null || delta > this.delayMax) {
                    this.delayMax = delta;
                }
                this.delaySum += delta;
                this.chunkCount++;
                this.current = chunk.timestamp;
            }
            if (this.chunkSizeMin == null || size < this.chunkSizeMin) {
                this.chunkSizeMin = size;
            }
            if (this.chunkSizeMax == null || size > this.chunkSizeMax) {
                this.chunkSizeMax = size;
            }
        }

        /**
         * Compute delay average
         * @return delay average
         */
        public Long getDelayAverage() {
            return this.chunkCount == 0 ? null : this.delaySum / this.chunkCount;
        }
        
        /**
         * Min delay (ms)
         * @return
         */
        public Long getDelayMin() {
            return this.delayMin;
        }
        
        /**
         * Max delay (ms)
         * @return
         */
        public Long getDelayMax() {
            return this.delayMax;
        }
                
        /**
         * Min chunk size (bytes)
         * @return
         */
        public Integer getChunkSizeMin() {
            return this.chunkSizeMin;
        }

        /**
         * Max chunk size (bytes)
         * @return
         */
        public Integer getChunkSizeMax() {
            return this.chunkSizeMax;
        }

        /**
         * Total collected chunk count.
         * @return
         */
        public Integer getChunkCount() {
            return this.chunkCount + 1;
        }
    }
    
    /**
     * Chunk data aggregator.
     * Aggregate each chunk data into byte array.
     *
     * @author cfauch
     */
    public static class Aggregator extends AbsCollector {
        
        /**
         * Stream to collect chunk data.
         */
        private ByteArrayOutputStream stream;

        /**
         * Write chunk data into stream
         * @throws IOException 
         */
        @Override
        protected void collect(Chunk chunk) throws IOException {
            if (this.stream != null) {
                this.stream.write(chunk.data);
            }
        }


        /**
         * Close stream
         * @throws IOException 
         */
        @Override
        protected void stop() throws IOException {
            if (this.stream != null) {
                this.stream.close();
            }
        }


        /**
         * Open stream
         */
        @Override
        protected void start() {
            if (this.stream == null) {
                this.stream = new ByteArrayOutputStream();
            }
        }
        
        /**
         * Return all data received
         * @return
         */
        public byte[] getData() {
            return this.stream.toByteArray();
        }
        
    }
    
    /**
     * Combine chunk collectors.
     *
     * @author cfauch
     */
    public static class CombineCollector extends AbsCollector {

        /**
         * First collector to apply
         */
        private final AbsCollector first;
        
        /**
         * Second collector to apply
         */
        private final AbsCollector second;
        
        /**
         * Constructor.
         *
         * @param first
         * @param second
         */
        private CombineCollector(final AbsCollector first, final AbsCollector second) {
            this.first = first;
            this.second = second;
        }
        
        /**
         * Collect chunk on each collectors
         */
        @Override
        protected void collect(final Chunk chunk) throws Exception {
            this.first.collect(chunk);
            this.second.collect(chunk);
        }
        
        /**
         * start each collectors
         */
        @Override
        protected void start() throws Exception {
            this.first.start();
            this.second.start();
        }
        
        /**
         * stop each collectors.
         */
        @Override
        protected void stop() throws Exception {
            this.first.stop();
            this.second.stop();
        }
        
    }
    
    /**
     * Abstract collector definition.
     *
     * @author cfauch
     */
    static abstract class AbsCollector {
        
        protected abstract void collect(Chunk chunk) throws Exception;
        
        protected void stop() throws Exception {};
        
        protected void start() throws Exception {};
        
        public AbsCollector andThen (final AbsCollector collector) {
            return new CombineCollector(this, collector);
        }
        
    }
    
    /**
     * Received chunk.
     *
     * @author cfauch
     */
    private static final class Chunk {
        
        /**
         * received data (bytes).
         */
        private final byte[] data;
        
        /**
         * receive timestamp.
         */
        private final long timestamp;
        
        /**
         * Constructor.
         *
         * @param timestamp
         * @param data
         */
        private Chunk(final long timestamp, final byte[] data) {
            this.data = data;
            this.timestamp = timestamp;
        }
        
    }
    
    private final class ReceivePacketsTask implements Callable<Void> {
        
        /**
         * Chunk size
         */
        private final int chunkSize;
        
        /**
         * Multicast port
         */
        private final int port;
        
        /**
         * Multicast address
         */
        private final String address;
        
        /**
         * The optional network interface to listen.
         */
        private final String networkInterface;
        
        /**
         * Current socket
         */
        private MulticastSocket socket;

        /**
         * Constructor.
         *
         */
        public ReceivePacketsTask(final int chunkSize, final String address, final int port, final String networkInterface) {
            this.chunkSize = chunkSize;
            this.address = address;
            this.port = port;
            this.networkInterface = networkInterface;
        }
        
        /**
         * Receive chunk from multicast socket.
         */
        @Override
        public Void call() throws Exception {
            byte[] chunk = new byte[this.chunkSize];
            MulticastSocket socket = null;
            InetAddress group = null;
            try {
                socket = new MulticastSocket(this.port);
                if (this.networkInterface != null) {
                    socket.setNetworkInterface(NetworkInterface.getByName(this.networkInterface));
                }
                group = InetAddress.getByName(this.address);
                socket.joinGroup(group);
                setSocket(socket);
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(chunk, chunk.length);
                    socket.receive(packet);
                    queue.put(new Chunk(System.currentTimeMillis(), Arrays.copyOf(packet.getData(), packet.getLength())));
                }
                return null;
            } finally {
                if (socket != null) {
                    if (group != null) {
                        socket.leaveGroup(group);
                    }
                    socket.close();
                }
            }
        }
        
        public void stop() {
            final MulticastSocket socket = getSocket();
            if (socket != null) {
                socket.close();
            }
        }
     
        private synchronized void setSocket(final MulticastSocket socket) {
            this.socket = socket;
        }
        
        private synchronized MulticastSocket getSocket() {
            return this.socket;
        }
    }
    
    /**
     * Constructor.
     *
     */
    public MulticastCollector(final int chunkSize, final String address, final int port, final String networkInterface) {
        this.executor = Executors.newFixedThreadPool(1);
        this.receiver = new ReceivePacketsTask(chunkSize, address, port, networkInterface);
        this.queue = new ArrayBlockingQueue<>(600000);
    }
    

    
    /**
     * Collect chunks.
     *  
     * @param collector where to collect chunk
     * @param timeout how long to wait for received chunks before to return(in units of unit)
     * @param unit the time unit of timeout
     * @return the given collector.
     * @throws Exception
     */
    public <U extends AbsCollector> U collect(final U collector, final long timeout, final TimeUnit unit) 
            throws Exception {
        collector.start();
        while(true) {
            final Chunk chunk = this.queue.poll(timeout, unit);
            if (chunk == null) {
                collector.stop();
                return collector;
            }
            collector.collect(chunk);
        }
    }
    
    /**
     * Build and returns statistics collector.
     * @return
     */
    public Statistics statistics() {
        return new Statistics();
    }
    
    /**
     * Build and return a data aggregator.
     * @return
     */
    public Aggregator aggregate() {
        return new Aggregator();
    }
    
    /**
     * Start multicast reception.
     */
    public void start() {
        this.executor.submit(this.receiver);
    }
    
    /**
     * Stop multicast reception.
     */
    public void stop() {
        try {
            this.receiver.stop();
            this.executor.shutdownNow();
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
