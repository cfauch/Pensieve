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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author c.fauch
 *
 */
public class SocketWithControlFlowRateTest {

    @Rule
    public MulticastCollectorRule collectman = new MulticastCollectorRule(1024, "233.54.12.235", 4446, "lo");

    @Test(expected=NullPointerException.class)
    public void SocketWithControlFlowNullDriver() throws Exception {
        final String[] words = new String[] {"HELLO WORLD!", "COUCOU", "MON VOISIN TOTORO"};
        try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(null)
                .networkInterface("lo")
                .ttl(255)
                .open()) {
            for (String w : words) {
                final byte[] data = w.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
            }
            final byte[] rcv = this.collectman.collect(this.collectman.aggregate(), 5, TimeUnit.SECONDS).getData();
            Assert.assertEquals("HELLO WORLD!COUCOUMON VOISIN TOTORO", new String(rcv));
        }
    }

    @Test(expected=NullPointerException.class)
    public void SocketWithControlFlowNullPacket() throws Exception {
        try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new ScheduleWithFixedDelay(1, TimeUnit.SECONDS))
                .networkInterface("lo")
                .ttl(255)
                .open()) {
            socket.send(null);
            final byte[] rcv = this.collectman.collect(this.collectman.aggregate(), 5, TimeUnit.SECONDS).getData();
            Assert.assertEquals("HELLO WORLD!COUCOUMON VOISIN TOTORO", new String(rcv));
        }
    }
    
    @Test
    public void SocketWithControlFlowRateFixedRate() throws Exception {
        final String[] words = new String[] {"HELLO WORLD!", "COUCOU", "MON VOISIN TOTORO"};
        try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new ScheduleAtFixedRate(1, TimeUnit.SECONDS))
                .networkInterface("lo")
                .ttl(255)
                .open()) {
            for (String w : words) {
                final byte[] data = w.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
            }
            final byte[] rcv = this.collectman.collect(this.collectman.aggregate(), 5, TimeUnit.SECONDS).getData();
            Assert.assertEquals("HELLO WORLD!COUCOUMON VOISIN TOTORO", new String(rcv));
        }
    }

    @Test
    public void SocketWithControlFlowRateFixedDelay() throws Exception {
        final String[] words = new String[] {"HELLO WORLD!", "COUCOU", "MON VOISIN TOTORO"};
        try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new ScheduleWithFixedDelay(1, TimeUnit.SECONDS))
                .networkInterface("lo")
                .ttl(255)
                .open()) {
            for (String w : words) {
                final byte[] data = w.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
            }
            final byte[] rcv = this.collectman.collect(this.collectman.aggregate(), 5, TimeUnit.SECONDS).getData();
            Assert.assertEquals("HELLO WORLD!COUCOUMON VOISIN TOTORO", new String(rcv));
        }
    }
    
    @Test
    public void SocketWithControlFlowRateTransderRate() throws Exception {
        final String[] words = new String[] {"HELLO WORLD!", "COUCOU", "MON VOISIN TOTORO"};
        try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(24))
                .networkInterface("lo")
                .ttl(255)
                .open()) {
            for (String w : words) {
                final byte[] data = w.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
            }
            final byte[] rcv = this.collectman.collect(this.collectman.aggregate(), 5, TimeUnit.SECONDS).getData();
            Assert.assertEquals("HELLO WORLD!COUCOUMON VOISIN TOTORO", new String(rcv));
        }
    }
    
}
