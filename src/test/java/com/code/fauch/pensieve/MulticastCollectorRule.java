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

import java.util.concurrent.TimeUnit;

import org.junit.rules.ExternalResource;

/**
 * @author c.fauch
 *
 */
public final class MulticastCollectorRule extends ExternalResource {
    
    private final MulticastCollector adaptee;
    
    /**
     * Constructor.
     *
     */
    public MulticastCollectorRule(final int chunkSize, final String address, final int port) {
        this.adaptee = new MulticastCollector(chunkSize, address, port, null);
    }

    /**
     * Constructor.
     *
     */
    public MulticastCollectorRule(final int chunkSize, final String address, final int port, final String networkInterface) {
        this.adaptee = new MulticastCollector(chunkSize, address, port, networkInterface);
    }

    /**
     * @see org.junit.rules.ExternalResource#before()
     */
    @Override
    protected void before() throws Throwable {
        this.adaptee.start();
    }

    /**
     * @see org.junit.rules.ExternalResource#after()
     */
    @Override
    protected void after() {
        this.adaptee.stop();
    }

    /**
     * @return data aggregator
     */
    public MulticastCollector.Aggregator aggregate() {
        return this.adaptee.aggregate();
    }

    /**
     * Build and returns statistics collector.
     * @return
     */
    public MulticastCollector.Statistics statistics() {
        return this.adaptee.statistics();
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
    public <U extends MulticastCollector.AbsCollector> U collect(final U collector, final long timeout, final TimeUnit unit) 
            throws Exception {
        return this.adaptee.collect(collector, timeout, unit);
    }
    
}
