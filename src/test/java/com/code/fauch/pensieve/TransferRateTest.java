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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author c.fauch
 *
 */
public class TransferRateTest {

    @Test
    public void testTransferRateNominal() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new TransferRate(48).execute(p -> packets.add(p), p->p.length())) {
            ctrl.open();
            for (int i = 1; i <=2; i++) {
                ctrl.transfer("HELLO ");
                ctrl.transfer("WORLD!");
            }
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!", "HELLO ", "WORLD!"), packets);
    }
    
    @Test
    public void testTransferRateNegativeRate() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new TransferRate(-48).execute(p -> packets.add(p), p->p.length())) {
            ctrl.open();
            for (int i = 1; i <=2; i++) {
                ctrl.transfer("HELLO ");
                ctrl.transfer("WORLD!");
            }
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!", "HELLO ", "WORLD!"), packets);
    }
    
    @Test
    public void testTransferRateZeroRate() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new TransferRate(0).execute(p -> packets.add(p), p->p.length())) {
            ctrl.open();
            for (int i = 1; i <=2; i++) {
                ctrl.transfer("HELLO ");
                ctrl.transfer("WORLD!");
            }
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!", "HELLO ", "WORLD!"), packets);
    }
    
    @Test(expected=NullPointerException.class)
    public void testTransferRateNullConsumer() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new TransferRate(48).execute(null, p->p.length())) {
            ctrl.open();
            for (int i = 1; i <=2; i++) {
                ctrl.transfer("HELLO ");
                ctrl.transfer("WORLD!");
            }
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!", "HELLO ", "WORLD!"), packets);
    }
    
    @Test(expected=NullPointerException.class)
    public void testTransferRateNullFunction() throws InterruptedException {
        final ArrayList<String> packets = new ArrayList<String>();
        try(FlowController<String> ctrl = new TransferRate(48).execute(p -> packets.add(p), null)) {
            ctrl.open();
            for (int i = 1; i <=2; i++) {
                ctrl.transfer("HELLO ");
                ctrl.transfer("WORLD!");
            }
        }
        Assert.assertEquals(Arrays.asList("HELLO ", "WORLD!", "HELLO ", "WORLD!"), packets);
    }
    
}
