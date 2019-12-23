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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author c.fauch
 *
 */
public class ScheduleWithFixedDelayTest {

    @Test
    public void testScheduleWithFixedDelayNominal() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new ScheduleWithFixedDelay(1, TimeUnit.SECONDS).execute(p -> packets.add(p), p->p.length())) {
            ctrl.open();
            ctrl.transfer("HELLO ");
            ctrl.transfer("WORLD!");
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!"), packets);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testScheduleWithFixedDelayNegativeDelay() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new ScheduleWithFixedDelay(-1, TimeUnit.SECONDS).execute(p -> packets.add(p), p->p.length())) {
            ctrl.open();
            ctrl.transfer("HELLO ");
            ctrl.transfer("WORLD!");
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!"), packets);
    }
    
    @Test(expected=NullPointerException.class)
    public void testScheduleWithFixedDelayNullConsumer() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new ScheduleWithFixedDelay(1, TimeUnit.SECONDS).execute(null, p->p.length())) {
            ctrl.open();
            ctrl.transfer("HELLO ");
            ctrl.transfer("WORLD!");
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!"), packets);
    }
    
    @Test(expected=NullPointerException.class)
    public void testScheduleWithFixedDelayNullFunction() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new ScheduleWithFixedDelay(1, TimeUnit.SECONDS).execute(p -> packets.add(p), null)) {
            ctrl.open();
            ctrl.transfer("HELLO ");
            ctrl.transfer("WORLD!");
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!"), packets);
    }
}
