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

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Describe the expected behavior of a driver like expected by the flow controller.
 * This object is used by the flow controller to drive packets unstacking from the transfer queue.
 * 
 * @author c.fauch
 *
 */
public abstract class AbsDriver {

    /**
     * Start packets unstacking from the flow controller.
     * 
     * @param ctrl the flow controller (not null)
     */
    abstract void start(final FlowController<?> ctrl);
    
    /**
     * Stop the packets unstacking from the flow controller.
     * 
     * @param ctrl the flow controller (not null)
     */
    abstract void stop(final FlowController<?> ctrl);
    
    /**
     * Builds and returns a flow controller.
     * 
     * @param <U>
     * @param consumer the packet consumer to handle each unstacking packets
     * @param bytesFunction the function to call on each packet to compute the size in bytes of each transfered packet.
     * @return the new flow controller
     */
    public <U> FlowController<U> execute(final Consumer<U> consumer, ToIntFunction<U> bytesFunction) {
        return new FlowController<U>(this, consumer, bytesFunction);
    }

}
