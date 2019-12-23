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
/**
 * <p>
 * The main API to control data flow rate.
 * </p>
 * <p> 
 * On the first side, packets are put in a transfer queue with an unpredictible flow rate.
 * On the second side, packets are unstack and processed by controlling flow rate.
 * </p>
 * <h3>How to transfer packets by controlling flow rate</h3>
 * <p>
 * The first thing to do is to choose the right driver. There are three drivers:
 * <ul>
 * <li><code>ScheduleAtFixedRate</code>: to schedule a transfer task at fixed rate 
 * (with the same period between each beginning of transfer tasks)</li>
 * <li><code>ScheduleWithFixedDelay</code>: to schedule a transfer task with a fixed delay between the
 * end of the first task and the start of the second one</li>
 * <li><code>TransferRate</code>: to transfer the packets according a transfer rate given in bits/s </li>
 * </ul>
 * You can implement your own driver by extended <code>AbsDriver</code>.
 * </p>
 * <p>
 * Next, call the <code>execute(consumer, byteFunction)</code> method to create a new flow controller with 
 * following arguments:
 * <ul>
 * <li><code>consumer</code>: the consumer to call to process the unstacked packet</li>
 * <li><code>byteFunction</code>: the function to call to compute the size in bytes of the transfered packet.</p>
 * </ul>
 * </p>
 * <p>
 * In the end, open the flow controller and call <code>transfer</code> method to add packets into the queue.
 * </p>
 * <h4>Transferring string packets: transfer rate</h4>
 * <pre>
 *      final ArrayList<String> packets = new ArrayList<String>();
 *      try(FlowController<String> ctrl = new TransferRate(48).execute(p -> packets.add(p), p->p.length())) {
 *          ctrl.open();
 *          for (int i = 1; i <=2; i++) {
 *              ctrl.transfer("HELLO ");
 *              ctrl.transfer("WORLD!");
 *          }
 *      }
 * </pre>
 * <h4>Transferring string packets: at fixed rate</h4>
 * <pre>
 *      final ArrayList<String> packets = new ArrayList<String>();
 *      try(FlowController<String> ctrl = new ScheduleAtFixedRate(1, TimeUnit.SECONDS).execute(p -> packets.add(p), p->p.length())) {
 *          ctrl.open();
 *          ctrl.transfer("HELLO ");
 *          ctrl.transfer("WORLD!");
 *      }
 * </pre>
 * <h4>Transferring string packets: with fixed delay</h4>
 * <pre>
 *      final ArrayList<String> packets = new ArrayList<String>();
 *      try(FlowController<String> ctrl = new ScheduleWithFixedDelay(1, TimeUnit.SECONDS).execute(p -> packets.add(p), p->p.length())) {
 *          ctrl.open();
 *          ctrl.transfer("HELLO ");
 *          ctrl.transfer("WORLD!");
 *      }
 * </pre>
 * <h3>Socket with data flow rate control</h3>
 * <p>
 * To send data through a socket by controlling data rate, you must instantiate a 
 * {@linkplain com.code.fauch.pensieve.SocketWithControlFlowRate} like this:
 * <pre>
 * SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(24))
 *              .networkInterface("lo")
 *              .open()
 * </pre>
 * <ul>
 * <li><code>new TransferRate(24)</code> is the driver to use by the flow controller to drive flow rate</li>
 * <li><code>"lo"</code> is the network interface to use by the socket</li>
 * <li><code>open()</code> this method creates the socket and open it</li>
 * </ul>
 * Now, you just have to send packets by calling the <code>send</code> method. The packets will be sent with 
 * a given rate depending on the driver used.
 * <h4>complete example:</h4>
 * <pre>
 *      final String[] words = new String[] {"HELLO WORLD!", "COUCOU", "MON VOISIN TOTORO"};
 *      try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(24))
 *              .networkInterface("lo")
 *              .open()) {
 *          for (String w : words) {
 *              final byte[] data = w.getBytes();
 *              socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
 *          }
 *      }
 * </pre>
 * </p>
 * 
 * @author c.fauch
 *
 */
package com.code.fauch.pensieve;
