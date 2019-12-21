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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This socket use a {@linkplain com.code.fauch.pensieve.FlowController} to control data flow rate.
 * 
 * @author c.fauch
 *
 */
public final class SocketWithControlFlowRate extends MulticastSocket implements AutoCloseable, Consumer<DatagramPacket> {

    /**
     * The internal flow controller.
     */
    private final FlowController<DatagramPacket> ctrl;
    
    /**
     * Socket builder.
     * 
     * @author c.fauch
     *
     */
    public static class Builder {
        
        /**
         * The driver to use to control data flow rate.
         */
        private final AbsDriver driver;
        
        /**
         * The optional source address.
         */
        private String address;
        
        /**
         * The optional network interface to use.
         */
        private String networkInterface;
        
        /**
         * The optional time to leave
         */
        private Integer ttl;
        
        /**
         * The optional source port.
         */
        private Integer port;
        
        /**
         * Constructor.
         * 
         * @param driver the flow rate driver (not null)
         */
        private Builder(final AbsDriver driver) {
            this.driver = driver;
        }
        
        /**
         * Specifies a network interface.
         * 
         * @param netInterface the network interface
         * @return this builder
         */
        public Builder networkInterface(final String netInterface) {
            this.networkInterface = netInterface;
            return this;
        }
        
        /**
         * Specifies the time to leave.
         * 
         * @param ttl the time to leave
         * @return this builder
         */
        public Builder ttl(final int ttl) {
            this.ttl = ttl;
            return this;
        }
        
        /**
         * Specifies a source port.
         * 
         * @param port the source port
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }
        
        /**
         * Specifies a source address.
         * 
         * @param address the source address
         * @return this builder
         */
        public Builder address(final String address) {
            this.address = address;
            return this;
        }
        
        /**
         * Build and open the  socket.
         * 
         * @return the corresponding socket
         * @throws IOException
         */
        public SocketWithControlFlowRate open() throws IOException {
            if (this.address == null && this.port == null) {
                return new SocketWithControlFlowRate(this.driver, this.networkInterface, this.ttl);
            } else if (this.address != null && this.port != null) {
                return new SocketWithControlFlowRate(
                        this.driver,
                        new InetSocketAddress(this.address, this.port),
                        this.networkInterface,
                        this.ttl);
            } else if (this.address != null) {
                return new SocketWithControlFlowRate(
                        this.driver,
                        new InetSocketAddress(this.address, 0),
                        this.networkInterface,
                        this.ttl);
            } else {
                return new SocketWithControlFlowRate(
                        this.driver,
                        this.port,
                        this.networkInterface,
                        this.ttl);
            }
        }
    }
    
    /**
     * Constructor.
     * 
     * @param driver the driver to use to control data flow rate (not null)
     * @param address the source address (not null)
     * @param networkInterface the network interface
     * @param ttl the time to leave
     * @throws IOException
     */
    private SocketWithControlFlowRate(final AbsDriver driver, final InetSocketAddress address, 
            final String networkInterface, final Integer ttl) throws IOException {
        super(address);
        if (networkInterface != null) {
            setNetworkInterface(NetworkInterface.getByName(networkInterface));
        }
        if (ttl != null) {
            setTimeToLive(ttl);
        }
        this.ctrl = driver.execute(this, p->p.getLength());
        this.ctrl.open();
    }
    
    /**
     * Constructor.
     * 
     * @param driver the driver to use to control data flow rate (not null)
     * @param port the source port
     * @param networkInterface the network interface
     * @param ttl the time to leave
     * @throws IOException
     */
    private SocketWithControlFlowRate(final AbsDriver driver, final int port, 
            final String networkInterface, final Integer ttl) throws IOException {
        super(port);
        if (networkInterface != null) {
            setNetworkInterface(NetworkInterface.getByName(networkInterface));
        }
        if (ttl != null) {
            setTimeToLive(ttl);
        }
        this.ctrl = driver.execute(this, p->p.getLength());
        this.ctrl.open();
    }
    
    /**
     * Constructor.
     * 
     * @param driver the driver to use to control data flow rate (not null)
     * @param networkInterface the network interface
     * @param ttl the time to leave
     * @throws IOException
     */
    private SocketWithControlFlowRate(final AbsDriver driver, 
            final String networkInterface, final Integer ttl) throws IOException {
        super();
        if (networkInterface != null) {
            setNetworkInterface(NetworkInterface.getByName(networkInterface));
        }
        if (ttl != null) {
            setTimeToLive(ttl);
        }
        this.ctrl = driver.execute(this, p->p.getLength());
        this.ctrl.open();
    }
    
    /**
     * Return a builder to build the socket with the given flow rate driver.
     * 
     * @param driver the driver for the flow rate controller (not null)
     * @return the builder
     */
    public static Builder with(final AbsDriver driver) {
        return new Builder(Objects.requireNonNull(driver, "driver is missing"));
    }
    
    /**
     * Overridden to push the packet into the transfer queue of the flow controller 
     * instead of send it directly through the socket.
     */
    @Override
    public void send(final DatagramPacket packet) {
        try {
            this.ctrl.transfer(Objects.requireNonNull(packet, "packet is mandatory"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("|  streaming is interrupted");
        }
    }
    
    /**
     * Send packet through the socket (not null).
     */
    @Override
    public void accept(final DatagramPacket packet) {
        try {
            super.send(packet);
        } catch (IOException e) {
            System.err.println("|  Unable to send packet." + e);
        }
    }
    
    /**
     * Close the internal streaming object.
     */
    @Override
    public void close() {
        try {
            this.ctrl.close();
        } finally {
            super.close();
        }
    }
    
}
